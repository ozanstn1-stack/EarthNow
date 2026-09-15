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
    private val geminiProvider: GeminiProvider,
    private val templateProvider: TemplateSummaryProvider,
    private val settingsRepository: SettingsRepository,
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

    fun availableProviders(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (BuildConfig.AI_OPENAI_API_KEY.isNotBlank() || BuildConfig.AI_BASE_URL.isNotBlank()) {
            list += openAiProvider.id to openAiProvider.displayName
        }
        if (BuildConfig.AI_GEMINI_API_KEY.isNotBlank()) {
            list += geminiProvider.id to geminiProvider.displayName
        }
        list += templateProvider.id to templateProvider.displayName
        return list
    }

    private suspend fun pickProvider(pref: String): String = when (pref) {
        "openai" -> "openai"
        "gemini" -> "gemini"
        "template" -> "template"
        else -> {
            when {
                BuildConfig.AI_GEMINI_API_KEY.isNotBlank() && BuildConfig.AI_OPENAI_API_KEY.isBlank() -> "gemini"
                BuildConfig.AI_OPENAI_API_KEY.isNotBlank() -> "openai"
                else -> "template"
            }
        }
    }
}


