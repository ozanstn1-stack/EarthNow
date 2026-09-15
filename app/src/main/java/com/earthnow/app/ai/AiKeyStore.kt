package com.earthnow.app.ai

import com.earthnow.app.BuildConfig
import com.earthnow.app.data.prefs.SettingsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * Resolves the effective AI credentials: keys entered at runtime in Settings
 * (stored encrypted) take precedence over keys compiled from
 * local.properties at build time.
 */
@Singleton
class AiKeyStore @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val cache = ConcurrentHashMap<String, String>()
    private var cachedBaseUrl: String? = null

    /** Suspending accessor for ViewModels and providers. */
    suspend fun runtimeKey(provider: String): String {
        val value = settingsRepository.runtimeAiKey(provider)
        cache[provider] = value
        return value
    }

    suspend fun effectiveKey(provider: String): String {
        val runtime = runtimeKey(provider)
        if (runtime.isNotBlank()) return runtime
        return when (provider) {
            "deepseek" -> BuildConfig.AI_DEEPSEEK_API_KEY
            "openai" -> BuildConfig.AI_OPENAI_API_KEY
            "gemini" -> BuildConfig.AI_GEMINI_API_KEY
            else -> ""
        }
    }

    suspend fun setKey(provider: String, value: String) {
        settingsRepository.setRuntimeAiKey(provider, value.trim())
        cache[provider] = value.trim()
    }

    /** Blocking accessor safe on OkHttp background threads. */
    fun blockingEffectiveKey(provider: String): String {
        cache[provider]?.let { if (it.isNotBlank()) return it }
        return runBlocking {
            effectiveKey(provider).also { cache[provider] = it }
        }
    }

    suspend fun runtimeBaseUrl(): String {
        val value = settingsRepository.runtimeOpenAiBaseUrl()
        cachedBaseUrl = value
        return value
    }

    suspend fun setBaseUrl(url: String) {
        settingsRepository.setRuntimeOpenAiBaseUrl(url)
        cachedBaseUrl = url.trim()
    }

    /** Blocking accessor safe on OkHttp background threads. */
    fun blockingRuntimeBaseUrl(): String {
        cachedBaseUrl?.let { return it }
        return runBlocking { runtimeBaseUrl().also { cachedBaseUrl = it } }
    }

    /** True when any provider has a usable key (runtime or build-time). */
    suspend fun anyProviderConfigured(): Boolean =
        effectiveKey("deepseek").isNotBlank() ||
            effectiveKey("openai").isNotBlank() ||
            effectiveKey("gemini").isNotBlank()
}