package com.healthsync.ai.domain.repository

import com.healthsync.ai.domain.model.DailyPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DailyPlanRepository {
    suspend fun generatePlan(date: LocalDate): DailyPlan
    suspend fun getPlanForDate(date: LocalDate): DailyPlan?
    fun getPlansForDateRange(start: LocalDate, end: LocalDate): Flow<List<DailyPlan>>
    suspend fun savePlan(plan: DailyPlan)
}
