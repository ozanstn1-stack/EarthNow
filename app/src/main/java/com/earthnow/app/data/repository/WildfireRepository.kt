package com.earthnow.app.data.repository

import com.earthnow.app.data.api.FirmsApi
import com.earthnow.app.domain.model.Wildfire
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NASA LANCE FIRMS active fire detections (VIIRS). Requires a free MAP_KEY
 * (see README). If no key is configured, an empty result is returned and
 * the UI shows "Wildfire data unavailable".
 */
@Singleton
class WildfireRepository @Inject constructor(
    private val api: FirmsApi,
    private val cache: JsonCache
) {
    private val ttl = 15 * 60_000L
    private val key = "firms_world_day"

    data class WildfireResult(val fires: List<Wildfire>, val fetchedAt: Long, val stale: Boolean, val error: String?)

    suspend fun all(apiKey: String): WildfireResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext WildfireResult(emptyList(), System.currentTimeMillis(), false, "no_api_key")
        }
        cache.getStale(key)?.let { (json, ts) ->
            val fresh = System.currentTimeMillis() - ts <= ttl
            if (fresh) return@withContext WildfireResult(parseCsv(json), ts, false, null)
        }
        try {
            val resp = api.areaCsv(apiKey, "VIIRS_SNPP_NRT", "world", 1)
            if (!resp.isSuccessful) {
                return@withContext WildfireResult(emptyList(), System.currentTimeMillis(), false, "http_${resp.code()}")
            }
            val body = resp.body() ?: ""
            val fires = parseCsv(body)
            if (fires.isNotEmpty()) cache.put(key, body)
            WildfireResult(fires, System.currentTimeMillis(), false, null)
        } catch (e: Exception) {
            val stale = cache.getStale(key)
            if (stale != null) WildfireResult(parseCsv(stale.first), stale.second, true, null)
            else WildfireResult(emptyList(), System.currentTimeMillis(), false, "network")
        }
    }

    suspend fun forceRefresh(apiKey: String): WildfireResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext WildfireResult(emptyList(), System.currentTimeMillis(), false, "no_api_key")
        try {
            val resp = api.areaCsv(apiKey, "VIIRS_SNPP_NRT", "world", 1)
            if (!resp.isSuccessful) return@withContext WildfireResult(emptyList(), System.currentTimeMillis(), false, "http_${resp.code()}")
            val body = resp.body() ?: ""
            val fires = parseCsv(body)
            if (fires.isNotEmpty()) cache.put(key, body)
            WildfireResult(fires, System.currentTimeMillis(), false, null)
        } catch (e: Exception) {
            WildfireResult(emptyList(), System.currentTimeMillis(), false, "network")
        }
    }

    fun parseCsv(csv: String): List<Wildfire> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().split(",").map { it.trim() }
        val idx = mapOf(
            "latitude" to header.indexOf("latitude"),
            "longitude" to header.indexOf("longitude"),
            "bright_ti4" to header.indexOf("bright_ti4"),
            "acq_date" to header.indexOf("acq_date"),
            "acq_time" to header.indexOf("acq_time"),
            "satellite" to header.indexOf("satellite"),
            "confidence" to header.indexOf("confidence"),
            "frp" to header.indexOf("frp"),
            "daynight" to header.indexOf("daynight")
        )
        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(",")
            fun col(name: String): String? = idx[name]?.takeIf { it >= 0 && it < cols.size }?.let { cols[it].trim() }
            val lat = col("latitude")?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = col("longitude")?.toDoubleOrNull() ?: return@mapNotNull null
            Wildfire(
                latitude = lat,
                longitude = lon,
                brightnessK = col("bright_ti4")?.toDoubleOrNull() ?: 0.0,
                acqDate = col("acq_date") ?: "",
                acqTime = col("acq_time") ?: "",
                satellite = col("satellite") ?: "",
                confidence = col("confidence") ?: "",
                frp = col("frp")?.toDoubleOrNull() ?: 0.0,
                dayNight = col("daynight") ?: "",
                source = "NASA FIRMS"
            )
        }
    }

    fun lastUpdated(): Long? = kotlinx.coroutines.runBlocking { cache.getStale(key)?.second }
}