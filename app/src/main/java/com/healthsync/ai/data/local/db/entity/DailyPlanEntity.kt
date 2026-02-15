package com.healthsync.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_plans")
data class DailyPlanEntity(
    @PrimaryKey val date: String,
    val recoveryStatus: String,
    val workoutJson: String,    // Serialized Workout
    val nutritionJson: String,  // Serialized NutritionPlan
    val coachNotes: String
)
