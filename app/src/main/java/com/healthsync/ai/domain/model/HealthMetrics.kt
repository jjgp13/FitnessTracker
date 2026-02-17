package com.healthsync.ai.domain.model

import java.time.LocalDate

data class HealthMetrics(
    val date: LocalDate,
    val sleepDurationMinutes: Int,
    val deepSleepMinutes: Int,
    val remSleepMinutes: Int,
    val lightSleepMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val sleepScore: Int?,
    val hrvMs: Double,
    val hrvRolling7DayAvg: Double,
    val restingHeartRate: Int,
    val bloodPressureSystolic: Int?,
    val bloodPressureDiastolic: Int?,
    val steps: Int,
    val weight: Double?,
    val bodyFatPercentage: Double?,
    val dataSources: Map<String, String> = emptyMap(),
    val metricDates: Map<String, String> = emptyMap(),
    val exerciseSessions: List<ExerciseSummaryDomain> = emptyList()
)

data class ExerciseSummaryDomain(
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val startTime: String,
    val notes: String? = null
)
