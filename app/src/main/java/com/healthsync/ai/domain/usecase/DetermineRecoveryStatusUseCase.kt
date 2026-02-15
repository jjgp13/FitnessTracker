package com.healthsync.ai.domain.usecase

import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.model.RecoveryStatus
import javax.inject.Inject

class DetermineRecoveryStatusUseCase @Inject constructor() {

    operator fun invoke(metrics: HealthMetrics): RecoveryStatus {
        val hrv = metrics.hrvMs
        val hrv7DayAvg = metrics.hrvRolling7DayAvg

        if (hrv7DayAvg <= 0) return RecoveryStatus.MODERATE

        return when {
            hrv < hrv7DayAvg * 0.90 -> RecoveryStatus.ACTIVE_RECOVERY
            hrv < hrv7DayAvg * 0.95 || (metrics.sleepScore != null && metrics.sleepScore < 70) ->
                RecoveryStatus.MODERATE
            else -> RecoveryStatus.FULL_SEND
        }
    }
}
