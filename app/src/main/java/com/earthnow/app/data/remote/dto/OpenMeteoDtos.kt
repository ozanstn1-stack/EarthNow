package com.earthnow.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastDto(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "current") val current: CurrentDto? = null,
    @Json(name = "current_units") val currentUnits: Map<String, String>? = null,
    @Json(name = "hourly") val hourly: HourlyDto? = null,
    @Json(name = "hourly_units") val hourlyUnits: Map<String, String>? = null,
    @Json(name = "daily") val daily: DailyDto? = null,
    @Json(name = "error") val error: Boolean? = null,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Double? = null,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
    @Json(name = "cloud_cover") val cloudCover: Double? = null,
    @Json(name = "precipitation") val precipitation: Double? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: Double? = null,
    @Json(name = "rain") val rain: Double? = null,
    @Json(name = "snowfall") val snowfall: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "visibility") val visibility: Double? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null,
    @Json(name = "is_day") val isDay: Int? = null,
    @Json(name = "interval") val interval: Int? = null
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Double?> = emptyList(),
    @Json(name = "precipitation") val precipitation: List<Double?> = emptyList(),
    @Json(name = "rain") val rain: List<Double?> = emptyList(),
    @Json(name = "snowfall") val snowfall: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Double?> = emptyList(),
    @Json(name = "wind_u_component_10m") val windU10m: List<Double?> = emptyList(),
    @Json(name = "wind_v_component_10m") val windV10m: List<Double?> = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m_max") val tempMax: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val tempMin: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipProbMax: List<Double?> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<String?> = emptyList(),
    @Json(name = "sunset") val sunset: List<String?> = emptyList(),
    @Json(name = "moonrise") val moonrise: List<String?> = emptyList(),
    @Json(name = "moonset") val moonset: List<String?> = emptyList(),
    @Json(name = "moon_phase") val moonPhase: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGridItemDto(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "hourly") val hourly: HourlyDto? = null,
    @Json(name = "error") val error: Boolean? = null,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoMarineDto(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "hourly") val hourly: MarineHourlyDto? = null,
    @Json(name = "error") val error: Boolean? = null,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class MarineHourlyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "sea_surface_temperature") val seaSurfaceTemperature: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGridMarineItemDto(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "hourly") val hourly: MarineHourlyDto? = null,
    @Json(name = "error") val error: Boolean? = null,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingDto(
    @Json(name = "results") val results: List<GeocodingResultDto> = emptyList(),
    @Json(name = "error") val error: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "feature_code") val featureCode: String? = null,
    @Json(name = "population") val population: Int? = null
)