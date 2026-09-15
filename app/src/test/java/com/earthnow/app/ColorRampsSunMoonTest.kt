package com.earthnow.app

import com.earthnow.app.util.ColorRamps
import com.earthnow.app.util.SunMoon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorRampsTest {

    private fun alpha(color: Int): Int = com.earthnow.app.util.ColorRamps.alpha(color)

    @Test
    fun temperatureRampExtremes() {
        assertTrue(ColorRamps.temperature(-50.0) != ColorRamps.temperature(50.0))
        assertEquals(ColorRamps.temperature(-50.0), ColorRamps.temperature(-100.0)) // clamped
        assertEquals(ColorRamps.temperature(45.0), ColorRamps.temperature(80.0))    // clamped
        assertTrue(alpha(ColorRamps.temperature(-50.0)) == 255) // fully opaque
    }

    @Test
    fun cloudIsTransparentAtZero() {
        assertEquals(0, alpha(ColorRamps.cloud(0.0)))
        assertTrue(alpha(ColorRamps.cloud(100.0)) > 200)
    }

    @Test
    fun auroraOpacityScales() {
        assertEquals(0, alpha(ColorRamps.aurora(0.0)))
        assertTrue(alpha(ColorRamps.aurora(32.0)) > alpha(ColorRamps.aurora(10.0)))
    }
}

class SunMoonTest {

    @Test
    fun noonAtEquatorOnEquinoxHasHighSun() {
        // 2024-03-20 12:00 UTC, equator, 0 lon — near equinox, sun near zenith
        val ts = java.time.Instant.parse("2024-03-20T12:00:00Z").toEpochMilli()
        val pos = SunMoon.sunPosition(0.0, 0.0, ts)
        assertTrue("elevation should be high, got ${pos.elevationDeg}", pos.elevationDeg > 60)
        assertTrue(pos.isDay)
    }

    @Test
    fun nightSideDetected() {
        // 2024-06-21 00:00 UTC — midnight at Greenwich, northern summer
        val midnight = java.time.Instant.parse("2024-06-21T00:00:00Z").toEpochMilli()
        assertTrue(SunMoon.isNight(50.0, 0.0, midnight))
        // 2024-06-21 12:00 UTC — noon at Greenwich; southern winter daytime
        val noon = java.time.Instant.parse("2024-06-21T12:00:00Z").toEpochMilli()
        assertTrue(!SunMoon.isNight(-30.0, 0.0, noon))
    }

    @Test
    fun declinationSeasonalRange() {
        val june = java.time.Instant.parse("2024-06-21T00:00:00Z").toEpochMilli()
        val dec = SunMoon.solarDeclinationDeg(june)
        assertTrue("expected northern declination ~23°, got $dec", dec in 20.0..25.0)
    }

    @Test
    fun moonPhaseInRange() {
        val ts = System.currentTimeMillis()
        val f = SunMoon.moonPhaseFraction(ts)
        assertTrue(f in 0.0..1.0)
        assertTrue(SunMoon.moonPhaseName(f).isNotBlank())
    }
}