package com.healthsync.ai.domain.model

import java.time.LocalDate

data class DailyPlan(
    val date: LocalDate,
    val recoveryStatus: RecoveryStatus,
    val workout: Workout,
    val nutritionPlan: NutritionPlan,
    val coachNotes: String
)

enum class RecoveryStatus {
    FULL_SEND,
    MODERATE,
    ACTIVE_RECOVERY
}
