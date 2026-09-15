package com.earthnow.app.ai

import com.earthnow.app.domain.model.LocationContext

/**
 * Provider-independent AI abstraction. Implementations must only use the
 * structured, real data contained in [LocationContext] and must never
 * invent facts. Safety rules for hazard questions are enforced in the
 * shared system prompt.
 */
interface AiSummaryProvider {
    val id: String
    val displayName: String
    suspend fun summarize(
        context: LocationContext,
        tempUnit: com.earthnow.app.util.Units.TempUnit,
        windUnit: com.earthnow.app.util.Units.WindUnit,
        language: String = "en"
    ): String
    suspend fun askQuestion(
        question: String,
        dataContext: String,
        language: String = "en"
    ): String
}

data class AiQuestion(
    val question: String,
    val data: String
)