package com.healthsync.ai.data.remote.gemini.dto

import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.model.Exercise
import com.healthsync.ai.domain.model.Macros
import com.healthsync.ai.domain.model.Meal
import com.healthsync.ai.domain.model.NutritionPlan
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.Workout
import com.healthsync.ai.domain.model.WorkoutType
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyPlanResponse(
    val date: String,
    val recoveryStatus: String,
    val workout: WorkoutResponse,
    val nutritionPlan: NutritionPlanResponse,
    val coachNotes: String
)

@Serializable
data class WorkoutResponse(
    val type: String,
    val warmUp: List<ExerciseResponse>,
    val mainBlock: List<ExerciseResponse>,
    val coolDown: List<ExerciseResponse>,
    val estimatedDurationMinutes: Int
)

@Serializable
data class ExerciseResponse(
    val name: String,
    val sets: Int? = null,
    val reps: String? = null,
    val weight: String? = null,
    val notes: String? = null
)

@Serializable
data class NutritionPlanResponse(
    val targetCalories: Int,
    val macros: MacrosResponse,
    val meals: List<MealResponse>,
    val snacks: List<MealResponse>
)

@Serializable
data class MacrosResponse(
    val carbsPercent: Int,
    val proteinPercent: Int,
    val fatPercent: Int
)

@Serializable
data class MealResponse(
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

fun DailyPlanResponse.toDomain(): DailyPlan = DailyPlan(
    date = LocalDate.parse(date),
    recoveryStatus = try {
        RecoveryStatus.valueOf(recoveryStatus)
    } catch (_: Exception) {
        RecoveryStatus.MODERATE
    },
    workout = workout.toDomain(),
    nutritionPlan = nutritionPlan.toDomain(),
    coachNotes = coachNotes
)

private fun WorkoutResponse.toDomain(): Workout = Workout(
    type = try {
        WorkoutType.valueOf(type)
    } catch (_: Exception) {
        WorkoutType.STRENGTH
    },
    warmUp = warmUp.map { it.toDomain() },
    mainBlock = mainBlock.map { it.toDomain() },
    coolDown = coolDown.map { it.toDomain() },
    estimatedDurationMinutes = estimatedDurationMinutes
)

private fun ExerciseResponse.toDomain(): Exercise = Exercise(
    name = name,
    sets = sets,
    reps = reps,
    weight = weight,
    notes = notes
)

private fun NutritionPlanResponse.toDomain(): NutritionPlan = NutritionPlan(
    targetCalories = targetCalories,
    macros = macros.toDomain(),
    meals = meals.map { it.toDomain() },
    snacks = snacks.map { it.toDomain() }
)

private fun MacrosResponse.toDomain(): Macros = Macros(
    carbsPercent = carbsPercent,
    proteinPercent = proteinPercent,
    fatPercent = fatPercent
)

private fun MealResponse.toDomain(): Meal = Meal(
    name = name,
    description = description,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat
)
