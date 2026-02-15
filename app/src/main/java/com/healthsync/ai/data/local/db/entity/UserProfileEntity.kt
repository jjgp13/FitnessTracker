package com.healthsync.ai.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val age: Int,
    val primarySportsJson: String,
    val strengthFocusJson: String,
    val dietaryPreferencesJson: String,
    val carbsPercent: Int,
    val proteinPercent: Int,
    val fatPercent: Int
)
