package com.earthnow.app.data.repository

import android.content.Context
import com.earthnow.app.data.api.OpenMeteoGeocodingApi
import com.earthnow.app.domain.model.Place
import com.earthnow.app.util.GeoMath
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Search across cities (Open-Meteo Geocoding), countries (Natural Earth
 * public-domain dataset), volcanoes (GVP catalog) and major ocean regions
 * (static reference list — approximate center points, labeled as such).
 */
@Singleton
class GeocodingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val volcanoRepository: VolcanoRepository,
    private val moshi: Moshi
) {
    private var countries: List<CountryFeature>? = null

    suspend fun search(query: String, limit: Int = 12): List<Place> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val results = mutableListOf<Place>()

        try {
            val dto = geocodingApi.search(q, count = limit)
            dto.results.forEach { r ->
                results += Place(
                    id = "geo_${r.id ?: r.hashCode()}",
                    name = r.name ?: q,
                    country = r.country,
                    admin1 = r.admin1,
                    lat = r.latitude ?: 0.0,
                    lon = r.longitude ?: 0.0,
                    kind = when (r.featureCode) {
                        "PCLI", "PCLS", "PCLD", "PCLIX", "PCLF", "PCL" -> "country"
                        "VOLC" -> "volcano"
                        else -> "city"
                    },
                    population = r.population
                )
            }
        } catch (e: Exception) { }

        val ql = q.lowercase()
        countries().filter { c ->
            c.name.lowercase().contains(ql) || c.iso.contains(ql)
        }.take(6).forEach { c ->
            if (results.none { it.kind == "country" && it.name.equals(c.name, ignoreCase = true) }) {
                results += Place(
                    id = "country_${c.iso}",
                    name = c.name,
                    country = c.name,
                    lat = c.centroidLat,
                    lon = c.centroidLon,
                    kind = "country"
                )
            }
        }

        volcanoRepository.getAll().filter { v ->
            v.name.lowercase().contains(ql) || v.country.lowercase().contains(ql)
        }.take(8).forEach { v ->
            if (results.none { it.id == "volc_${v.name}" }) {
                results += Place(
                    id = "volc_${v.name}",
                    name = v.name,
                    country = v.country,
                    lat = v.latitude,
                    lon = v.longitude,
                    kind = "volcano"
                )
            }
        }

        OCEANS.filter { it.name.lowercase().contains(ql) }.forEach { o ->
            if (results.none { it.id == o.id }) results += o
        }

        return results.distinctBy { it.id }.take(limit)
    }

    suspend fun reverse(lat: Double, lon: Double): String? {
        var best: CountryFeature? = null
        var bestArea = Double.MAX_VALUE
        countries().forEach { c ->
            if (c.contains(lat, lon)) {
                val area = c.boundingArea()
                if (area < bestArea) { bestArea = area; best = c }
            }
        }
        return best?.name
    }

    suspend fun countries(): List<CountryFeature> {
        countries?.let { return it }
        val list = try {
            val json = context.assets.open("countries.geojson").bufferedReader().use { it.readText() }
            val fc = moshi.adapter(NeGeoJson::class.java).fromJson(json)
            fc?.features.orEmpty().mapNotNull { f ->
                val props = f.properties ?: return@mapNotNull null
                val name = props.name ?: return@mapNotNull null
                CountryFeature(name, props.iso ?: "", f.geometry)
            }
        } catch (e: Exception) { emptyList() }
        countries = list
        return list
    }

    private val OCEANS = listOf(
        Place("ocean_pacific", "Pacific Ocean", null, lat = 0.0, lon = -150.0, kind = "ocean"),
        Place("ocean_atlantic", "Atlantic Ocean", null, lat = 0.0, lon = -30.0, kind = "ocean"),
        Place("ocean_indian", "Indian Ocean", null, lat = -20.0, lon = 80.0, kind = "ocean"),
        Place("ocean_arctic", "Arctic Ocean", null, lat = 80.0, lon = 0.0, kind = "ocean"),
        Place("ocean_southern", "Southern Ocean", null, lat = -65.0, lon = 0.0, kind = "ocean"),
        Place("sea_mediterranean", "Mediterranean Sea", null, lat = 36.0, lon = 18.0, kind = "ocean"),
        Place("sea_caribbean", "Caribbean Sea", null, lat = 15.0, lon = -75.0, kind = "ocean"),
        Place("sea_south_china", "South China Sea", null, lat = 12.0, lon = 113.0, kind = "ocean"),
        Place("sea_bering", "Bering Sea", null, lat = 58.0, lon = -178.0, kind = "ocean"),
        Place("sea_red", "Red Sea", null, lat = 20.0, lon = 38.0, kind = "ocean"),
        Place("sea_black", "Black Sea", null, lat = 43.0, lon = 35.0, kind = "ocean"),
        Place("sea_baltic", "Baltic Sea", null, lat = 59.0, lon = 20.0, kind = "ocean"),
        Place("sea_north", "North Sea", null, lat = 56.0, lon = 3.0, kind = "ocean"),
        Place("sea_japan", "Sea of Japan", null, lat = 40.0, lon = 135.0, kind = "ocean"),
        Place("gulf_mexico", "Gulf of Mexico", null, lat = 25.0, lon = -90.0, kind = "ocean"),
        Place("bay_bengal", "Bay of Bengal", null, lat = 15.0, lon = 88.0, kind = "ocean")
    )
}

