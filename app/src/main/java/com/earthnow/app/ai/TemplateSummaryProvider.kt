package com.earthnow.app.ai

import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.util.Units
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic fallback summary used when no AI provider is configured
 * or the AI call fails. Built exclusively from real data — clearly labeled
 * as a template, not AI-generated.
 */
@Singleton
class TemplateSummaryProvider @Inject constructor() : AiSummaryProvider {
    override val id = "template"
    override val displayName = "Local template (no AI configured)"

    override suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String
    ): String { if (language == "tr") return summarizeTr(context, tempUnit, windUnit)
        val w = context.weather
        val aurora = context.aurora
        val kp = aurora?.kpIndex
        val sb = StringBuilder()
        sb.append("Current conditions at ${context.place.name}")
        if (!context.place.country.isNullOrBlank()) sb.append(", ${context.place.country}")
        sb.append(":\n")
        w?.let {
            sb.append("Temperature ${it.temperatureC?.let { t -> com.earthnow.app.util.Units.tempLabel(t, tempUnit) } ?: "unavailable"}")
            sb.append(" · Wind ${it.windSpeedKmh?.let { s -> com.earthnow.app.util.Units.windLabel(s, windUnit) } ?: "unavailable"}")
            if (it.windDirectionDeg != null) sb.append(" (${com.earthnow.app.util.GeoMath.compassDirection(it.windDirectionDeg)})")
            sb.append("\nCloud cover ${it.cloudCover?.let { c -> "${c.toInt()}%" } ?: "unavailable"}")
            sb.append(" · Rain probability ${it.rainProbability?.let { r -> "${r.toInt()}%" } ?: "unavailable"}\n")
        }
        sb.append("Earthquakes today within 1500 km: ${context.earthquakesTodayCount}\n")
        sb.append("Wildfires detected nearby: ${context.wildfires.size}\n")
        if (context.volcanoesNearby.isNotEmpty()) {
            sb.append("Nearby volcanoes (static GVP catalog): ${context.volcanoesNearby.joinToString { it.name }}\n")
        } else {
            sb.append("No volcanoes in the static GVP catalog nearby.\n")
        }
        sb.append("Aurora: Kp ${kp?.let { "%.1f".format(it) } ?: "unavailable"}")
        sb.append(" — estimated visibility: ${when { kp == null -> "unknown"; kp >= 5 -> "high"; kp >= 3 -> "moderate"; else -> "low" }}\n")
        if (context.oceanTemp != null) sb.append("Sea surface temperature nearby: ${context.oceanTemp}°C\n")
        sb.append("\nThis summary is generated from live data sources. For safety guidance, check local official authorities and emergency services.")
        return sb.toString()
    }

    private fun summarizeTr(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit
    ): String {
        val w = context.weather
        val kp = context.aurora?.kpIndex
        val sb = StringBuilder()
        sb.append("${context.place.name} için mevcut koşullar")
        if (!context.place.country.isNullOrBlank()) sb.append(", ${context.place.country}")
        sb.append(":\n")
        w?.let {
            sb.append("Sıcaklık ${it.temperatureC?.let { t -> com.earthnow.app.util.Units.tempLabel(t, tempUnit) } ?: "veri yok"}")
            sb.append(" · Rüzgâr ${it.windSpeedKmh?.let { s -> com.earthnow.app.util.Units.windLabel(s, windUnit) } ?: "veri yok"}")
            if (it.windDirectionDeg != null) sb.append(" (${com.earthnow.app.util.GeoMath.compassDirection(it.windDirectionDeg)})")
            sb.append("\nBulutluluk ${it.cloudCover?.let { c -> "${c.toInt()}%" } ?: "veri yok"}")
            sb.append(" · Yağış olasılığı ${it.rainProbability?.let { r -> "${r.toInt()}%" } ?: "veri yok"}\n")
        }
        sb.append("Bugün 1500 km içinde deprem sayısı: ${context.earthquakesTodayCount}\n")
        sb.append("Yakında tespit edilen yangın sayısı: ${context.wildfires.size}\n")
        if (context.volcanoesNearby.isNotEmpty()) {
            sb.append("Yakındaki volkanlar (GVP statik katalog): ${context.volcanoesNearby.joinToString { it.name }}\n")
        } else {
            sb.append("GVP kataloğunda yakında volkan yok.\n")
        }
        sb.append("Aurora: Kp ${kp?.let { "%.1f".format(it) } ?: "veri yok"}")
        sb.append(" — tahmini görünürlük: ${when { kp == null -> "bilinmiyor"; kp >= 5 -> "yüksek"; kp >= 3 -> "orta"; else -> "düşük" }}\n")
        if (context.oceanTemp != null) sb.append("Yakındaki deniz yüzeyi sıcaklığı: ${context.oceanTemp}°C\n")
        sb.append("\nBu özet canlı veri kaynaklarından üretilmiştir. Güvenlik bilgisi için yerel resmî makamlara ve acil servislere başvurun.")
        return sb.toString()
    }

    override suspend fun askQuestion(question: String, dataContext: String, language: String): String =
        "AI is not configured in this build, so I can only answer from the data shown in the app. " +
            "The current structured data for this location is:\n\n$dataContext\n\n" +
            "For safety guidance, always check your local official authorities and emergency services."
}