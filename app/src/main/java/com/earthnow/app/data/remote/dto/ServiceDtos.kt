package com.earthnow.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UsgsFeedDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "metadata") val metadata: UsgsMetadataDto? = null,
    @Json(name = "features") val features: List<UsgsFeatureDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UsgsMetadataDto(
    @Json(name = "generated") val generated: Long? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "count") val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class UsgsFeatureDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "properties") val properties: UsgsPropertiesDto? = null,
    @Json(name = "geometry") val geometry: UsgsGeometryDto? = null
)

@JsonClass(generateAdapter = true)
data class UsgsPropertiesDto(
    @Json(name = "mag") val mag: Double? = null,
    @Json(name = "place") val place: String? = null,
    @Json(name = "time") val time: Long? = null,
    @Json(name = "updated") val updated: Long? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "detail") val detail: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "tsunami") val tsunami: Int? = null,
    @Json(name = "felt") val felt: Int? = null,
    @Json(name = "cdi") val cdi: Double? = null,
    @Json(name = "dmin") val dmin: Double? = null,
    @Json(name = "magType") val magType: String? = null
)

@JsonClass(generateAdapter = true)
data class UsgsGeometryDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UsgsDetailDto(
    @Json(name = "properties") val properties: UsgsPropertiesDto? = null,
    @Json(name = "geometry") val geometry: UsgsGeometryDto? = null,
    @Json(name = "type") val type: String? = null
)

// NOAA SWPC
@JsonClass(generateAdapter = true)
data class NoaaKpDto(
    @Json(name = "time_tag") val timeTag: String? = null,
    @Json(name = "kp_index") val kpIndex: Int? = null,
    @Json(name = "estimated_kp") val estimatedKp: Double? = null,
    @Json(name = "kp") val kp: String? = null
)

@JsonClass(generateAdapter = true)
data class NoaaOvationDto(
    @Json(name = "Observation Time") val observationTime: String? = null,
    @Json(name = "Forecast Time") val forecastTime: String? = null,
    @Json(name = "coordinates") val coordinates: List<List<Double>> = emptyList()
)

// RainViewer
@JsonClass(generateAdapter = true)
data class RainViewerMapsDto(
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated") val generated: Long? = null,
    @Json(name = "host") val host: String? = null,
    @Json(name = "radar") val radar: RainViewerRadarDto? = null,
    @Json(name = "satellite") val satellite: RainViewerRadarDto? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerRadarDto(
    @Json(name = "past") val past: List<RainViewerFrameDto> = emptyList(),
    @Json(name = "nowcast") val nowcast: List<RainViewerFrameDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RainViewerFrameDto(
    @Json(name = "time") val time: Long? = null,
    @Json(name = "path") val path: String? = null
)

// AI (OpenAI-compatible + Gemini)
@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "temperature") val temperature: Double = 0.3,
    @Json(name = "max_tokens") val maxTokens: Int = 600
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "choices") val choices: List<ChatChoiceDto> = emptyList(),
    @Json(name = "error") val error: ChatErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class ChatChoiceDto(
    @Json(name = "message") val message: ChatMessageDto? = null
)

@JsonClass(generateAdapter = true)
data class ChatErrorDto(
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContentDto>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfigDto = GeminiConfigDto()
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    @Json(name = "parts") val parts: List<GeminiPartDto>
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiConfigDto(
    @Json(name = "temperature") val temperature: Double = 0.3,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 600
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidateDto> = emptyList(),
    @Json(name = "error") val error: GeminiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    @Json(name = "content") val content: GeminiContentDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDto(
    @Json(name = "message") val message: String? = null
)