package com.healthsync.ai.domain.model

data class NutritionPlan(
    val targetCalories: Int,
    val macros: Macros,
    val meals: List<Meal>,
    val snacks: List<Meal>
)

data class Macros(
    val carbsPercent: Int,
    val proteinPercent: Int,
    val fatPercent: Int
)

data class Meal(
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)
