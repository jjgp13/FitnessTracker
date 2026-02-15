package com.healthsync.ai.data.remote.gemini

import com.google.firebase.ai.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    suspend fun generateDailyPlan(prompt: String): Result<String> {
        return try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini returned empty response"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
