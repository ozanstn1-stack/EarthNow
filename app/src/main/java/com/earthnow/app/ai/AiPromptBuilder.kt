package com.earthnow.app.ai

import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.util.GeoMath
import com.earthnow.app.util.TimeFormat
import com.earthnow.app.util.Units
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Builds structured prompts from real data only. The JSON passed to the
 * AI contains exactly the values collected from the data sources — the AI
 * is instructed to base its answer exclusively on this payload.
 */
object AiPromptBuilder {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

    private const val BASE_SYSTEM_PROMPT = """You are a concise Earth-observation assistant inside a live Earth explorer app.
RULES (must always be followed):
1. Use ONLY the structured data provided in the user message. Never invent, guess, or recall any measurement, event, or forecast.
2. If a field is null or missing, say "Data unavailable" for that item — do not estimate.
3. Weather data is a model forecast; aurora estimates are model-based; earthquake/wildfire/volcano data come from official feeds. Express appropriate uncertainty ("may", "estimated", "reported").
4. Never provide definitive safety advice about earthquakes, volcanoes, storms, or other hazards. If the user asks about danger, say: "For safety guidance, check your local official authorities and emergency services." and keep it brief.
5. Answer in 3-6 sentences for summaries, or a short paragraph for questions. Use plain language.
6. Prefer metric units as provided; convert only when the user asks.
7. Answer in the language given by the `language` field: "tr" means Turkish, "en" means English. If the field is missing, answer in English."""

    fun summarySystemPrompt(language: String = "en"): String =
        BASE_SYSTEM_PROMPT + if (language == "tr") "\n\nThe app language is Turkish. Write the entire answer in Turkish." else ""

    fun summaryPayload(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String = "en"
    ): String {
        val w = context.weather
        val aurora = context.aurora
        val kp = aurora?.kpIndex
        val auroraLabel = when {
            kp == null -> "unknown"
            kp >= 7 -> "very high"
            kp >= 5 -> "high"
            kp >= 3 -> "moderate"
            else -> "low"
        }
        val data = linkedMapOf<String, Any?>(
            "place" to context.place.name,
            "country" to context.place.country,
            "location_type" to context.place.kind,
            "temperature_celsius" to w?.temperatureC,
            "feels_like_celsius" to w?.feelsLikeC,
            "wind_kmh" to w?.windSpeedKmh,
            "wind_gusts_kmh" to w?.windGustsKmh,
            "wind_direction_deg" to w?.windDirectionDeg,
            "cloud_cover_percent" to w?.cloudCover,
            "rain_probability_percent" to w?.rainProbability,
            "precipitation_mm" to w?.precipitationMm,
            "humidity_percent" to w?.humidity,
            "pressure_hpa" to w?.pressureHpa,
            "weather_code" to w?.weatherCode,
            "weather_code_text" to w?.codeText(),
            "earthquakes_last_24h_within_1500km" to context.earthquakesTodayCount,
            "largest_earthquake_magnitude" to context.earthquakes.firstOrNull()?.mag,
            "wildfires_nearby_count" to context.wildfires.size,
            "volcanoes_nearby" to context.volcanoesNearby.map { v -> mapOf(
                "name" to v.name, "country" to v.country, "evidence" to v.evidence
            ) },
            "aurora_kp_index" to kp,
            "aurora_estimated_visibility" to auroraLabel,
            "ocean_surface_temperature_celsius" to context.oceanTemp,
            "language" to language,
            "data_updated_at" to TimeFormat.ago(context.fetchedAt)
        )
        @Suppress("UNCHECKED_CAST")
        val adapter = moshi.adapter<Any>(mapType) as com.squareup.moshi.JsonAdapter<Map<String, Any?>>
        return adapter.toJson(data)
    }

    fun questionSystemPrompt(language: String = "en"): String =
        BASE_SYSTEM_PROMPT + if (language == "tr") "\n\nThe app language is Turkish. Write the entire answer in Turkish." else ""

    fun questionPayload(question: String, dataContext: String): String =
        """Question from the user: "$question"

Structured data context currently shown in the app (real values from official sources only):
$dataContext"""

