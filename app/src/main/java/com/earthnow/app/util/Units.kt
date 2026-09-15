package com.earthnow.app.util

import kotlin.math.roundToInt

object Units {

    enum class TempUnit(val label: String) { CELSIUS("°C"), FAHRENHEIT("°F") }
    enum class WindUnit(val label: String) { KMH("km/h"), MPH("mph"), KNOTS("knots") }
    enum class PressureUnit(val label: String) { HPA("hPa"), INHG("inHg") }
    enum class DistUnit(val label: String) { KM("km"), MILES("mi") }

    fun temp(celsius: Double, unit: TempUnit): Double = when (unit) {
        TempUnit.CELSIUS -> celsius
        TempUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    }

    fun tempLabel(celsius: Double, unit: TempUnit): String =
        "${temp(celsius, unit).roundToInt()}${unit.label}"

    fun wind(kmh: Double, unit: WindUnit): Double = when (unit) {
        WindUnit.KMH -> kmh
        WindUnit.MPH -> kmh * 0.621371
        WindUnit.KNOTS -> kmh * 0.539957
    }

    fun windLabel(kmh: Double, unit: WindUnit): String =
        "${wind(kmh, unit).roundToInt()} ${unit.label}"

    fun pressure(hpa: Double, unit: PressureUnit): String = when (unit) {
        PressureUnit.HPA -> "${hpa.roundToInt()} hPa"
        PressureUnit.INHG -> String.format(java.util.Locale.US, "%.2f inHg", hpa * 0.0295299830714)
    }

    fun dist(km: Double, unit: DistUnit): String {
        val v = if (unit == DistUnit.KM) km else km * 0.621371
        return if (v >= 10) "${v.roundToInt()} ${unit.label}"
        else String.format(java.util.Locale.US, "%.1f %s", v, unit.label)
    }
}