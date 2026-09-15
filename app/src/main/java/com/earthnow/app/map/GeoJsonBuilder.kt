package com.earthnow.app.map

import android.graphics.Bitmap
import com.earthnow.app.domain.model.Earthquake
import com.earthnow.app.domain.model.Volcano
import com.earthnow.app.domain.model.Wildfire
import com.earthnow.app.util.Bbox
import org.json.JSONArray
import org.json.JSONObject

/** Builds GeoJSON FeatureCollections from real domain data. */
object GeoJsonBuilder {

    fun featureCollection(features: List<JSONObject>): String {
        val fc = JSONObject()
        fc.put("type", "FeatureCollection")
        fc.put("features", JSONArray(features))
        return fc.toString()
    }

    fun pointFeature(lon: Double, lat: Double, props: Map<String, Any?>): JSONObject {
        val f = JSONObject()
        f.put("type", "Feature")
        val g = JSONObject()
        g.put("type", "Point")
        g.put("coordinates", JSONArray().put(lon).put(lat))
        f.put("geometry", g)
        val p = JSONObject()
        props.forEach { (k, v) ->
            when (v) {
                null -> {}
                is Number -> p.put(k, v)
                is Boolean -> p.put(k, v)
                else -> p.put(k, v.toString())
            }
        }
        f.put("properties", p)
        return f
    }

    fun lineFeature(coords: List<Pair<Double, Double>>, props: Map<String, Any?>): JSONObject {
        val f = JSONObject()
        f.put("type", "Feature")
        val g = JSONObject()
        g.put("type", "LineString")
        val arr = JSONArray()
        coords.forEach { (lon, lat) -> arr.put(JSONArray().put(lon).put(lat)) }
        g.put("coordinates", arr)
        f.put("geometry", g)
        val p = JSONObject()
        props.forEach { (k, v) -> p.put(k, v.toString()) }
        f.put("properties", p)
        return f
    }

    fun polygonFeature(rings: List<List<Pair<Double, Double>>>, props: Map<String, Any?>): JSONObject {
        val f = JSONObject()
        f.put("type", "Feature")
        val g = JSONObject()
        g.put("type", "Polygon")
        val ringsArr = JSONArray()
        rings.forEach { ring ->
            val r = JSONArray()
            ring.forEach { (lon, lat) -> r.put(JSONArray().put(lon).put(lat)) }
            ringsArr.put(r)
        }
        g.put("coordinates", ringsArr)
        f.put("geometry", g)
        val p = JSONObject()
        props.forEach { (k, v) -> p.put(k, v.toString()) }
        f.put("properties", p)
        return f
    }

    fun earthquakes(features: List<Earthquake>): String {
        val list = features.map { q ->
            pointFeature(
                q.lon, q.lat,
                mapOf(
                    "type" to "earthquake",
                    "id" to q.id,
                    "mag" to (q.mag ?: 0.0),
                    "place" to q.place,
                    "time" to q.timeMillis,
                    "depth" to (q.depthKm ?: 0.0),
                    "url" to (q.url ?: "")
                )
            )
        }
        return featureCollection(list)
    }

    fun wildfires(features: List<Wildfire>): String {
        val list = features.map { w ->
            pointFeature(
                w.longitude, w.latitude,
                mapOf(
                    "type" to "wildfire",
                    "lat" to w.latitude,
                    "lon" to w.longitude,
                    "brightness" to w.brightnessK,
                    "acq_date" to w.acqDate,
                    "acq_time" to w.acqTime,
                    "satellite" to w.satellite,
                    "confidence" to w.confidence,
                    "frp" to w.frp,
                    "source" to w.source
                )
            )
        }
        return featureCollection(list)
    }

    fun volcanoes(features: List<Volcano>): String {
        val list = features.map { v ->
            pointFeature(
                v.longitude, v.latitude,
                mapOf(
                    "type" to "volcano",
                    "name" to v.name,
                    "country" to v.country,
                    "elevation" to v.elevationM,
                    "evidence" to v.evidence,
                    "last_eruption" to v.lastEruption
                )
            )
        }
        return featureCollection(list)
    }

    /** Flow lines for the wind layer built from a real u/v grid. */
    fun windLines(
        grid: WindGrid,
        bbox: Bbox,
        maxLines: Int = 800
    ): String {
        val features = mutableListOf<JSONObject>()
        var count = 0
        val step = grid.latStep
        val latStart = maxOf(bbox.south, -85.0)
        val latEnd = minOf(bbox.north, 85.0)
        var lat = latStart
        val stride = if (grid.size > maxLines) 2 else 1
        var idx = 0
        while (lat <= latEnd && count < maxLines) {
            var lon = bbox.west
            val nLons = ((bbox.width / step)).toInt().coerceIn(1, 400)
            repeat(nLons) {
                if (idx % stride == 0) {
                    val u = grid.uValue(lat, lon)
                    val v = grid.vValue(lat, lon)
                    if (u != null && v != null) {
                        val speed = kotlin.math.sqrt(u * u + v * v)
                        if (speed > 1.5) {
                            val scale = 0.6
                            val cosLat = kotlin.math.abs(kotlin.math.cos(Math.toRadians(lat))).coerceAtLeast(0.15)
                            val dLon = u * scale * 0.7 / cosLat
                            val dLat = v * scale
                            val p1 = lon to lat
                            val p2 = (lon + dLon) to (lat + dLat)
                            features += lineFeature(
                                listOf(p1, p2),
                                mapOf("speed" to speed, "type" to "wind")
                            )
                            count++
                        }
                    }
                }
                idx++
                lon += step
            }
            lat += step * stride.coerceAtLeast(1)
        }
        return featureCollection(features)
    }
}

/** In-memory wind grid used by the wind layer renderer. */
class WindGrid(
    val latStep: Double,
    val lonStep: Double,
    val west: Double,
    val south: Double,
    private val u: Map<Long, Float>,
    private val v: Map<Long, Float>
) {
    val size: Int get() = u.size

    fun uValue(lat: Double, lon: Double): Float? {
        val li = ((lat - south) / latStep).toInt()
        val lj = (((lon - west + 360.0) % 360.0) / lonStep).toInt()
        return u[com.earthnow.app.domain.model.GridWeather.encode(li, lj)]
    }

    fun vValue(lat: Double, lon: Double): Float? {
        val li = ((lat - south) / latStep).toInt()
        val lj = (((lon - west + 360.0) % 360.0) / lonStep).toInt()
        return v[com.earthnow.app.domain.model.GridWeather.encode(li, lj)]
    }
}