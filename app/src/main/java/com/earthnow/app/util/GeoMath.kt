package com.earthnow.app.util

import kotlin.math.*

object GeoMath {
    const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    fun normalizeLon(lon: Double): Double = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun compassDirection(degrees: Double): String {
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val idx = ((degrees + 11.25) / 22.5).toInt() % 16
        return dirs[idx]
    }
}

data class Bbox(val west: Double, val south: Double, val east: Double, val north: Double) {
    /** Longitude span in degrees, correctly handling the full-world and
     *  anti-meridian cases (west=-180/east=180 must yield 360, not 0). */
    val width: Double get() {
        val w = (east - west) % 360.0
        return if (w <= 0.0) w + 360.0 else w
    }
    val height: Double get() = (north - south).coerceIn(0.0, 180.0)
    val centerLat: Double get() = (south + north) / 2
    val centerLon: Double get() {
        var c = west + width / 2
        if (c > 180) c -= 360
        return c
    }
    fun contains(lat: Double, lon: Double): Boolean =
        lat in south..north && ((lon - west + 360.0) % 360.0) <= width

    companion object {
        fun world(): Bbox = Bbox(-180.0, -85.0, 180.0, 85.0)
    }
}