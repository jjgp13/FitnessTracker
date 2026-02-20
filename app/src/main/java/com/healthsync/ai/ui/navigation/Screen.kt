package com.healthsync.ai.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Onboarding : Screen("onboarding")
    data object MorningBriefing : Screen("morning_briefing")
    data object WorkoutDetail : Screen("workout_detail")
    data object Nutrition : Screen("nutrition")
    data object Schedule : Screen("schedule")
    data object DailyPlan : Screen("daily_plan")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
