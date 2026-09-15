package com.earthnow.app.data.repository

import com.earthnow.app.data.api.RainViewerApi
import com.earthnow.app.domain.model.RadarFrame
import com.earthnow.app.domain.model.RadarFrames
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RadarRepository @Inject constructor(
    private val api: RainViewerApi,
    private val cache: JsonCache
) {
    private val ttl = 10 * 60_000L
    private val key = "rainviewer_frames"

    data class RadarResult(val frames: RadarFrames, val fetchedAt: Long, val stale: Boolean)

    suspend fun frames(): RadarResult = withContext(Dispatchers.IO) {
        cache.getStale(key)?.let { (json, ts) ->
            val fresh = System.currentTimeMillis() - ts <= ttl
            if (fresh) return@withContext RadarResult(deserialize(json), ts, false)
        }
        try {
            val dto = api.weatherMaps()
            val frames = RadarFrames(
                generated = dto.generated ?: 0L,
                host = dto.host ?: "https://tilecache.rainviewer.com",
                past = dto.radar?.past.orEmpty().mapNotNull { f ->
                    val t = f.time ?: return@mapNotNull null
                    RadarFrame(t, f.path ?: "")
                },
                nowcast = dto.radar?.nowcast.orEmpty().mapNotNull { f ->
                    val t = f.time ?: return@mapNotNull null
                    RadarFrame(t, f.path ?: "")
                }
            )
            cache.put(key, serialize(frames))
            RadarResult(frames, System.currentTimeMillis(), false)
        } catch (e: Exception) {
            val stale = cache.getStale(key)
            if (stale != null) RadarResult(deserialize(stale.first), stale.second, true)
            else throw e
        }
    }

    suspend fun forceRefresh(): RadarResult = withContext(Dispatchers.IO) {
        val dto = api.weatherMaps()
        val frames = RadarFrames(
            generated = dto.generated ?: 0L,
            host = dto.host ?: "https://tilecache.rainviewer.com",
            past = dto.radar?.past.orEmpty().mapNotNull { f ->
                val t = f.time ?: return@mapNotNull null
                RadarFrame(t, f.path ?: "")
            },
            nowcast = dto.radar?.nowcast.orEmpty().mapNotNull { f ->
                val t = f.time ?: return@mapNotNull null
                RadarFrame(t, f.path ?: "")
            }
        )
        cache.put(key, serialize(frames))
        RadarResult(frames, System.currentTimeMillis(), false)
    }

    private fun serialize(f: RadarFrames): String = buildString {
        append(f.generated).append('|').append(f.host).append('|')
        f.all.forEach { append(it.time).append(':').append(it.path).append(';') }
    }

    private fun deserialize(s: String): RadarFrames = try {
        val parts = s.split('|')
        val frames = mutableListOf<RadarFrame>()
        if (parts.size >= 3) {
            parts[2].split(';').filter { it.isNotBlank() }.forEach {
                val c = it.split(':')
                if (c.size >= 2) frames += RadarFrame(c[0].toLong(), c[1])
            }
        }
        RadarFrames(parts.getOrNull(0)?.toLongOrNull() ?: 0L, parts.getOrNull(1) ?: "", frames, emptyList())
    } catch (e: Exception) {
        RadarFrames(0L, "https://tilecache.rainviewer.com", emptyList(), emptyList())
    }
}