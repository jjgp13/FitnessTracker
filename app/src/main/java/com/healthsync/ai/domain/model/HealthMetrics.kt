package com.healthsync.ai.domain.model

import java.time.LocalDate

data class HealthMetrics(
    val date: LocalDate,
    val sleepDurationMinutes: Int,
    val deepSleepMinutes: Int,
    val remSleepMinutes: Int,
    val sleepScore: Int?,
    val hrvMs: Double,
    val hrvRolling7DayAvg: Double,
    val restingHeartRate: Int,
    val bloodPressureSystolic: Int?,
    val bloodPressureDiastolic: Int?,
    val steps: Int,
    val weight: Double?,
    val bodyFatPercentage: Double?,
    val dataSources: Map<String, String> = emptyMap()
)
