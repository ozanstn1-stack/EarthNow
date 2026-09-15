package com.earthnow.app.ai

import com.earthnow.app.BuildConfig
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.domain.model.UserSettings
import com.earthnow.app.util.Units
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Routes AI requests to the configured provider. The template fallback is
 * always used as a last resort so the feature never silently fails.
 */
@Singleton
class AiManager @Inject constructor(
    private val openAiProvider: OpenAiProvider,
    private val deepSeekProvider: DeepSeekProvider,
    private val geminiProvider: GeminiProvider,
    private val templateProvider: TemplateSummaryProvider,
    private val settingsRepository: SettingsRepository,
    private val keyStore: AiKeyStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) {
    data class AiResult(val text: String, val provider: String, val usedFallback: Boolean)

    private fun appLanguage(): String =
        runCatching { appContext.resources.configuration.locales[0].language }.getOrDefault("en")

    private fun providerUnavailable(e: Exception, fallback: String): String =
        if (appLanguage() == "tr") {
            "AI sağlayıcısına ulaşılamadı (${e.message ?: "hata"}). Bunun yerine veri şablonu gösteriliyor:\n\n$fallback"
        } else {
            "AI provider unavailable (${e.message ?: "error"}). Showing data template instead:\n\n$fallback"
        }

    suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit
    ): AiResult {
        val s = settingsRepository.settings.first()
        val lang = appLanguage()
        if (!s.aiEnabled) {
            return AiResult(templateProvider.summarize(context, tempUnit, windUnit, lang), templateProvider.displayName, true)
        }
        val chosen = pickProvider(s.aiProvider)
        return try {
            when (chosen) {
                "template" -> AiResult(templateProvider.summarize(context, tempUnit, windUnit, lang), templateProvider.displayName, true)
                "gemini" -> AiResult(geminiProvider.summarize(context, tempUnit, windUnit, lang), geminiProvider.displayName, false)
                "deepseek" -> AiResult(deepSeekProvider.summarize(context, tempUnit, windUnit, lang), deepSeekProvider.displayName, false)
                else -> AiResult(openAiProvider.summarize(context, tempUnit, windUnit, lang), openAiProvider.displayName, false)
            }
        } catch (e: Exception) {
            val fallback = templateProvider.summarize(context, tempUnit, windUnit, lang)
            AiResult(providerUnavailable(e, fallback), templateProvider.displayName, true)
        }
    }

    suspend fun askQuestion(question: String, dataContext: String): AiResult {
        val s = settingsRepository.settings.first()
        val lang = appLanguage()
        if (!s.aiEnabled) {
            return AiResult(templateProvider.askQuestion(question, dataContext, lang), templateProvider.displayName, true)
        }
        val chosen = pickProvider(s.aiProvider)
        return try {
            when (chosen) {
                "template" -> AiResult(templateProvider.askQuestion(question, dataContext, lang), templateProvider.displayName, true)
                "gemini" -> AiResult(geminiProvider.askQuestion(question, dataContext, lang), geminiProvider.displayName, false)
                "deepseek" -> AiResult(deepSeekProvider.askQuestion(question, dataContext, lang), deepSeekProvider.displayName, false)
                else -> AiResult(openAiProvider.askQuestion(question, dataContext, lang), openAiProvider.displayName, false)
            }
        } catch (e: Exception) {
            AiResult(
                providerUnavailable(e, templateProvider.askQuestion(question, dataContext, lang)),
                templateProvider.displayName,
                true
            )
        }
    }

    suspend fun availableProviders(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (keyStore.effectiveKey("deepseek").isNotBlank()) {
            list += deepSeekProvider.id to deepSeekProvider.displayName
        }
        if (keyStore.effectiveKey("openai").isNotBlank()) {
            list += openAiProvider.id to openAiProvider.displayName
        }
        if (keyStore.effectiveKey("gemini").isNotBlank()) {
            list += geminiProvider.id to geminiProvider.displayName
        }
        list += templateProvider.id to templateProvider.displayName
        return list
    }

    private suspend fun pickProvider(pref: String): String = when (pref) {
        "openai" -> if (keyStore.effectiveKey("openai").isNotBlank()) "openai" else "template"
        "gemini" -> if (keyStore.effectiveKey("gemini").isNotBlank()) "gemini" else "template"
        "deepseek" -> if (keyStore.effectiveKey("deepseek").isNotBlank()) "deepseek" else "template"
        "template" -> "template"
        else -> {
            // Auto: prefer the cheapest configured provider, fall back to
            // the local template so the feature never fails silently.
            when {
                keyStore.effectiveKey("deepseek").isNotBlank() -> "deepseek"
                keyStore.effectiveKey("openai").isNotBlank() -> "openai"
                keyStore.effectiveKey("gemini").isNotBlank() -> "gemini"
                else -> "template"
            }
        }
    }
}


