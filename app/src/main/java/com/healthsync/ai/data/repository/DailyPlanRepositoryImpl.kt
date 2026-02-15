package com.healthsync.ai.data.repository

import com.healthsync.ai.data.local.db.dao.DailyPlanDao
import com.healthsync.ai.data.local.db.dao.HealthMetricsDao
import com.healthsync.ai.data.local.db.dao.UserProfileDao
import com.healthsync.ai.data.local.db.entity.DailyPlanEntity
import com.healthsync.ai.data.local.mapper.toDomain
import com.healthsync.ai.data.remote.gemini.GeminiClient
import com.healthsync.ai.data.remote.gemini.PromptBuilder
import com.healthsync.ai.data.remote.gemini.dto.DailyPlanResponse
import com.healthsync.ai.data.remote.gemini.dto.toDomain
import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.model.NutritionPlan
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.Workout
import com.healthsync.ai.domain.repository.CalendarRepository
import com.healthsync.ai.domain.repository.DailyPlanRepository
import com.healthsync.ai.domain.usecase.DetermineRecoveryStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyPlanRepositoryImpl @Inject constructor(
    private val geminiClient: GeminiClient,
    private val dailyPlanDao: DailyPlanDao,
    private val healthMetricsDao: HealthMetricsDao,
    private val userProfileDao: UserProfileDao,
    private val calendarRepository: CalendarRepository,
    private val determineRecoveryStatusUseCase: DetermineRecoveryStatusUseCase,
    private val json: Json
) : DailyPlanRepository {

    override suspend fun generatePlan(date: LocalDate): DailyPlan {
        // 1. Check cache
        val cached = dailyPlanDao.getByDate(date.toString())
        if (cached != null) return cached.toDomain(json)

        // 2. Get user profile
        val profileEntity = userProfileDao.getProfile().first()
            ?: throw IllegalStateException("User profile not found. Complete onboarding first.")
        val profile = profileEntity.toDomain()

        // 3. Get health metrics
        val metricsEntity = healthMetricsDao.getByDate(date.toString())
            ?: throw IllegalStateException("No health metrics found for $date")
        val metrics = metricsEntity.toDomain()

        // 4. Get week schedule
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekSchedule = calendarRepository.getWeekSchedule(weekStart)

        // 5. Determine recovery status
        val recoveryStatus = determineRecoveryStatusUseCase(metrics)

        // 6. Build prompt
        val systemInstruction = PromptBuilder.buildSystemInstruction(profile)
        val dailyPrompt = PromptBuilder.buildDailyPlanPrompt(metrics, recoveryStatus, weekSchedule)
        val fullPrompt = "$systemInstruction\n\n---\n\n$dailyPrompt"

        // 7. Call Gemini
        val responseText = geminiClient.generateDailyPlan(fullPrompt).getOrThrow()

        // 8. Parse JSON — strip markdown fences if present
        val cleanJson = responseText
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*$"), "")
            .trim()
        val dailyPlanResponse = json.decodeFromString<DailyPlanResponse>(cleanJson)

        // 9. Convert to domain
        val plan = dailyPlanResponse.toDomain()

        // 10. Save to cache
        savePlan(plan)

        // 11. Return
        return plan
    }

    override suspend fun getPlanForDate(date: LocalDate): DailyPlan? {
        return dailyPlanDao.getByDate(date.toString())?.toDomain(json)
    }

    override fun getPlansForDateRange(start: LocalDate, end: LocalDate): Flow<List<DailyPlan>> {
        return dailyPlanDao.getForDateRange(start.toString(), end.toString())
            .map { entities -> entities.map { it.toDomain(json) } }
    }

    override suspend fun savePlan(plan: DailyPlan) {
        dailyPlanDao.insert(plan.toEntity(json))
    }
}

private fun DailyPlanEntity.toDomain(json: Json): DailyPlan = DailyPlan(
    date = LocalDate.parse(date),
    recoveryStatus = try {
        RecoveryStatus.valueOf(recoveryStatus)
    } catch (_: Exception) {
        RecoveryStatus.MODERATE
    },
    workout = json.decodeFromString<WorkoutSerializable>(workoutJson).toDomain(),
    nutritionPlan = json.decodeFromString<NutritionPlanSerializable>(nutritionJson).toDomain(),
    coachNotes = coachNotes
)

