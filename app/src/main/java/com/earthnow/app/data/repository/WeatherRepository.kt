package com.earthnow.app.data.repository

import com.earthnow.app.data.api.OpenMeteoApi
import com.earthnow.app.data.remote.dto.DailyDto
import com.earthnow.app.data.remote.dto.HourlyDto
import com.earthnow.app.data.remote.dto.OpenMeteoForecastDto
import com.earthnow.app.domain.model.DailyPoint
import com.earthnow.app.domain.model.HourlyPoint
import com.earthnow.app.domain.model.WeatherPoint
import com.earthnow.app.util.TimeFormat
import com.squareup.moshi.Moshi
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val cache: JsonCache,
    private val moshi: Moshi
) {
    private val ttl = 20 * 60_000L

    suspend fun current(lat: Double, lon: Double): WeatherPoint =
        withContext(Dispatchers.IO) {
            val key = "weather_point_${lat.round2()}_${lon.round2()}"
            cache.getStale(key)?.let { (json, ts) ->
                val dto = try { moshi.adapter(OpenMeteoForecastDto::class.java).fromJson(json) } catch (e: Exception) { null }
                if (dto != null && System.currentTimeMillis() - ts < ttl) {
                    return@withContext dto.toDomain(lat, lon, ts)
                }
            }
            val dto = api.forecast(
                latitude = lat.toString(),
                longitude = lon.toString(),
                current = "temperature_2m,apparent_temperature,relative_humidity_2m,surface_pressure," +
                    "wind_speed_10m,wind_direction_10m,wind_gusts_10m,cloud_cover,precipitation," +
                    "precipitation_probability,rain,snowfall,weather_code,visibility,uv_index,is_day",
                hourly = "temperature_2m,precipitation_probability,precipitation,weather_code," +
                    "wind_speed_10m,cloud_cover",
                daily = "temperature_2m_max,temperature_2m_min,precipitation_probability_max," +
                    "sunrise,sunset,moonrise,moonset,moon_phase,weather_code",
                forecastDays = 3,
                pastDays = 1
            )
            if (dto.error == true) throw IllegalStateException(dto.reason ?: "Open-Meteo error")
            val json = moshi.adapter(OpenMeteoForecastDto::class.java).toJson(dto)
            cache.put(key, json)
            dto.toDomain(lat, lon, System.currentTimeMillis())
        }

    private fun OpenMeteoForecastDto.toDomain(lat: Double, lon: Double, fetchedAt: Long): WeatherPoint {
        val cur = current
        return WeatherPoint(
            lat = lat,
            lon = lon,
            timezone = runCatching { TimeZone.getTimeZone(timezone ?: "UTC") }.getOrDefault(TimeZone.getTimeZone("UTC")),
            time = cur?.time?.let { TimeFormat.isoToMillis(it) },
            temperatureC = cur?.temperature2m,
            feelsLikeC = cur?.apparentTemperature,
            humidity = cur?.relativeHumidity2m,
            pressureHpa = cur?.surfacePressure,
            windSpeedKmh = cur?.windSpeed10m,
            windDirectionDeg = cur?.windDirection10m,
            windGustsKmh = cur?.windGusts10m,
            cloudCover = cur?.cloudCover,
            precipitationProbability = cur?.precipitationProbability,
            precipitationMm = cur?.precipitation,
            weatherCode = cur?.weatherCode,
            snowProbability = cur?.snowfall?.let { if (it > 0) 100.0 else 0.0 },
            rainProbability = cur?.rain?.let { if (it > 0) 100.0 else cur.precipitationProbability },
            visibility = cur?.visibility,
            uvIndex = cur?.uvIndex,
            isDay = cur?.isDay?.let { it == 1 },
            fetchedAt = fetchedAt,
            hourly = hourly?.toHourly() ?: emptyList(),
            daily = daily?.toDaily() ?: emptyList()
        )
    }

    private fun HourlyDto.toHourly(): List<HourlyPoint> {
        val n = minOf(
            time.size, temperature2m.size, precipitationProbability.size,
            weatherCode.size, windSpeed10m.size, cloudCover.size, precipitation.size
        )
        return (0 until n).mapNotNull { i ->
            val ts = TimeFormat.isoToMillis(time[i]) ?: return@mapNotNull null
            HourlyPoint(
                time = ts,
                temperatureC = temperature2m.getOrNull(i),
                precipitationProbability = precipitationProbability.getOrNull(i),
                precipitationMm = precipitation.getOrNull(i),
                weatherCode = weatherCode.getOrNull(i),
                windSpeedKmh = windSpeed10m.getOrNull(i),
                cloudCover = cloudCover.getOrNull(i)
            )
        }
    }

    private fun DailyDto.toDaily(): List<DailyPoint> {
        val n = minOf(
            time.size, tempMax.size, tempMin.size, precipProbMax.size,
            sunrise.size, sunset.size, moonrise.size, moonset.size, moonPhase.size
        )
        return (0 until n).map { i ->
            DailyPoint(
                date = time[i],
                tempMaxC = tempMax.getOrNull(i),
                tempMinC = tempMin.getOrNull(i),
                precipitationProbabilityMax = precipProbMax.getOrNull(i),
                sunrise = sunrise.getOrNull(i),
                sunset = sunset.getOrNull(i),
                moonrise = moonrise.getOrNull(i),
                moonset = moonset.getOrNull(i),
                moonPhase = moonPhase.getOrNull(i),
                weatherCode = weatherCode.getOrNull(i)
            )
        }
    }

    private fun Double.round2(): Double = kotlin.math.round(this * 100.0) / 100.0
}