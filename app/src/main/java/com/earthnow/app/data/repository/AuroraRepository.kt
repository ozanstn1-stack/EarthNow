package com.earthnow.app.data.repository

import com.earthnow.app.data.api.NoaaSwpcApi
import com.earthnow.app.domain.model.AuroraData
import com.earthnow.app.domain.model.AuroraPoint
import com.earthnow.app.util.TimeFormat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuroraRepository @Inject constructor(
    private val api: NoaaSwpcApi,
    private val cache: JsonCache
) {
    private val ttl = 15 * 60_000L
    private val key = "noaa_aurora"

    data class AuroraResult(val data: AuroraData, val fetchedAt: Long, val stale: Boolean)

    suspend fun latest(): AuroraResult = withContext(Dispatchers.IO) {
        cache.getStale(key)?.let { (json, ts) ->
            val fresh = System.currentTimeMillis() - ts <= ttl
            if (fresh) return@withContext AuroraResult(deserialize(json), ts, false)
        }
        try {
            val ovation = api.ovation()
            val kp = runCatching { api.kpIndex() }.getOrNull()
            val data = AuroraData(
                observationTime = ovation.observationTime?.let { TimeFormat.isoToMillis(it) },
                forecastTime = ovation.forecastTime?.let { TimeFormat.isoToMillis(it) },
                points = ovation.coordinates.mapNotNull { c ->
                    if (c.size >= 3) AuroraPoint(c[0], c[1], c[2].toInt()) else null
                },
                kpIndex = kp?.lastOrNull()?.estimatedKp,
                kpUpdatedAt = kp?.lastOrNull()?.timeTag?.let { TimeFormat.isoToMillis(it) }
            )
            cache.put(key, serialize(data))
            AuroraResult(data, System.currentTimeMillis(), false)
        } catch (e: Exception) {
            val stale = cache.getStale(key)
            if (stale != null) AuroraResult(deserialize(stale.first), stale.second, true)
            else throw e
        }
    }

    suspend fun forceRefresh(): AuroraResult = withContext(Dispatchers.IO) {
        try {
            val ovation = api.ovation()
            val kp = runCatching { api.kpIndex() }.getOrNull()
            val data = AuroraData(
                observationTime = ovation.observationTime?.let { TimeFormat.isoToMillis(it) },
                forecastTime = ovation.forecastTime?.let { TimeFormat.isoToMillis(it) },
                points = ovation.coordinates.mapNotNull { c ->
                    if (c.size >= 3) AuroraPoint(c[0], c[1], c[2].toInt()) else null
                },
                kpIndex = kp?.lastOrNull()?.estimatedKp,
                kpUpdatedAt = kp?.lastOrNull()?.timeTag?.let { TimeFormat.isoToMillis(it) }
            )
            cache.put(key, serialize(data))
            AuroraResult(data, System.currentTimeMillis(), false)
        } catch (e: Exception) {
            val stale = cache.getStale(key)
            if (stale != null) AuroraResult(deserialize(stale.first), stale.second, true)
            else throw e
        }
    }

    fun probabilityText(kp: Double?): Pair<String, String> {
        val k = kp ?: return "Unknown" to "No Kp data available"
        return when {
            k >= 7 -> "Very high" to "Kp $k — aurora likely visible far from the poles"
            k >= 5 -> "High" to "Kp $k — aurora may be visible in northern Scandinavia, Iceland, Alaska"
            k >= 3 -> "Moderate" to "Kp $k — aurora possible near the polar circles"
            else -> "Low" to "Kp $k — aurora mostly confined to polar regions"
        }
    }

    private fun serialize(d: AuroraData): String = buildString {
        append(d.observationTime).append('|').append(d.forecastTime).append('|')
        append(d.kpIndex).append('|').append(d.kpUpdatedAt).append('|')
        d.points.forEach { append(it.lon).append(',').append(it.lat).append(',').append(it.intensity).append(';') }
    }

    private fun deserialize(s: String): AuroraData = try {
        val parts = s.split('|')
        val points = mutableListOf<AuroraPoint>()
        if (parts.size >= 5) {
            parts[4].split(';').filter { it.isNotBlank() }.forEach {
                val c = it.split(',')
                if (c.size >= 3) points += AuroraPoint(c[0].toDouble(), c[1].toDouble(), c[2].toInt())
            }
        }
        AuroraData(
            observationTime = parts.getOrNull(0)?.toLongOrNull(),
            forecastTime = parts.getOrNull(1)?.toLongOrNull(),
            points = points,
            kpIndex = parts.getOrNull(2)?.toDoubleOrNull(),
            kpUpdatedAt = parts.getOrNull(3)?.toLongOrNull()
        )
    } catch (e: Exception) {
        AuroraData(null, null, emptyList(), null, null)
    }
}