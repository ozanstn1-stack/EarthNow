package com.earthnow.app.data.api

import com.earthnow.app.data.remote.dto.ChatCompletionRequest
import com.earthnow.app.data.remote.dto.ChatCompletionResponse
import com.earthnow.app.data.remote.dto.GeminiRequest
import com.earthnow.app.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatCompletionRequest): ChatCompletionResponse
}

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @retrofit2.http.Path("model") model: String,
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

interface FirmsApi {
    @GET("api/area/csv/{mapKey}/{source}/{area}/{dayRange}")
    suspend fun areaCsv(
        @retrofit2.http.Path("mapKey") mapKey: String,
        @retrofit2.http.Path("source") source: String,
        @retrofit2.http.Path("area") area: String,
        @retrofit2.http.Path("dayRange") dayRange: Int = 1
    ): retrofit2.Response<String>
}