private fun DailyPlan.toEntity(json: Json): DailyPlanEntity = DailyPlanEntity(
    date = date.toString(),
    recoveryStatus = recoveryStatus.name,
    workoutJson = json.encodeToString(WorkoutSerializable.serializer(), WorkoutSerializable.fromDomain(workout)),
    nutritionJson = json.encodeToString(NutritionPlanSerializable.serializer(), NutritionPlanSerializable.fromDomain(nutritionPlan)),
    coachNotes = coachNotes
)

// Serializable wrappers for Room JSON storage
@kotlinx.serialization.Serializable
private data class ExerciseSerializable(
    val name: String,
    val sets: Int? = null,
    val reps: String? = null,
    val weight: String? = null,
    val notes: String? = null
) {
    fun toDomain() = com.healthsync.ai.domain.model.Exercise(name, sets, reps, weight, notes)
    companion object {
        fun fromDomain(e: com.healthsync.ai.domain.model.Exercise) =
            ExerciseSerializable(e.name, e.sets, e.reps, e.weight, e.notes)
    }
}

@kotlinx.serialization.Serializable
private data class WorkoutSerializable(
    val type: String,
    val warmUp: List<ExerciseSerializable>,
    val mainBlock: List<ExerciseSerializable>,
    val coolDown: List<ExerciseSerializable>,
    val estimatedDurationMinutes: Int
) {
    fun toDomain() = Workout(
        type = try { com.healthsync.ai.domain.model.WorkoutType.valueOf(type) } catch (_: Exception) { com.healthsync.ai.domain.model.WorkoutType.STRENGTH },
        warmUp = warmUp.map { it.toDomain() },
        mainBlock = mainBlock.map { it.toDomain() },
        coolDown = coolDown.map { it.toDomain() },
        estimatedDurationMinutes = estimatedDurationMinutes
    )
    companion object {
        fun fromDomain(w: Workout) = WorkoutSerializable(
            type = w.type.name,
            warmUp = w.warmUp.map { ExerciseSerializable.fromDomain(it) },
            mainBlock = w.mainBlock.map { ExerciseSerializable.fromDomain(it) },
            coolDown = w.coolDown.map { ExerciseSerializable.fromDomain(it) },
            estimatedDurationMinutes = w.estimatedDurationMinutes
        )
    }
}

@kotlinx.serialization.Serializable
private data class MacrosSerializable(
    val carbsPercent: Int,
    val proteinPercent: Int,
    val fatPercent: Int
) {
    fun toDomain() = com.healthsync.ai.domain.model.Macros(carbsPercent, proteinPercent, fatPercent)
    companion object {
        fun fromDomain(m: com.healthsync.ai.domain.model.Macros) =
            MacrosSerializable(m.carbsPercent, m.proteinPercent, m.fatPercent)
    }
}

@kotlinx.serialization.Serializable
private data class MealSerializable(
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
) {
    fun toDomain() = com.healthsync.ai.domain.model.Meal(name, description, calories, protein, carbs, fat)
    companion object {
        fun fromDomain(m: com.healthsync.ai.domain.model.Meal) =
            MealSerializable(m.name, m.description, m.calories, m.protein, m.carbs, m.fat)
    }
}

@kotlinx.serialization.Serializable
private data class NutritionPlanSerializable(
    val targetCalories: Int,
    val macros: MacrosSerializable,
    val meals: List<MealSerializable>,
    val snacks: List<MealSerializable>
) {
    fun toDomain() = NutritionPlan(
        targetCalories = targetCalories,
        macros = macros.toDomain(),
        meals = meals.map { it.toDomain() },
        snacks = snacks.map { it.toDomain() }
    )
    companion object {
        fun fromDomain(n: NutritionPlan) = NutritionPlanSerializable(
            targetCalories = n.targetCalories,
            macros = MacrosSerializable.fromDomain(n.macros),
            meals = n.meals.map { MealSerializable.fromDomain(it) },
            snacks = n.snacks.map { MealSerializable.fromDomain(it) }
        )
    }
}
