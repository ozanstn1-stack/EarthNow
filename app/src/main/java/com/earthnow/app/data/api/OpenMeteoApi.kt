package com.earthnow.app.data.api

import com.earthnow.app.data.remote.dto.GeocodingDto
import com.earthnow.app.data.remote.dto.OpenMeteoForecastDto
import com.earthnow.app.data.remote.dto.OpenMeteoGridItemDto
import com.earthnow.app.data.remote.dto.OpenMeteoGridMarineItemDto
import com.earthnow.app.data.remote.dto.OpenMeteoMarineDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("current") current: String,
        @Query("hourly") hourly: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 3,
        @Query("past_days") pastDays: Int = 1
    ): OpenMeteoForecastDto

    @GET("v1/forecast")
    suspend fun gridForecast(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("hourly") hourly: String,
        @Query("timezone") timezone: String = "GMT",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("past_days") pastDays: Int = 1,
        @Query("models") models: String = "gfs_seamless"
    ): List<OpenMeteoGridItemDto>

    @GET("v1/marine")
    suspend fun marine(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("hourly") hourly: String = "sea_surface_temperature",
        @Query("timezone") timezone: String = "GMT",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("past_days") pastDays: Int = 1
    ): OpenMeteoMarineDto

    @GET("v1/marine")
    suspend fun gridMarine(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("hourly") hourly: String = "sea_surface_temperature",
        @Query("timezone") timezone: String = "GMT",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("past_days") pastDays: Int = 1
    ): List<OpenMeteoGridMarineItemDto>
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingDto
}