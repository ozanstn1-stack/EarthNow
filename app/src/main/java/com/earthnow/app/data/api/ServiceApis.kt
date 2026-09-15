package com.earthnow.app.data.api

import com.earthnow.app.data.remote.dto.NoaaKpDto
import com.earthnow.app.data.remote.dto.NoaaOvationDto
import com.earthnow.app.data.remote.dto.UsgsFeedDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UsgsApi {
    @GET("earthquakes/feed/v1.0/summary/all_day.geojson")
    suspend fun allDay(): UsgsFeedDto

    @GET("earthquakes/feed/v1.0/summary/all_week.geojson")
    suspend fun allWeek(): UsgsFeedDto

    @GET("earthquakes/feed/v1.0/summary/4.5_day.geojson")
    suspend fun significantDay(): UsgsFeedDto
}

interface NoaaSwpcApi {
    @GET("json/planetary_k_index_1m.json")
    suspend fun kpIndex(): List<NoaaKpDto>

    @GET("json/ovation_aurora_latest.json")
    suspend fun ovation(): NoaaOvationDto
}

interface RainViewerApi {
    @GET("public/weather-maps.json")
    suspend fun weatherMaps(): com.earthnow.app.data.remote.dto.RainViewerMapsDto
}