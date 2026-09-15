package com.earthnow.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.earthnow.app.domain.model.AuroraPoint
import com.earthnow.app.domain.model.GridWeather
import com.earthnow.app.util.ColorRamps
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Builds GeoJSON for gridded data layers. Vector fills project correctly on
 * the globe (image sources degenerate at the poles), so every raster-style
 * overlay is rendered as a FeatureCollection of colored cells.
 */
object RasterRenderer {

    private const val LAT_MAX = 85.0

    /** One polygon feature per grid cell, precolored via the ramp. */
    fun gridToGeoJson(grid: GridWeather, ramp: (Double) -> Int): String {
        val features = JSONArray()
        val step = grid.lonStep
        val nLats = ((grid.north - grid.south) / grid.latStep).roundToInt().coerceIn(1, 200)
        val lonSpan = run {
            val w = (grid.east - grid.west) % 360.0
            if (w <= 0.0) w + 360.0 else w
        }
        val nLons = (lonSpan / step).roundToInt().coerceIn(1, 400)

        for (li in 0 until nLats) {
            val lat1 = (grid.south + li * grid.latStep).coerceIn(-LAT_MAX, LAT_MAX)
            val lat2 = (grid.south + (li + 1) * grid.latStep).coerceIn(-LAT_MAX, LAT_MAX)
            if (lat2 - lat1 < 0.01) continue
            for (lj in 0 until nLons) {
                val value = grid.values[GridWeather.encode(li, lj)] ?: continue
                val color = colorToCss(ramp(value.toDouble()))
                val lon1 = normalizeLon(grid.west + lj * step)
                val lon2 = normalizeLon(grid.west + (lj + 1) * step)
                if (lon2 <= lon1) {
                    // Cell crosses the anti-meridian: two features.
                    addCell(features, lon1, lat1, 180.0, lat2, color)
                    addCell(features, -180.0, lat1, lon2, lat2, color)
                } else {
                    addCell(features, lon1, lat1, lon2, lat2, color)
                }
            }
        }
        android.util.Log.d(
            "EarthNowGrid",
            "gridToGeoJson: ${features.length()} cells, step=${grid.latStep}, west=${grid.west}, south=${grid.south}, east=${grid.east}, north=${grid.north}"
        )
        return featureCollection(features)
    }

    /** Aurora oval: OVATION points (lon 0..359, lat -90..90, intensity 0..32). */
    fun auroraToGeoJson(points: List<AuroraPoint>, max: Double = 32.0): String {
        val features = JSONArray()
        for (p in points) {
            if (p.intensity <= 0) continue
            val lat = p.lat.coerceIn(-LAT_MAX, LAT_MAX)
            if (p.lat < -LAT_MAX || p.lat > LAT_MAX) continue
            val lon1 = normalizeLon(p.lon - 0.5)
            val lon2 = normalizeLon(p.lon + 0.5)
            val color = colorToCss(ColorRamps.aurora(p.intensity.toDouble(), max))
            val lat1 = (lat - 0.5).coerceAtLeast(-LAT_MAX)
            val lat2 = (lat + 0.5).coerceAtMost(LAT_MAX)
            if (lon2 > lon1) {
                addCell(features, lon1, lat1, lon2, lat2, color, outline = true)
            } else {
                addCell(features, lon1, lat1, 180.0, lat2, color, outline = true)
                addCell(features, -180.0, lat1, lon2, lat2, color, outline = true)
            }
        }
        return featureCollection(features)
    }

    private fun addCell(
        features: JSONArray,
        lon1: Double,
        lat1: Double,
        lon2: Double,
        lat2: Double,
        color: String,
        outline: Boolean = false
    ) {
        if (lon2 - lon1 < 0.001 || lat2 - lat1 < 0.001) return
        val ring = JSONArray()
        ring.put(JSONArray().put(lon1).put(lat1))
        ring.put(JSONArray().put(lon2).put(lat1))
        ring.put(JSONArray().put(lon2).put(lat2))
        ring.put(JSONArray().put(lon1).put(lat2))
        ring.put(JSONArray().put(lon1).put(lat1))

        val geometry = JSONObject()
        geometry.put("type", "Polygon")
        geometry.put("coordinates", JSONArray().put(ring))

        val props = JSONObject()
        props.put("c", color)
        if (outline) props.put("o", 1)

        val feature = JSONObject()
        feature.put("type", "Feature")
        feature.put("geometry", geometry)
        feature.put("properties", props)
        features.put(feature)
    }

    private fun featureCollection(features: JSONArray): String {
        val fc = JSONObject()
        fc.put("type", "FeatureCollection")
        fc.put("features", features)
        return fc.toString()
    }

    private fun normalizeLon(lon: Double): Double {
        var l = lon % 360.0
        if (l < -180.0) l += 360.0
        if (l > 180.0) l -= 360.0
        return l
    }

    /** #AARRGGBB int -> rgba() string understood by MapLibre expressions. */
    fun colorToCss(color: Int): String {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF
        val alpha = (a / 255.0)
        return "rgba($r,$g,$b,$alpha)"
    }

    /** Kept for the settings legend and any bitmap needs (e.g. sharing). */
    fun gridToBitmap(grid: GridWeather, ramp: (Double) -> Int): Bitmap {
        val nLats = ((grid.north - grid.south) / grid.latStep).roundToInt().coerceIn(1, 200)
        val lonSpan = run {
            val w = (grid.east - grid.west) % 360.0
            if (w <= 0.0) w + 360.0 else w
        }
        val nLons = (lonSpan / grid.lonStep).roundToInt().coerceIn(1, 400)
        val w = (nLons * 3).coerceIn(1, 1200)
        val h = (nLats * 3).coerceIn(1, 600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val cellW = w.toFloat() / nLons
        val cellH = h.toFloat() / nLats
        for (li in 0 until nLats) {
            for (lj in 0 until nLons) {
                val v = grid.values[GridWeather.encode(li, lj)] ?: continue
                paint.color = ramp(v.toDouble())
                canvas.drawRect(lj * cellW, li * cellH, (lj + 1) * cellW, (li + 1) * cellH, paint)
            }
        }
        return bmp
    }

    /** Painter for the Compose legend. */
    fun legendColors(ramp: (Double) -> Int, min: Double, max: Double, steps: Int = 12): List<Int> =
        (0 until steps).map { i -> ramp(min + (max - min) * i / (steps - 1)) }
}