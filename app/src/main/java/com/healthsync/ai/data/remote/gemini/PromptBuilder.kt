package com.healthsync.ai.data.remote.gemini

import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.UserProfile
import com.healthsync.ai.domain.model.WeekSchedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object PromptBuilder {

    fun buildSystemInstruction(profile: UserProfile): String = buildString {
        appendLine("You are a High-Performance Sports Scientist and Nutritionist AI assistant.")
        appendLine()
        appendLine("Your client profile:")
        appendLine("- ${profile.age}-year-old male")
        appendLine("- Primary sports: ${profile.primarySports.joinToString(", ")}")
        appendLine("- Strength focus: ${profile.strengthFocus.joinToString(", ")}")
        appendLine("- Dietary preferences: ${profile.dietaryPreferences.joinToString(", ")}")
        appendLine("- Default macro split: ${profile.macroSplit.carbsPercent}% Carbs, ${profile.macroSplit.proteinPercent}% Protein, ${profile.macroSplit.fatPercent}% Fat")
        appendLine()
        appendLine("Your responsibilities:")
        appendLine("1. ANALYZE: Evaluate Sleep Quality (Deep/REM), HRV, and Blood Pressure data")
        appendLine("2. TRAIN: Generate workouts focusing on explosive power and sport-specific training")
        appendLine("3. FUEL: Recommend 3 meals and 2 snacks matching the macro split")
        appendLine("4. FORMAT: Always return a valid JSON object matching the DailyPlan schema")
        appendLine()
        appendLine("Schedule awareness rules:")
        appendLine("- If today is a GAME DAY: Plan pre-game nutrition (carb-loading), light activation workout")
        appendLine("- If today is PRE-GAME (game tomorrow): Taper intensity, avoid heavy legs, focus mobility")
        appendLine("- If today is POST-GAME (game yesterday): Recovery session, anti-inflammatory meals")
        appendLine("- If HRV is >10% below 7-day rolling average: Pivot to Active Recovery")
        appendLine("- Respect busy calendar blocks: shorten workouts on packed days")
    }

    fun buildDailyPlanPrompt(
        metrics: HealthMetrics,
        recoveryStatus: RecoveryStatus,
        weekSchedule: WeekSchedule
    ): String {
        val today = metrics.date
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayContext = determineDayContext(today, weekSchedule)

        val hrvDelta = if (metrics.hrvRolling7DayAvg > 0) {
            ((metrics.hrvMs - metrics.hrvRolling7DayAvg) / metrics.hrvRolling7DayAvg * 100)
                .let { "%.1f".format(it) }
        } else {
            "N/A"
        }

        val sleepHours = metrics.sleepDurationMinutes / 60
        val sleepMins = metrics.sleepDurationMinutes % 60

        return buildString {
            appendLine("Today is $dayOfWeek, $dateStr.")
            appendLine()
            appendLine("## Today's Health Metrics")
            appendLine("- Sleep: ${sleepHours}h ${sleepMins}m total (Deep: ${metrics.deepSleepMinutes}m, REM: ${metrics.remSleepMinutes}m, Light: ${metrics.lightSleepMinutes}m, Awake: ${metrics.awakeMinutes}m)")
            metrics.sleepScore?.let { appendLine("- Sleep Score: $it/100") }
            appendLine("- HRV: ${metrics.hrvMs}ms (7-day avg: ${metrics.hrvRolling7DayAvg}ms)")
            appendLine("- Resting Heart Rate: ${metrics.restingHeartRate} bpm")
            if (metrics.bloodPressureSystolic != null && metrics.bloodPressureDiastolic != null) {
                appendLine("- Blood Pressure: ${metrics.bloodPressureSystolic}/${metrics.bloodPressureDiastolic} mmHg")
            }
            appendLine("- Steps: ${metrics.steps}")
            if (metrics.exerciseSessions.isNotEmpty()) {
                appendLine()
                appendLine("## Yesterday's Exercise Sessions")
                metrics.exerciseSessions.forEach { exercise ->
                    appendLine("- ${exercise.title}: ${exercise.durationMinutes} min${if (exercise.notes != null) " (${exercise.notes})" else ""}")
                }
            }
            metrics.weight?.let { appendLine("- Weight: $it lbs") }
            metrics.bodyFatPercentage?.let { appendLine("- Body Fat: $it%") }
            appendLine()
            appendLine("## Recovery Status: ${recoveryStatus.name}")
            appendLine("HRV today: ${metrics.hrvMs}ms | 7-day avg: ${metrics.hrvRolling7DayAvg}ms | Delta: $hrvDelta%")
            appendLine()
            appendLine("## This Week's Schedule")
            appendLine("- Game days this week: ${
                if (weekSchedule.gameDays.isEmpty()) "None"
                else weekSchedule.gameDays.joinToString(", ") {
                    "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${it.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                }
            }")
            appendLine("- Today is: $dayContext")
            appendLine()
            appendLine("## Generate Today's Plan")
            appendLine("Return ONLY a valid JSON object (no markdown fences) matching this schema:")
            appendLine(JSON_SCHEMA)
        }
    }

    fun determineDayContext(today: LocalDate, weekSchedule: WeekSchedule): String {
        val gameDays = weekSchedule.gameDays
        val isGameDay = gameDays.any { it == today }
        if (isGameDay) return "GAME_DAY"

        val isPreGame = gameDays.any { it == today.plusDays(1) }
        if (isPreGame) return "PRE_GAME"

        val isPostGame = gameDays.any { it == today.minusDays(1) }
        if (isPostGame) return "POST_GAME"

        return "TRAINING_DAY"
    }

    private val JSON_SCHEMA = """
{
  "date": "YYYY-MM-DD",
  "recoveryStatus": "FULL_SEND | MODERATE | ACTIVE_RECOVERY",
  "workout": {
    "type": "STRENGTH | SOCCER_DRILLS | VOLLEYBALL | RECOVERY | REST",
    "warmUp": [{"name": "...", "sets": 2, "reps": "10", "weight": null, "notes": null}],
    "mainBlock": [{"name": "...", "sets": 4, "reps": "6-8", "weight": "225lbs", "notes": "..."}],
    "coolDown": [{"name": "...", "sets": 1, "reps": "60 seconds", "weight": null, "notes": null}],
    "estimatedDurationMinutes": 55
  },
  "nutritionPlan": {
    "targetCalories": 2800,
    "macros": {"carbsPercent": 40, "proteinPercent": 30, "fatPercent": 30},
    "meals": [
      {"name": "Breakfast", "description": "...", "calories": 650, "protein": 45, "carbs": 60, "fat": 22}
    ],
    "snacks": [
      {"name": "Post-Workout", "description": "...", "calories": 350, "protein": 30, "carbs": 40, "fat": 8}
    ]
  },
  "coachNotes": "Your HRV is solid today. Full intensity training recommended..."
}
    """.trimIndent()
}
