package com.earthnow.app.data.repository

import com.earthnow.app.data.api.UsgsApi
import com.earthnow.app.data.remote.dto.UsgsFeatureDto
import com.earthnow.app.domain.model.Earthquake
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Singleton
class EarthquakeRepository @Inject constructor(
    private val api: UsgsApi,
    private val cache: JsonCache,
    private val moshi: Moshi
) {
    private val ttl = 10 * 60_000L
    private val key = "usgs_all_day"

    data class EarthquakeResult(val quakes: List<Earthquake>, val fetchedAt: Long, val stale: Boolean)

    suspend fun allDay(): EarthquakeResult = withContext(Dispatchers.IO) {
        cache.getStale(key)?.let { (json, ts) ->
            val fresh = System.currentTimeMillis() - ts <= ttl
            if (fresh) return@withContext EarthquakeResult(parseFeedJson(json), ts, false)
        }
        try {
            val dto = api.allDay()
            val json = moshi.adapter(com.earthnow.app.data.remote.dto.UsgsFeedDto::class.java).toJson(dto)
            cache.put(key, json)
            EarthquakeResult(dto.features.map { it.toDomain() }, System.currentTimeMillis(), false)
        } catch (e: Exception) {
            val stale = cache.getStale(key)
            if (stale != null) EarthquakeResult(parseFeedJson(stale.first), stale.second, true)
            else throw e
        }
    }

    suspend fun significantToday(minMag: Double = 4.5): List<Earthquake> =
        allDay().quakes.filter { (it.mag ?: 0.0) >= minMag }

    suspend fun forceRefresh(): EarthquakeResult = withContext(Dispatchers.IO) {
        val dto = api.allDay()
        val json = moshi.adapter(com.earthnow.app.data.remote.dto.UsgsFeedDto::class.java).toJson(dto)
        cache.put(key, json)
        EarthquakeResult(dto.features.map { it.toDomain() }, System.currentTimeMillis(), false)
    }

    private fun parseFeedJson(json: String): List<Earthquake> = try {
        moshi.adapter(com.earthnow.app.data.remote.dto.UsgsFeedDto::class.java)
            .fromJson(json)?.features?.map { it.toDomain() } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    private fun UsgsFeatureDto.toDomain(): Earthquake {
        val coords = geometry?.coordinates.orEmpty()
        return Earthquake(
            id = id ?: "",
            mag = properties?.mag,
            place = properties?.place ?: "Unknown location",
            lat = coords.getOrNull(1) ?: 0.0,
            lon = coords.getOrNull(0) ?: 0.0,
            depthKm = coords.getOrNull(2),
            timeMillis = properties?.time ?: 0L,
            url = properties?.url,
            status = properties?.status,
            tsunami = properties?.tsunami?.let { it == 1 },
            felt = properties?.felt,
            dmin = properties?.dmin,
            magType = properties?.magType
        )
    }

    fun lastUpdated(): Long? = runBlocking { cache.getStale(key)?.second }
}