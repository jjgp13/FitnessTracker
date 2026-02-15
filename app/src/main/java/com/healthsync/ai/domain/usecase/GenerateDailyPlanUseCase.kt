package com.healthsync.ai.domain.usecase

import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.repository.DailyPlanRepository
import java.time.LocalDate
import javax.inject.Inject

class GenerateDailyPlanUseCase @Inject constructor(
    private val dailyPlanRepository: DailyPlanRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<DailyPlan> {
        return try {
            Result.success(dailyPlanRepository.generatePlan(date))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
