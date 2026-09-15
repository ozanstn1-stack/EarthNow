package com.earthnow.app.data.repository

import android.content.Context
import com.earthnow.app.domain.model.Volcano
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Static volcano catalog derived from the Smithsonian Institution Global
 * Volcanism Program (GVP) "Volcanoes of the World" database (Holocene
 * volcanoes). The catalog is bundled with the app; it contains real GVP
 * records (name, country, region, coordinates, elevation, primary
 * eruption evidence). It is NOT a real-time feed: the UI always labels it
 * as such and never invents current activity.
 */
@Singleton
class VolcanoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cached: List<Volcano>? = null

    suspend fun getAll(): List<Volcano> = withContext(Dispatchers.IO) {
        cached ?: run {
            val list = loadFromAsset()
            cached = list
            list
        }
    }

    suspend fun nearby(lat: Double, lon: Double, radiusKm: Double = 400.0): List<Volcano> {
        val all = getAll()
        return all
            .map { it to com.earthnow.app.util.GeoMath.haversineKm(lat, lon, it.latitude, it.longitude) }
            .filter { it.second <= radiusKm }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun loadFromAsset(): List<Volcano> = try {
        val csv = context.assets.open("volcanoes_gvp_holocene.csv").bufferedReader().use { it.readText() }
        csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }.drop(1).mapNotNull { line ->
            val cols = line.split("|")
            if (cols.size < 8) return@mapNotNull null
            val lat = cols[3].toDoubleOrNull() ?: return@mapNotNull null
            val lon = cols[4].toDoubleOrNull() ?: return@mapNotNull null
            Volcano(
                name = cols[0].trim(),
                country = cols[1].trim(),
                region = cols[2].trim(),
                latitude = lat,
                longitude = lon,
                elevationM = cols[5].toIntOrNull() ?: 0,
                evidence = cols[6].trim(),
                lastEruption = cols[7].trim()
            )
        }
    } catch (e: Exception) { emptyList() }
}