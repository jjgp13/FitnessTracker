package com.healthsync.ai.data.healthconnect

import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Duration

object HealthConnectMapper {

    fun mapSleepDuration(sessions: List<SleepSessionRecord>): Int {
        if (sessions.isEmpty()) return 0
        return sessions.sumOf { session ->
            Duration.between(session.startTime, session.endTime).toMinutes()
        }.toInt()
    }

    fun mapDeepSleepMinutes(sessions: List<SleepSessionRecord>): Int {
        return sessions.sumOf { session ->
            session.stages
                .filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
                .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        }.toInt()
    }

    fun mapRemSleepMinutes(sessions: List<SleepSessionRecord>): Int {
        return sessions.sumOf { session ->
            session.stages
                .filter { it.stage == SleepSessionRecord.STAGE_TYPE_REM }
                .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        }.toInt()
    }

    fun mapHrvAverage(records: List<HeartRateVariabilityRmssdRecord>): Double {
        if (records.isEmpty()) return 0.0
        return records.map { it.heartRateVariabilityMillis }.average()
    }
}
