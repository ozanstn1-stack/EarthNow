package com.earthnow.app.domain.model

import java.util.TimeZone

enum class LayerType(
    val id: String,
    val title: String,
    val emoji: String,
    val isRaster: Boolean,
    val sourceLabel: String,
    val updateNote: String,
    val isEvent: Boolean = false
) {
    TEMPERATURE(
        "temperature", "Temperature", "🌡️", true,
        "Open-Meteo (GFS / DWD ICON models)", "Updated hourly (model runs)"
    ),
    PRECIPITATION(
        "precipitation", "Precipitation", "🌧️", true,
        "RainViewer weather radar", "Updated every 10 minutes"
    ),
    CLOUDS(
        "clouds", "Cloud Cover", "☁️", true,
        "Open-Meteo (GFS / DWD ICON models)", "Updated hourly (model runs)"
    ),
    WIND(
        "wind", "Wind", "🌬️", true,
        "Open-Meteo (GFS / DWD ICON models)", "Updated hourly (model runs)"
    ),
    OCEAN_TEMP(
        "ocean_temp", "Ocean Temperature", "🌊", true,
        "Open-Meteo Marine (ERA5 / ICON)", "Updated hourly"
    ),
    WILDFIRES(
        "wildfires", "Wildfires", "🔥", false,
        "NASA LANCE FIRMS (VIIRS)", "Near real-time (satellite overpass)",
        isEvent = true
    ),
    EARTHQUAKES(
        "earthquakes", "Earthquakes", "🌍", false,
        "USGS Earthquake Hazards Program", "Continuous feed",
        isEvent = true
    ),
    VOLCANOES(
        "volcanoes", "Volcanoes", "🌋", false,
        "Smithsonian GVP (static catalog)", "Catalog: updated periodically; no real-time feed",
        isEvent = true
    ),
    AURORA(
        "aurora", "Aurora", "🌌", true,
        "NOAA SWPC (OVATION + Kp index)", "Updated every ~30 minutes"
    ),
    DAY_NIGHT(
        "day_night", "Day / Night", "☀️", false,
        "Calculated (solar position)", "Real time"
    )
}

data class WeatherPoint(
    val lat: Double,
    val lon: Double,
    val timezone: TimeZone,
    val time: Long?,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val humidity: Double?,
    val pressureHpa: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDeg: Double?,
    val windGustsKmh: Double?,
    val cloudCover: Double?,
    val precipitationProbability: Double?,
    val precipitationMm: Double?,
    val weatherCode: Int?,
    val snowProbability: Double?,
    val rainProbability: Double?,
    val visibility: Double?,
    val uvIndex: Double?,
    val isDay: Boolean?,
    val fetchedAt: Long,
    val hourly: List<HourlyPoint> = emptyList(),
    val daily: List<DailyPoint> = emptyList()
) {
    fun codeText(): String? = weatherCode?.let { WmoCodes.text(it) }
    fun codeEmoji(): String? = weatherCode?.let { WmoCodes.emoji(it) }
}

data class HourlyPoint(
    val time: Long,
    val temperatureC: Double?,
    val precipitationProbability: Double?,
    val precipitationMm: Double?,
    val weatherCode: Int?,
    val windSpeedKmh: Double?,
    val cloudCover: Double?
)

data class DailyPoint(
    val date: String,
    val tempMaxC: Double?,
    val tempMinC: Double?,
    val precipitationProbabilityMax: Double?,
    val sunrise: String?,
    val sunset: String?,
    val moonrise: String?,
    val moonset: String?,
    val moonPhase: Double?,
    val weatherCode: Int?
)

