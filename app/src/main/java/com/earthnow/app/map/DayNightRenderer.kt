package com.earthnow.app.map

import com.earthnow.app.util.SunMoon
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Renders the real day/night terminator as a translucent night-side
 * polygon, computed from the actual solar position (NOAA/Meeus ephemeris —
 * no invented data).
 */
object DayNightRenderer {

    fun nightOverlay(utcMillis: Long): Pair<String, Float> {
        val decRad = Math.toRadians(SunMoon.solarDeclinationDeg(utcMillis))
        val raDeg = SunMoon.solarRightAscensionDeg(utcMillis)
        val gmst = SunMoon.gmstDeg(utcMillis)
        val nightPoleLat = if (decRad > 0) -90.0 else 90.0

        val rings = mutableListOf<List<Pair<Double, Double>>>()

        // Soft edge: several polygons at increasing terminator offsets
        val offsets = listOf(0.0 to 0.45, 2.0 to 0.30, 5.0 to 0.14, 9.0 to 0.05)
        for ((offset, _) in offsets) {
            val terminator = mutableListOf<Pair<Double, Double>>()
            for (lonI in 0..72) {
                val lon = lonI * 5.0
                val lat = terminatorLat(lon, gmst, raDeg, decRad, offset)
                terminator += lon to lat
            }
            // Close the polygon through the night pole
            terminator += (180.0 to nightPoleLat)
            terminator += (360.0 to terminator.first().second)
            rings += terminator
        }

        val features = rings.map { ring ->
            GeoJsonBuilder.polygonFeature(listOf(ring), mapOf("kind" to "night"))
        }
        return GeoJsonBuilder.featureCollection(features) to 0.45f
    }

    private fun terminatorLat(lonDeg: Double, gmstDeg: Double, raDeg: Double, decRad: Double, altDeg: Double): Double {
        // Binary search for latitude where solar elevation == altDeg at given longitude
        val hraDeg = ((gmstDeg + lonDeg - raDeg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        var lo = -89.5
        var hi = 89.5
        repeat(40) {
            val mid = (lo + hi) / 2
            val e = elevation(mid, hraDeg, decRad)
            if (e > altDeg) {
                lo = mid
            } else {
                hi = mid
            }
        }
        return (lo + hi) / 2
    }

    private fun elevation(latDeg: Double, hraDeg: Double, decRad: Double): Double {
        val lat = Math.toRadians(latDeg)
        val hra = Math.toRadians(hraDeg)
        return kotlin.math.asin(
            sin(lat) * sin(decRad) + cos(lat) * cos(decRad) * cos(hra)
        )
    }
}