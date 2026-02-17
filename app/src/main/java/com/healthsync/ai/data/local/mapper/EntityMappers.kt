package com.healthsync.ai.data.local.mapper

import com.healthsync.ai.data.local.db.entity.DailyPlanEntity
import com.healthsync.ai.data.local.db.entity.HealthMetricsEntity
import com.healthsync.ai.data.local.db.entity.UserProfileEntity
import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.model.ExerciseSummaryDomain
import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.model.Macros
import com.healthsync.ai.domain.model.NutritionPlan
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.UserProfile
import com.healthsync.ai.domain.model.Workout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

@kotlinx.serialization.Serializable
private data class ExerciseSummarySerializable(
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val startTime: String,
    val notes: String? = null
) {
    fun toDomain() = ExerciseSummaryDomain(type, title, durationMinutes, startTime, notes)
    companion object {
        fun fromDomain(e: ExerciseSummaryDomain) =
            ExerciseSummarySerializable(e.type, e.title, e.durationMinutes, e.startTime, e.notes)
    }
}

fun HealthMetricsEntity.toDomain(): HealthMetrics = HealthMetrics(
    date = LocalDate.parse(date),
    sleepDurationMinutes = sleepDurationMinutes,
    deepSleepMinutes = deepSleepMinutes,
    remSleepMinutes = remSleepMinutes,
    lightSleepMinutes = lightSleepMinutes,
    awakeMinutes = awakeMinutes,
    sleepScore = sleepScore,
    hrvMs = hrvMs,
    hrvRolling7DayAvg = hrvRolling7DayAvg,
    restingHeartRate = restingHeartRate,
    bloodPressureSystolic = bloodPressureSystolic,
    bloodPressureDiastolic = bloodPressureDiastolic,
    steps = steps,
    weight = weight,
    bodyFatPercentage = bodyFatPercentage,
    dataSources = json.decodeFromString<Map<String, String>>(dataSources),
    metricDates = json.decodeFromString<Map<String, String>>(metricDates),
    exerciseSessions = try {
        json.decodeFromString<List<ExerciseSummarySerializable>>(exerciseSessionsJson).map { it.toDomain() }
    } catch (_: Exception) { emptyList() }
)

fun HealthMetrics.toEntity(): HealthMetricsEntity = HealthMetricsEntity(
    date = date.toString(),
    sleepDurationMinutes = sleepDurationMinutes,
    deepSleepMinutes = deepSleepMinutes,
    remSleepMinutes = remSleepMinutes,
    lightSleepMinutes = lightSleepMinutes,
    awakeMinutes = awakeMinutes,
    sleepScore = sleepScore,
    hrvMs = hrvMs,
    hrvRolling7DayAvg = hrvRolling7DayAvg,
    restingHeartRate = restingHeartRate,
    bloodPressureSystolic = bloodPressureSystolic,
    bloodPressureDiastolic = bloodPressureDiastolic,
    steps = steps,
    weight = weight,
    bodyFatPercentage = bodyFatPercentage,
    dataSources = json.encodeToString(dataSources),
    metricDates = json.encodeToString(metricDates),
    exerciseSessionsJson = json.encodeToString(exerciseSessions.map { ExerciseSummarySerializable.fromDomain(it) })
)

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    age = age,
    primarySports = json.decodeFromString<List<String>>(primarySportsJson),
    strengthFocus = json.decodeFromString<List<String>>(strengthFocusJson),
    dietaryPreferences = json.decodeFromString<List<String>>(dietaryPreferencesJson),
    macroSplit = Macros(carbsPercent, proteinPercent, fatPercent)
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    displayName = displayName,
    age = age,
    primarySportsJson = json.encodeToString(stringListSerializer, primarySports),
    strengthFocusJson = json.encodeToString(stringListSerializer, strengthFocus),
    dietaryPreferencesJson = json.encodeToString(stringListSerializer, dietaryPreferences),
    carbsPercent = macroSplit.carbsPercent,
    proteinPercent = macroSplit.proteinPercent,
    fatPercent = macroSplit.fatPercent
)