object WmoCodes {
    fun text(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "🌨️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}

data class GridWeather(
    val layer: LayerType,
    val latStep: Double,
    val lonStep: Double,
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
    val values: Map<Long, Float>,  // key = encodeGridIndex(latIdx, lonIdx)
    val time: Long,
    val fetchedAt: Long
) {
    companion object {
        fun encode(latIdx: Int, lonIdx: Int): Long =
            (latIdx.toLong() shl 20) or lonIdx.toLong()

        fun latIdxOf(lat: Double, south: Double, step: Double): Int =
            ((lat - south) / step).toInt().coerceIn(0, 10000)
        fun lonIdxOf(lon: Double, west: Double, step: Double): Int =
            (((lon - west + 360.0) % 360.0) / step).toInt().coerceIn(0, 10000)
    }
}

data class Earthquake(
    val id: String,
    val mag: Double?,
    val place: String,
    val lat: Double,
    val lon: Double,
    val depthKm: Double?,
    val timeMillis: Long,
    val url: String?,
    val status: String?,
    val tsunami: Boolean?,
    val felt: Int?,
    val dmin: Double?,
    val magType: String?
)

data class Wildfire(
    val latitude: Double,
    val longitude: Double,
    val brightnessK: Double,
    val acqDate: String,
    val acqTime: String,
    val satellite: String,
    val confidence: String,
    val frp: Double,
    val dayNight: String,
    val source: String
) {
    val detectedAtMillis: Long?
        get() {
            // FIRMS acq_time is minutes-of-day in UTC (e.g. 149 = 02:49)
            val min = acqTime.toIntOrNull() ?: return null
            val h = min / 60
            val m = min % 60
            return try {
                val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                f.parse(String.format("%s %02d:%02d", acqDate, h, m))?.time
            } catch (e: Exception) { null }
        }
}

data class Volcano(
    val name: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val elevationM: Int,
    val evidence: String,
    val lastEruption: String
)

data class AuroraData(
    val observationTime: Long?,
    val forecastTime: Long?,
    val points: List<AuroraPoint>,          // [lon, lat, intensity]
    val kpIndex: Double?,                    // latest estimated Kp
    val kpUpdatedAt: Long?
)

data class AuroraPoint(val lon: Double, val lat: Double, val intensity: Int)

data class RadarFrames(
    val generated: Long,
    val host: String,
    val past: List<RadarFrame>,
    val nowcast: List<RadarFrame>
) {
    val all: List<RadarFrame> get() = past + nowcast
}

data class RadarFrame(val time: Long, val path: String)

data class Place(
    val id: String,
    val name: String,
    val country: String? = null,
    val admin1: String? = null,
    val lat: Double,
    val lon: Double,
    val kind: String,       // city | country | ocean | volcano | region
    val population: Int? = null
)

data class SearchResult(val query: String, val place: Place)

data class LocationContext(
    val place: Place,
    val weather: WeatherPoint?,
    val earthquakes: List<Earthquake>,
    val earthquakesTodayCount: Int,
    val wildfires: List<Wildfire>,
    val volcanoesNearby: List<Volcano>,
    val aurora: AuroraData?,
    val oceanTemp: Double?,
    val fetchedAt: Long
)

data class AiSummary(val text: String, val provider: String, val generatedAt: Long)

data class AiMessage(val role: String, val content: String)

data class DataSourceInfo(
    val layer: LayerType,
    val source: String,
    val updateFrequency: String,
    val lastUpdated: Long?,
    val attribution: String,
    val license: String,
    val requiresApiKey: Boolean
)

data class EventFeedItem(
    val timeMillis: Long?,
    val emoji: String,
    val title: String,
    val detail: String,
    val severity: Int
)

data class WatchRegion(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusKm: Double = 300.0,
    val earthquakeMinMag: Double? = null,
    val notifyWildfire: Boolean = false,
    val notifyVolcano: Boolean = false,
    val notifyAurora: Boolean = false,
    val notifySevereWeather: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationPrefs(
    val enabled: Boolean = false,
    val earthquakeMinMag: Double = 4.5,
    val wildfire: Boolean = false,
    val volcano: Boolean = false,
    val auroraHigh: Boolean = false,
    val severeWeather: Boolean = false
)

data class UserSettings(
    val themeMode: String = "dark",              // dark | light | system
    val languageMode: String = "system",         // system | en | tr
    val mapStyle: String = "space",              // space | satellite | streets
    val defaultLayers: Set<String> = emptySet(),
    val tempUnit: com.earthnow.app.util.Units.TempUnit = com.earthnow.app.util.Units.TempUnit.CELSIUS,
    val windUnit: com.earthnow.app.util.Units.WindUnit = com.earthnow.app.util.Units.WindUnit.KMH,
    val pressureUnit: com.earthnow.app.util.Units.PressureUnit = com.earthnow.app.util.Units.PressureUnit.HPA,
    val distUnit: com.earthnow.app.util.Units.DistUnit = com.earthnow.app.util.Units.DistUnit.KM,
    val refreshMinutes: Int = 30,
    val batterySaver: Boolean = false,
    val aiEnabled: Boolean = true,
    val aiProvider: String = "auto",             // auto | openai | gemini
    val notificationPrefs: NotificationPrefs = NotificationPrefs(),
    val onboardingDone: Boolean = false,
    val locationEnabled: Boolean = false,
    val liveModeWarningShown: Boolean = false
)