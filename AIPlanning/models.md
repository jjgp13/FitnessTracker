# HealthSync AI - Domain Models Reference

## HealthMetrics
```kotlin
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
```

## Data Source Priority (Primary → Fallback)
| Metric | Primary Source | Fallback Source | Lookback | Notes |
|--------|---------------|-----------------|----------|-------|
| Sleep (duration, deep, REM, score) | Eight Sleep | Fitbit (via Health Connect) | Today → Yesterday | Eight Sleep provides sleep score |
| Resting HR | Eight Sleep (min HR from timeseries) | Fitbit (via Health Connect) | Today → Last 3 days | Lowest 10% average from Fitbit |
| HRV | Eight Sleep (avg from timeseries) | Fitbit (via Health Connect) | Today → Last 3 days | HRV RMSSD |
| Steps | Fitbit (via Health Connect) | — | Yesterday only | **Prior day activity** for Gemini context |
| Weight | Withings (via Health Connect) | — | Last 90 days, most recent | Shows date of reading |
| Body Fat | Withings (via Health Connect) | — | Last 90 days, most recent | Shows date of reading |
| Blood Pressure | Withings (via Health Connect) | — | Last 90 days, most recent | Shows date of reading |

The `dataSources` map tracks which source provided each metric (keys: `sleep`, `restingHr`, `hrv`, `steps`, `weight`, `bodyFat`, `bloodPressure`).
The `metricDates` map tracks the ISO date when each metric was recorded. Displayed in the UI as "Source · Today/Yesterday/MMM d".

## DailyPlan + Workout + NutritionPlan
```kotlin
data class DailyPlan(
    val date: LocalDate,
    val recoveryStatus: RecoveryStatus,  // FULL_SEND, MODERATE, ACTIVE_RECOVERY
    val workout: Workout,
    val nutritionPlan: NutritionPlan,
    val coachNotes: String
)

enum class RecoveryStatus { FULL_SEND, MODERATE, ACTIVE_RECOVERY }

data class Workout(
    val type: WorkoutType,  // STRENGTH, SOCCER_DRILLS, VOLLEYBALL, RECOVERY, REST
    val warmUp: List<Exercise>,
    val mainBlock: List<Exercise>,
    val coolDown: List<Exercise>,
    val estimatedDurationMinutes: Int
)

data class Exercise(
    val name: String,
    val sets: Int? = null,
    val reps: String? = null,    // "8-10" or "30 seconds"
    val weight: String? = null,
    val notes: String? = null
)

data class NutritionPlan(
    val targetCalories: Int,
    val macros: Macros,          // 40C/30P/30F
    val meals: List<Meal>,
    val snacks: List<Meal>
)

data class Macros(carbsPercent: Int, proteinPercent: Int, fatPercent: Int)

data class Meal(
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)
```

## UserProfile
```kotlin
data class UserProfile(
    val id: String,
    val displayName: String,
    val age: Int,
    val primarySports: List<String>,
    val strengthFocus: List<String>,
    val dietaryPreferences: List<String>,
    val macroSplit: Macros
)
```

## Calendar Models
```kotlin
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val sportType: SportType?,  // AUTO-DETECTED or MANUALLY TAGGED
    val isAllDay: Boolean,
    val calendarName: String
)

enum class SportType { SOCCER, VOLLEYBALL, OTHER_SPORT }

data class WeekSchedule(
    val weekStartDate: LocalDate,
    val events: List<CalendarEvent>,
    val gameDays: List<LocalDate>,
    val availableTrainingSlots: List<LocalDate>
)

data class SportEventConfig(
    val autoDetectKeywords: Map<SportType, List<String>>,
    val manualOverrides: Map<Long, SportType?>
)
```

## Default Sport Keywords
```kotlin
SOCCER:     ["soccer", "futbol", "fútbol", "football"]
VOLLEYBALL: ["volleyball", "volley", "vball"]
OTHER:      ["game", "match", "practice", "training", "workout"]
```

## Recovery Status Logic
```
IF hrv < (hrv7DayAvg * 0.90)                       → ACTIVE_RECOVERY
ELSE IF hrv < (hrv7DayAvg * 0.95) OR sleepScore < 70 → MODERATE
ELSE                                                 → FULL_SEND
```

## User Profile Defaults (from FitnessTrackerPlan.md)
- Demographics: 32-year-old Male
- Sports: Soccer (Midfielder/Forward), Competitive Co-ed Volleyball
- Strength: Trap bar deadlifts, Nordic curls, Air squats
- Diet: High-protein Mexican and American cuisine
- Macros: 40% Carb, 30% Protein, 30% Fat
