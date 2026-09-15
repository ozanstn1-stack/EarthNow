package com.earthnow.app

import com.earthnow.app.util.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun celsiusToFahrenheit() {
        assertEquals(32.0, Units.temp(0.0, Units.TempUnit.FAHRENHEIT), 0.01)
        assertEquals(98.6, Units.temp(37.0, Units.TempUnit.FAHRENHEIT), 0.01)
        assertEquals(7.0, Units.temp(7.0, Units.TempUnit.CELSIUS), 0.01)
    }

    @Test
    fun kmhToMphAndKnots() {
        assertEquals(0.0, Units.wind(0.0, Units.WindUnit.MPH), 0.01)
        assertEquals(31.0, Units.wind(49.89, Units.WindUnit.MPH), 0.5)
        assertEquals(31.0, Units.wind(57.4, Units.WindUnit.KNOTS), 0.5)
        assertEquals(31.0, Units.wind(31.0, Units.WindUnit.KMH), 0.01)
    }

    @Test
    fun pressureConversion() {
        assertEquals("1014 hPa", Units.pressure(1014.0, Units.PressureUnit.HPA))
        assertEquals(29.94, Units.pressure(1014.0, Units.PressureUnit.INHG).let { s ->
            s.removeSuffix(" inHg").toDouble()
        }, 0.02)
    }

    @Test
    fun distanceConversion() {
        assertEquals("10 km", Units.dist(10.0, Units.DistUnit.KM))
        assertEquals("9.3 mi", Units.dist(15.0, Units.DistUnit.MILES))
        assertEquals("1.2 km", Units.dist(1.2, Units.DistUnit.KM))
    }

    @Test
    fun tempLabelFormatting() {
        assertEquals("24°C", Units.tempLabel(24.2, Units.TempUnit.CELSIUS))
        assertEquals("75°F", Units.tempLabel(24.0, Units.TempUnit.FAHRENHEIT))
    }
}