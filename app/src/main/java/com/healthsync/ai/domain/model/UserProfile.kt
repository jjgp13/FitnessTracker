package com.healthsync.ai.domain.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val age: Int,
    val primarySports: List<String>,
    val strengthFocus: List<String>,
    val dietaryPreferences: List<String>,
    val macroSplit: Macros
)
