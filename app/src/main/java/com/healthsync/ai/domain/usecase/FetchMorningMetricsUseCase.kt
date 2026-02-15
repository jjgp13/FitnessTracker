package com.healthsync.ai.domain.usecase

import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.repository.HealthMetricsRepository
import javax.inject.Inject

class FetchMorningMetricsUseCase @Inject constructor(
    private val healthMetricsRepository: HealthMetricsRepository
) {
    suspend operator fun invoke(): Result<HealthMetrics> {
        return try {
            Result.success(healthMetricsRepository.fetchTodayMetrics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
