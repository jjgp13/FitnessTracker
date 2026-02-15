package com.healthsync.ai.domain.model

data class Workout(
    val type: WorkoutType,
    val warmUp: List<Exercise>,
    val mainBlock: List<Exercise>,
    val coolDown: List<Exercise>,
    val estimatedDurationMinutes: Int
)

enum class WorkoutType {
    STRENGTH,
    SOCCER_DRILLS,
    VOLLEYBALL,
    RECOVERY,
    REST
}

data class Exercise(
    val name: String,
    val sets: Int? = null,
    val reps: String? = null,
    val weight: String? = null,
    val notes: String? = null
)
