package com.healthsync.ai.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Duration

object HealthConnectMapper {

    fun mapSleepDuration(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        // Take the longest session to avoid double-counting overlapping records
        return sessions.maxOf { session ->
            Duration.between(session.startTime, session.endTime).toMinutes()
        }.toInt()
    }

    fun mapDeepSleepMinutes(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        val longest = sessions.maxByOrNull { Duration.between(it.startTime, it.endTime) } ?: return 0
        return longest.stages
            .filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            .toInt()
    }

    fun mapRemSleepMinutes(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        val longest = sessions.maxByOrNull { Duration.between(it.startTime, it.endTime) } ?: return 0
        return longest.stages
            .filter { it.stage == SleepSessionRecord.STAGE_TYPE_REM }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            .toInt()
    }

    fun mapLightSleepMinutes(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        val longest = sessions.maxByOrNull { Duration.between(it.startTime, it.endTime) } ?: return 0
        return longest.stages
            .filter { it.stage == SleepSessionRecord.STAGE_TYPE_LIGHT }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            .toInt()
    }

    fun mapAwakeMinutes(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        val longest = sessions.maxByOrNull { Duration.between(it.startTime, it.endTime) } ?: return 0
        return longest.stages
            .filter { it.stage == SleepSessionRecord.STAGE_TYPE_AWAKE }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            .toInt()
    }

    fun mapHrvAverage(records: List<HeartRateVariabilityRmssdRecord>): Double {
        if (records.isEmpty()) return 0.0
        return records.map { it.heartRateVariabilityMillis }.average()
    }

    fun mapExerciseSessions(sessions: List<ExerciseSessionRecord>): List<ExerciseSummary> {
        return sessions.map { session ->
            ExerciseSummary(
                type = mapExerciseType(session.exerciseType),
                title = session.title ?: mapExerciseType(session.exerciseType),
                durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt(),
                startTime = session.startTime.toString(),
                notes = session.notes
            )
        }
    }

    private fun mapExerciseType(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Soccer"
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "Volleyball"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "Stretching"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            else -> "Exercise"
        }
    }
}

data class ExerciseSummary(
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val startTime: String,
    val notes: String? = null
)
