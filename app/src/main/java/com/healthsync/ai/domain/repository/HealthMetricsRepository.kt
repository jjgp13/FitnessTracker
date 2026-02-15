package com.healthsync.ai.domain.repository

import com.healthsync.ai.domain.model.HealthMetrics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HealthMetricsRepository {
    suspend fun fetchTodayMetrics(): HealthMetrics
    suspend fun getMetricsForDate(date: LocalDate): HealthMetrics?
    fun getMetricsForDateRange(start: LocalDate, end: LocalDate): Flow<List<HealthMetrics>>
    suspend fun saveMetrics(metrics: HealthMetrics)
}
