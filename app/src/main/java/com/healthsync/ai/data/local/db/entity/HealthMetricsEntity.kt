package com.healthsync.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_metrics")
data class HealthMetricsEntity(
    @PrimaryKey val date: String, // ISO LocalDate string
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
    val dataSources: String = "{}",
    val metricDates: String = "{}"
)
