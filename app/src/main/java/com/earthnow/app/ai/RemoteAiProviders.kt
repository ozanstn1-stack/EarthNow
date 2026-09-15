package com.earthnow.app.ai

import com.earthnow.app.data.api.GeminiApi
import com.earthnow.app.data.api.OpenAiApi
import com.earthnow.app.data.remote.dto.ChatCompletionRequest
import com.earthnow.app.data.remote.dto.ChatMessageDto
import com.earthnow.app.data.remote.dto.GeminiContentDto
import com.earthnow.app.data.remote.dto.GeminiPartDto
import com.earthnow.app.data.remote.dto.GeminiRequest
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.util.Units
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiProvider @Inject constructor(
    @javax.inject.Named("openai") private val api: OpenAiApi
) : AiSummaryProvider {
    override val id = "openai"
    override val displayName = "OpenAI-compatible API"

    private val model = System.getenv("AI_OPENAI_MODEL") ?: "gpt-4o-mini"

    override suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String
    ): String {
        val payload = AiPromptBuilder.summaryPayload(context, tempUnit, windUnit, language)
        return chat(AiPromptBuilder.summarySystemPrompt(language), payload)
    }

    override suspend fun askQuestion(question: String, dataContext: String, language: String): String =
        chat(AiPromptBuilder.questionSystemPrompt(language), AiPromptBuilder.questionPayload(question, dataContext))

    private suspend fun chat(system: String, user: String): String {
        val resp = api.chat(
            ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto("system", system),
                    ChatMessageDto("user", user)
                )
            )
        )
        resp.error?.message?.let { throw IllegalStateException("AI API error: $it") }
        return resp.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty AI response")
    }
}

@Singleton
class DeepSeekProvider @Inject constructor(
    @javax.inject.Named("deepseek") private val api: OpenAiApi
) : AiSummaryProvider {
    override val id = "deepseek"
    override val displayName = "DeepSeek"

    private val model = System.getenv("AI_DEEPSEEK_MODEL") ?: "deepseek-chat"

    override suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String
    ): String {
        val payload = AiPromptBuilder.summaryPayload(context, tempUnit, windUnit, language)
        return chat(AiPromptBuilder.summarySystemPrompt(language), payload)
    }

    override suspend fun askQuestion(question: String, dataContext: String, language: String): String =
        chat(AiPromptBuilder.questionSystemPrompt(language), AiPromptBuilder.questionPayload(question, dataContext))

    private suspend fun chat(system: String, user: String): String {
        if (com.earthnow.app.BuildConfig.AI_DEEPSEEK_API_KEY.isBlank()) {
            throw IllegalStateException("DeepSeek API key not configured")
        }
        val resp = api.chat(
            ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto("system", system),
                    ChatMessageDto("user", user)
                )
            )
        )
        resp.error?.message?.let { throw IllegalStateException("DeepSeek API error: $it") }
        return resp.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty DeepSeek response")
    }
}

@Singleton
class GeminiProvider @Inject constructor(
    private val api: GeminiApi
) : AiSummaryProvider {
    override val id = "gemini"
    override val displayName = "Google Gemini"

    private val model = System.getenv("AI_GEMINI_MODEL") ?: "gemini-2.0-flash"
    private val apiKey: String get() = com.earthnow.app.BuildConfig.AI_GEMINI_API_KEY

    override suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String
    ): String {
        val payload = AiPromptBuilder.summaryPayload(context, tempUnit, windUnit, language)
        return generate(AiPromptBuilder.summarySystemPrompt(language), payload)
    }

    override suspend fun askQuestion(question: String, dataContext: String, language: String): String =
        generate(AiPromptBuilder.questionSystemPrompt(language), AiPromptBuilder.questionPayload(question, dataContext))

    private suspend fun generate(system: String, user: String): String {
        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key not configured")
        val resp = api.generate(
            model = model,
            key = apiKey,
            request = GeminiRequest(
                contents = listOf(
                    GeminiContentDto(listOf(GeminiPartDto(system))),
                    GeminiContentDto(listOf(GeminiPartDto(user)))
                )
            )
        )
        resp.error?.message?.let { throw IllegalStateException("Gemini error: $it") }
        return resp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty Gemini response")
    }
}