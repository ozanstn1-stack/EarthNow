package com.earthnow.app.data.repository

import com.earthnow.app.data.api.OpenMeteoApi
import com.earthnow.app.domain.model.GridWeather
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.util.Bbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Fetches gridded weather fields (temperature, cloud cover, wind u/v,
 * sea surface temperature) for the visible viewport using real
 * Open-Meteo model data, with adaptive resolution based on zoom.
 */
@Singleton
class GridWeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val cache: JsonCache
) {
    private val cacheTtlNow = 30 * 60_000L
    private val cacheTtlOther = 4 * 60 * 60_000L

    /** Up to 150 coordinates per request: fewer requests means fewer 429s. */
    private val chunkSize = 150

    /** Retry with exponential backoff for rate-limited / transient failures.
     *  Backoff spans well over a minute so a full per-minute quota window
     *  can reset between attempts. */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        val delays = longArrayOf(2_000L, 10_000L, 35_000L)
        var last: Exception? = null
        repeat(delays.size + 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                val msg = e.message ?: ""
                val retryable = msg.contains("429") || msg.contains("50") || e is java.io.IOException
                if (!retryable || attempt == delays.size) throw e
                kotlinx.coroutines.delay(delays[attempt])
            }
        }
        throw last ?: IllegalStateException("request failed")
    }

    data class GridRequest(
        val variable: String,
        val bbox: Bbox,
        val timeMillis: Long,
        val batterySaver: Boolean,
        val isOcean: Boolean = false
    )

    /** Returns one GridWeather per requested variable. */
    suspend fun fetch(vararg variables: String, req: GridRequest): Map<String, GridWeather> =
        withContext(Dispatchers.IO) {
            variables.associateWith { variable ->
                fetchVariable(variable, req)
            }
        }

    private suspend fun fetchVariable(variable: String, req: GridRequest): GridWeather {
        val step = adaptiveStep(req.bbox, req.batterySaver)
        val west = req.bbox.west
        val south = req.bbox.south
        val north = req.bbox.north
        val east = req.bbox.east
        val latCount = max(1, ceil((north - south) / step).toInt())
        val lonCount = max(1, ceil(req.bbox.width / step).toInt())
        val nLats = min(latCount, 90)
        val nLons = min(lonCount, 180)

        val cacheKey = buildCacheKey(variable, req, step, nLats, nLons)
        val ttl = if (req.timeMillis == roundToHour(req.timeMillis)) cacheTtlNow else cacheTtlOther
        cache.get(cacheKey, ttl)?.let { cached ->
            deserialize(cached)?.let { return it }
        }

        val lats = DoubleArray(nLats) { i -> min(85.0, max(-85.0, south + step * i)) }
        // Open-Meteo expects longitudes in [-180, 180]
        val lons = DoubleArray(nLons) { i ->
            var lon = (west + step * i) % 360.0
            if (lon < -180.0) lon += 360.0
            if (lon > 180.0) lon -= 360.0
            lon
        }

        val values = mutableMapOf<Long, Float>()
        var hadFailure = false
        val concurrency = if (req.batterySaver) 2 else 3
        val sem = Semaphore(concurrency)

        coroutineScope {
            // Chunk over the full lat x lon grid (each request can hold up to
            // ~60 coordinates; mismatched lat/lon array lengths are rejected).
            val cells = ArrayList<Pair<Int, Int>>(nLats * nLons)
            for (li in 0 until nLats) {
                for (lj in 0 until nLons) {
                    cells.add(li to lj)
                }
            }
            val results = cells.chunked(chunkSize).map { chunk ->
                async {
                    sem.withPermit {
                        val samples = mutableListOf<GridSample>()
                        try {
                            // Small pacing delay to avoid burst rate-limiting.
                            kotlinx.coroutines.delay(if (req.batterySaver) 800 else 400)
                            val latStr = chunk.joinToString(",") { lats[it.first].toString() }
                            val lonStr = chunk.joinToString(",") { lons[it.second].toString() }
                            if (req.isOcean) {
                                val response = withRetry { api.gridMarine(latitude = latStr, longitude = lonStr) }
                                for (item in response) {
                                    val lat = item.latitude ?: continue
                                    val lon = item.longitude ?: continue
                                    val hourIdx = findHourIndex(item.hourly?.time.orEmpty(), req.timeMillis)
                                    val v = if (hourIdx >= 0) item.hourly?.seaSurfaceTemperature?.getOrNull(hourIdx) else null
                                    samples += GridSample(lat, lon, v)
                                }
                            } else {
                                val response = withRetry {
                                    api.gridForecast(
                                        latitude = latStr,
                                        longitude = lonStr,
                                        hourly = variable
                                    )
                                }
                                if (response.size != chunk.size) {
                                    android.util.Log.w("EarthNowGrid", "$variable chunk: requested ${chunk.size}, got ${response.size}")
                                }
                                for (item in response) {
                                    val lat = item.latitude ?: continue
                                    val lon = item.longitude ?: continue
                                    val hourIdx = findHourIndex(item.hourly?.time.orEmpty(), req.timeMillis)
                                    val v = if (hourIdx >= 0) extractValue(item, variable, hourIdx) else null
                                    samples += GridSample(lat, lon, v)
                                }
                            }
                        } catch (e: Exception) {
                            // Partial or no data for this chunk; keep others
                            hadFailure = true
                            android.util.Log.w("EarthNowGrid", "chunk failed for $variable: ${e.message}")
                        }
                        samples
                    }
                }
            }.awaitAll()

            var dropped = 0
            var samplesTotal = 0
            for (samples in results) {
                samplesTotal += samples.size
                for (s in samples) {
                    val latIdx = lats.indices.minByOrNull { abs(lats[it] - s.lat) } ?: continue
                    // Round to the nearest grid column: the API echoes model
                    // longitudes with tiny float jitter (e.g. 3.0 -> 2.9999),
                    // and truncation would collapse adjacent columns.
                    val lonIdx = kotlin.math.round(
                        (((s.lon - west) % 360.0 + 360.0) % 360.0) / step
                    ).toInt()
                    if (lonIdx !in 0 until nLons) {
                        dropped++
                        if (dropped <= 3) android.util.Log.w("EarthNowGrid", "drop lon out of range: ${s.lon} -> idx $lonIdx")
                        continue
                    }
                    val v = s.value
                    if (v == null) {
                        dropped++
                        if (dropped <= 3) android.util.Log.w("EarthNowGrid", "drop null value at ${s.lat},${s.lon}")
                        continue
                    }
                    values[GridWeather.encode(latIdx, lonIdx)] = v.toFloat()
                }
            }
            if (dropped > 0 || samplesTotal != cells.size) {
                android.util.Log.d("EarthNowGrid", "$variable: expected ${cells.size} samples, got $samplesTotal, dropped=$dropped, mapped=${values.size}")
            }
        }

        val grid = GridWeather(
            layer = if (req.isOcean) LayerType.OCEAN_TEMP else LayerType.TEMPERATURE,
            latStep = step,
            lonStep = step,
            west = west,
            south = south,
            east = east,
            north = north,
            values = values,
            time = req.timeMillis,
            fetchedAt = System.currentTimeMillis()
        )
        // Never cache an incomplete grid: a partial or failed fetch must not
        // be reused for hours (it would render permanent gaps in the layer).
        if (values.isNotEmpty() && !hadFailure) {
            cache.put(cacheKey, serialize(grid))
        }
        android.util.Log.d("EarthNowGrid", "$variable: ${values.size} values, step=$step, ${nLats}x$nLons grid, failed=$hadFailure")
        return grid
    }

    private fun findHourIndex(times: List<String>, targetMillis: Long): Int {
        if (times.isEmpty()) return -1
        var best = -1
        var bestDiff = Long.MAX_VALUE
        times.forEachIndexed { i, t ->
            val ts = com.earthnow.app.util.TimeFormat.isoToMillis(t) ?: return@forEachIndexed
            val d = kotlin.math.abs(ts - targetMillis)
            if (d < bestDiff) { bestDiff = d; best = i }
        }
        return best
    }

    private fun extractValue(item: com.earthnow.app.data.remote.dto.OpenMeteoGridItemDto, variable: String, hourIdx: Int): Double? {
        val h = item.hourly ?: return null
        return when (variable) {
            "temperature_2m" -> h.temperature2m.getOrNull(hourIdx)
            "cloud_cover" -> h.cloudCover.getOrNull(hourIdx)
            "wind_u_component_10m" -> h.windU10m.getOrNull(hourIdx)
            "wind_v_component_10m" -> h.windV10m.getOrNull(hourIdx)
            else -> null
        }
    }

    private data class GridSample(val lat: Double, val lon: Double, val value: Double?)

    private fun adaptiveStep(bbox: Bbox, batterySaver: Boolean): Double {
        // Keep the total number of sampled locations low: on the free tier
        // each location counts as an API call, and large multi-location
        // requests are rate-limited (429/503). ~250 points means at most two
        // 150-coordinate requests per layer.
        val target = if (batterySaver) 80.0 else 100.0
        var step = sqrt(bbox.width * bbox.height / target)
        step = max(0.25, step)
        step = min(25.0, step)
        return round(step * 2) / 2
    }

    private fun buildCacheKey(variable: String, req: GridRequest, step: Double, nLats: Int, nLons: Int): String {
        val hour = roundToHour(req.timeMillis)
        return "grid_${variable}_${step}_${nLats}x${nLons}_${hour}_${req.isOcean}"
    }

    private fun roundToHour(ts: Long): Long = ts - (ts % 3_600_000L)

    private fun serialize(g: GridWeather): String = buildString {
        append(g.latStep).append('|').append(g.lonStep).append('|')
        append(g.west).append('|').append(g.south).append('|').append(g.east).append('|').append(g.north).append('|')
        append(g.fetchedAt).append('|')
        g.values.forEach { (k, v) -> append(k).append(':').append(v).append(',') }
    }

    private fun deserialize(s: String): GridWeather? {
        return try {
            val parts = s.split('|')
            if (parts.size < 7) return null
            val values = mutableMapOf<Long, Float>()
            if (parts.size > 7 && parts[7].isNotBlank()) {
                parts[7].split(',').filter { it.isNotBlank() }.forEach {
                    val kv = it.split(':')
                    values[kv[0].toLong()] = kv[1].toFloat()
                }
            }
            GridWeather(
                layer = LayerType.TEMPERATURE,
                latStep = parts[0].toDouble(),
                lonStep = parts[1].toDouble(),
                west = parts[2].toDouble(),
                south = parts[3].toDouble(),
                east = parts[4].toDouble(),
                north = parts[5].toDouble(),
                values = values,
                time = 0,
                fetchedAt = parts[6].toLong()
            )
        } catch (e: Exception) { null }
    }

    private fun abs(d: Double) = if (d < 0) -d else d
}