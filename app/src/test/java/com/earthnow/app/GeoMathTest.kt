package com.earthnow.app

import com.earthnow.app.util.GeoMath
import com.earthnow.app.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun haversineKnownDistance() {
        // Istanbul -> Ankara is approximately 350 km
        val d = GeoMath.haversineKm(41.0082, 28.9784, 39.9334, 32.8597)
        assertTrue("Expected ~350 km, got $d", d in 300.0..420.0)
        // Same point
        assertEquals(0.0, GeoMath.haversineKm(41.0, 29.0, 41.0, 29.0), 0.001)
    }

    @Test
    fun compassDirections() {
        assertEquals("N", GeoMath.compassDirection(0.0))
        assertEquals("E", GeoMath.compassDirection(90.0))
        assertEquals("S", GeoMath.compassDirection(180.0))
        assertEquals("W", GeoMath.compassDirection(270.0))
        assertEquals("NW", GeoMath.compassDirection(315.0))
    }

    @Test
    fun bearingEast() {
        val b = GeoMath.bearing(40.0, 0.0, 40.0, 10.0)
        assertTrue(b in 85.0..95.0)
    }

    @Test
    fun normalizeLon() {
        assertEquals(170.0, GeoMath.normalizeLon(170.0), 0.001)
        assertEquals(-170.0, GeoMath.normalizeLon(190.0), 0.001)
    }
}

class TimeFormatTest {

    @Test
    fun agoFormats() {
        val now = 1_700_000_000_000L
        assertEquals("just now", TimeFormat.ago(now - 5_000, now))
        assertEquals("1 min ago", TimeFormat.ago(now - 60_000, now))
        assertEquals("31 min ago", TimeFormat.ago(now - 31 * 60_000, now))
        assertEquals("2 h 5 min ago", TimeFormat.ago(now - 125 * 60_000, now))
        assertEquals("2 d ago", TimeFormat.ago(now - 48 * 3600_000, now))
    }
}