@JsonClass(generateAdapter = true)
data class NeGeoJson(
    @Json(name = "type") val type: String? = null,
    @Json(name = "features") val features: List<NeFeature> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NeFeature(
    @Json(name = "type") val type: String? = null,
    @Json(name = "properties") val properties: NeProperties? = null,
    @Json(name = "geometry") val geometry: NeGeometry? = null
)

@JsonClass(generateAdapter = true)
data class NeProperties(
    @Json(name = "NAME") val name: String? = null,
    @Json(name = "NAME_LONG") val nameLong: String? = null,
    @Json(name = "ADM0_A3") val iso: String? = null,
    @Json(name = "ADMIN") val admin: String? = null,
    @Json(name = "ADM0_A3_IS") val iso2: String? = null
)

@JsonClass(generateAdapter = true)
data class NeGeometry(
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: Any? = null
)

@Suppress("UNCHECKED_CAST")
fun NeGeometry.rings(): List<List<List<Double>>> {
    val c = coordinates ?: return emptyList()
    return try {
        when (type) {
            "Polygon" -> (c as List<Any?>).map { ring ->
                (ring as List<Any?>).map { pt ->
                    (pt as List<Any?>).map { (it as Number).toDouble() }
                }
            }
            "MultiPolygon" -> (c as List<Any?>).flatMap { poly ->
                (poly as List<Any?>).map { ring ->
                    (ring as List<Any?>).map { pt ->
                        (pt as List<Any?>).map { (it as Number).toDouble() }
                    }
                }
            }
            else -> emptyList()
        }
    } catch (e: Exception) { emptyList() }
}

class CountryFeature(
    val name: String,
    val iso: String,
    private val geometry: NeGeometry?
) {
    val centroidLat: Double
    val centroidLon: Double

    init {
        var sumLat = 0.0
        var sumLon = 0.0
        var n = 0
        geometry?.rings()?.forEach { ring ->
            ring.forEach { p ->
                sumLat += p[1]
                sumLon += p[0]
                n++
            }
        }
        if (n == 0) {
            centroidLat = 0.0; centroidLon = 0.0
        } else {
            centroidLat = sumLat / n
            centroidLon = sumLon / n
        }
    }

    fun contains(lat: Double, lon: Double): Boolean {
        val rings = geometry?.rings() ?: return false
        // Point-in-polygon with longitude wrap handling
        for (ring in rings) {
            if (pointInRing(lat, lon, ring)) return true
        }
        return false
    }

    private fun pointInRing(lat: Double, lon: Double, ring: List<List<Double>>): Boolean {
        // normalize lon near ring longitudes
        val ref = ring.first()[0]
        val lons = if (abs(lon - ref) > 180.0 && abs(lon - (ref + 360.0)) <= 180.0) lon + 360.0 else lon
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = ring[i][0]; val yi = ring[i][1]
            val xj = ring[j][0]; val yj = ring[j][1]
            if ((yi > lat) != (yj > lat) &&
                lons < (xj - xi) * (lat - yi) / (yj - yi) + xi
            ) inside = !inside
            j = i
        }
        return inside
    }

    fun boundingArea(): Double {
        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
        geometry?.rings()?.forEach { ring ->
            ring.forEach { p ->
                if (p[1] < minLat) minLat = p[1]
                if (p[1] > maxLat) maxLat = p[1]
                if (p[0] < minLon) minLon = p[0]
                if (p[0] > maxLon) maxLon = p[0]
            }
        }
        return (maxLat - minLat) * (maxLon - minLon)
    }

    fun distanceToKm(lat: Double, lon: Double): Double =
        GeoMath.haversineKm(lat, lon, centroidLat, centroidLon)
}