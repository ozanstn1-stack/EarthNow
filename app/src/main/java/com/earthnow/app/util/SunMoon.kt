package com.earthnow.app.util

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class SunPosition(val azimuthDeg: Double, val elevationDeg: Double, val isDay: Boolean)

/**
 * Solar position and day/night computation (NOAA / Meeus solar calculator).
 * Used for the real day-night terminator, sun/moon info and night checks.
 */
object SunMoon {

    fun sunPosition(lat: Double, lon: Double, utcMillis: Long): SunPosition {
        val d = utcMillis / 86400000.0 + 2440587.5
        val t = (d - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t

        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t2
        val m = (357.52911 + 35999.05029 * t - 0.0001537 * t2) % 360.0
        val mr = Math.toRadians(m)
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t2
        val c = (1.914602 - 0.004817 * t - 0.000014 * t2) * sin(mr) +
            (0.019993 - 0.000101 * t) * sin(2 * mr) +
            0.000289 * sin(3 * mr)
        val trueLong = l0 + c
        val omega = 125.04 - 1934.136 * t
        val lambda = Math.toRadians(trueLong - 0.00569 - 0.00478 * sin(Math.toRadians(omega)))
        val eps0 = 23.43929111 - 0.013004167 * t - 1.638889e-7 * t2 + 5.036111e-7 * t3
        val eps = Math.toRadians(eps0 + 0.00256 * cos(Math.toRadians(omega)))

        val ra = Math.toDegrees(atan2(cos(eps) * sin(lambda), cos(lambda)))
        val dec = Math.toDegrees(asin(sin(eps) * sin(lambda)))

        val gmst = (280.46061837 + 360.98564736629 * (d - 2451545.0)) % 360.0
        val hra = ((gmst + lon - ra + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

        val alt = Math.toDegrees(
            asin(
                sin(Math.toRadians(lat)) * sin(Math.toRadians(dec)) +
                    cos(Math.toRadians(lat)) * cos(Math.toRadians(dec)) * cos(Math.toRadians(hra))
            )
        )
        val az = Math.toDegrees(
            atan2(
                -sin(Math.toRadians(hra)) * cos(Math.toRadians(dec)),
                cos(Math.toRadians(lat)) * sin(Math.toRadians(dec)) -
                    sin(Math.toRadians(lat)) * cos(Math.toRadians(dec)) * cos(Math.toRadians(hra))
            )
        )
        return SunPosition((az + 360.0) % 360.0, alt, alt > -0.833)
    }

    fun isNight(lat: Double, lon: Double, utcMillis: Long): Boolean =
        !sunPosition(lat, lon, utcMillis).isDay

    /** Apparent solar declination (degrees), used by the terminator renderer. */
    fun solarDeclinationDeg(utcMillis: Long): Double {
        val d = utcMillis / 86400000.0 + 2440587.5
        val t = (d - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t2
        val m = Math.toRadians((357.52911 + 35999.05029 * t - 0.0001537 * t2) % 360.0)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t2) * sin(m) +
            (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
        val omega = 125.04 - 1934.136 * t
        val lambda = Math.toRadians(l0 + c - 0.00569 - 0.00478 * sin(Math.toRadians(omega)))
        val eps0 = 23.43929111 - 0.013004167 * t - 1.638889e-7 * t2 + 5.036111e-7 * t3
        val eps = Math.toRadians(eps0 + 0.00256 * cos(Math.toRadians(omega)))
        return Math.toDegrees(asin(sin(eps) * sin(lambda)))
    }

    fun solarRightAscensionDeg(utcMillis: Long): Double {
        val d = utcMillis / 86400000.0 + 2440587.5
        val t = (d - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t2
        val m = Math.toRadians((357.52911 + 35999.05029 * t - 0.0001537 * t2) % 360.0)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t2) * sin(m) +
            (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
        val omega = 125.04 - 1934.136 * t
        val lambda = Math.toRadians(l0 + c - 0.00569 - 0.00478 * sin(Math.toRadians(omega)))
        val eps0 = 23.43929111 - 0.013004167 * t - 1.638889e-7 * t2 + 5.036111e-7 * t3
        val eps = Math.toRadians(eps0 + 0.00256 * cos(Math.toRadians(omega)))
        return Math.toDegrees(atan2(cos(eps) * sin(lambda), cos(lambda)))
    }

    fun gmstDeg(utcMillis: Long): Double {
        val d = utcMillis / 86400000.0 + 2440587.5
        return (280.46061837 + 360.98564736629 * (d - 2451545.0)) % 360.0
    }

    fun moonPhaseFraction(utcMillis: Long): Double {
        val d = utcMillis / 86400000.0 + 2440587.5
        val age = (d - 2451550.1) % 29.530588853
        return ((age / 29.530588853) + 1.0) % 1.0
    }

    fun moonPhaseName(fraction: Double): String {
        return when {
            fraction < 0.025 -> "New Moon"
            fraction < 0.225 -> "Waxing Crescent"
            fraction < 0.275 -> "First Quarter"
            fraction < 0.475 -> "Waxing Gibbous"
            fraction < 0.525 -> "Full Moon"
            fraction < 0.725 -> "Waning Gibbous"
            fraction < 0.775 -> "Last Quarter"
            fraction < 0.975 -> "Waning Crescent"
            else -> "New Moon"
        }
    }
}