    fun dataContextText(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit
    ): String {
        val w = context.weather
        val aurora = context.aurora
        val sb = StringBuilder()
        sb.append("- Location: ${context.place.name}, ${context.place.country ?: "unknown country"}\n")
        sb.append("- Temperature: ${w?.temperatureC?.let { com.earthnow.app.util.Units.tempLabel(it, tempUnit) } ?: "unavailable"}\n")
        sb.append("- Wind: ${w?.windSpeedKmh?.let { com.earthnow.app.util.Units.windLabel(it, windUnit) } ?: "unavailable"}\n")
        sb.append("- Cloud cover: ${w?.cloudCover?.let { "${it.toInt()}%" } ?: "unavailable"}\n")
        sb.append("- Rain probability: ${w?.rainProbability?.let { "${it.toInt()}%" } ?: "unavailable"}\n")
        sb.append("- Earthquakes today (within 1500 km): ${context.earthquakesTodayCount}\n")
        sb.append("- Largest nearby earthquake: M${context.earthquakes.firstOrNull()?.mag ?: "n/a"}\n")
        sb.append("- Wildfires nearby: ${context.wildfires.size}\n")
        sb.append("- Volcanoes nearby: ${context.volcanoesNearby.joinToString { v -> "${v.name} (${v.evidence})" }.ifEmpty { "none" }}\n")
        sb.append("- Aurora: Kp ${aurora?.kpIndex ?: "unavailable"} (estimated visibility: ${when { aurora?.kpIndex == null -> "unknown"; aurora.kpIndex >= 5 -> "high"; aurora.kpIndex >= 3 -> "moderate"; else -> "low" }})\n")
        sb.append("- Ocean surface temperature: ${context.oceanTemp?.let { "${it}°C" } ?: "not applicable or unavailable"}\n")
        return sb.toString()
    }

    fun eventsForFeed(context: LocationContext): List<com.earthnow.app.domain.model.EventFeedItem> {
        val items = mutableListOf<com.earthnow.app.domain.model.EventFeedItem>()
        val now = System.currentTimeMillis()
        context.earthquakes.filter { now - it.timeMillis <= 24 * 3600_000L }.take(8).forEach { q ->
            items += com.earthnow.app.domain.model.EventFeedItem(
                timeMillis = q.timeMillis,
                emoji = "🌍",
                title = "M${q.mag?.let { String.format("%.1f", it) } ?: "?"} earthquake",
                detail = q.place,
                severity = when { (q.mag ?: 0.0) >= 6 -> 3; (q.mag ?: 0.0) >= 5 -> 2; else -> 1 }
            )
        }
        context.wildfires.take(8).forEach { f ->
            items += com.earthnow.app.domain.model.EventFeedItem(
                timeMillis = f.detectedAtMillis,
                emoji = "🔥",
                title = "Wildfire detected (${f.satellite})",
                detail = "Brightness ${f.brightnessK.toInt()}K, confidence ${f.confidence}",
                severity = 2
            )
        }
        context.volcanoesNearby.take(5).forEach { v ->
            items += com.earthnow.app.domain.model.EventFeedItem(
                timeMillis = null,
                emoji = "🌋",
                title = "${v.name} — ${v.evidence}",
                detail = "${v.country} · ${v.region}. Static GVP catalog, not real-time.",
                severity = 1
            )
        }
        val w = context.weather
        if (w != null && (w.rainProbability ?: 0.0) >= 60 && w.precipitationMm ?: 0.0 >= 5.0) {
            items += com.earthnow.app.domain.model.EventFeedItem(
                timeMillis = w.time,
                emoji = "🌧️",
                title = "Heavy rain expected",
                detail = "Rain probability ${w.rainProbability?.toInt()}% · ${w.precipitationMm} mm",
                severity = 2
            )
        }
        return items.sortedByDescending { it.timeMillis ?: 0L }
    }

    fun nearbyStats(context: LocationContext): Map<String, Any?> {
        val w = context.weather
        val aurora = context.aurora
        return linkedMapOf(
            "earthquakes_today" to context.earthquakesTodayCount,
            "wildfires_nearby" to context.wildfires.size,
            "volcanoes_nearby" to context.volcanoesNearby.size,
            "aurora_kp" to aurora?.kpIndex,
            "rain_probability" to w?.rainProbability,
            "wind_kmh" to w?.windSpeedKmh,
            "temperature_celsius" to w?.temperatureC,
            "cloud_cover" to w?.cloudCover
        )
    }

    fun nearbyDistanceKm(context: LocationContext, lat: Double, lon: Double): Double? =
        GeoMath.haversineKm(lat, lon, context.place.lat, context.place.lon)
}