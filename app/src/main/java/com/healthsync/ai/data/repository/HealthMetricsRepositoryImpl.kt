package com.healthsync.ai.data.repository

import com.healthsync.ai.data.healthconnect.HealthConnectDataSource
import com.healthsync.ai.data.healthconnect.HealthConnectMapper
import com.healthsync.ai.data.local.db.dao.HealthMetricsDao
import com.healthsync.ai.data.local.mapper.toDomain
import com.healthsync.ai.data.local.mapper.toEntity
import com.healthsync.ai.data.remote.eightsleep.EightSleepDataSource
import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.repository.HealthMetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthMetricsRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val eightSleepDataSource: EightSleepDataSource,
    private val healthMetricsDao: HealthMetricsDao
) : HealthMetricsRepository {

    override suspend fun fetchTodayMetrics(): HealthMetrics {
        val today = LocalDate.now()

        // Read from Health Connect
        val sleepSessions = healthConnectDataSource.readSleepData(today)
        val sleepDuration = HealthConnectMapper.mapSleepDuration(sleepSessions)
        val deepSleep = HealthConnectMapper.mapDeepSleepMinutes(sleepSessions)
        val remSleep = HealthConnectMapper.mapRemSleepMinutes(sleepSessions)
        val restingHr = healthConnectDataSource.readHeartRate(today) ?: 0
        val steps = healthConnectDataSource.readSteps(today)
        val weight = healthConnectDataSource.readWeight()
        val bodyFat = healthConnectDataSource.readBodyFat()
        val bp = healthConnectDataSource.readBloodPressure()
        val hrvRecords = healthConnectDataSource.readHrvData(today)
        var hrvMs = HealthConnectMapper.mapHrvAverage(hrvRecords)

        // Read from Eight Sleep if authenticated
        var sleepScore: Int? = null
        if (eightSleepDataSource.isAuthenticated()) {
            val eightSleepResult = eightSleepDataSource.getSleepMetrics(
                startDate = today.toString(),
                endDate = today.toString()
            )
            eightSleepResult.getOrNull()?.sleeps?.firstOrNull()?.let { session ->
                sleepScore = session.score
                // Use Eight Sleep HRV if Health Connect HRV is unavailable
                if (hrvMs == 0.0) {
                    val eightSleepHrv = session.timeseries?.hrv
                        ?.mapNotNull { it.value }
                    if (!eightSleepHrv.isNullOrEmpty()) {
                        hrvMs = eightSleepHrv.average()
                    }
                }
            }
        }

        // Calculate rolling 7-day HRV average from Room
        val recentMetrics = healthMetricsDao.getRecent(7).map { it.toDomain() }
        val hrvValues = recentMetrics.map { it.hrvMs }.filter { it > 0 }
        val hrvRolling7DayAvg = if (hrvValues.isNotEmpty()) {
            (hrvValues + hrvMs).filter { it > 0 }.average()
        } else {
            hrvMs
        }

        val metrics = HealthMetrics(
            date = today,
            sleepDurationMinutes = sleepDuration,
            deepSleepMinutes = deepSleep,
            remSleepMinutes = remSleep,
            sleepScore = sleepScore,
            hrvMs = hrvMs,
            hrvRolling7DayAvg = hrvRolling7DayAvg,
            restingHeartRate = restingHr,
            bloodPressureSystolic = bp?.first,
            bloodPressureDiastolic = bp?.second,
            steps = steps,
            weight = weight,
            bodyFatPercentage = bodyFat
        )

        // Save to Room
        healthMetricsDao.insert(metrics.toEntity())

        return metrics
    }

    override suspend fun getMetricsForDate(date: LocalDate): HealthMetrics? {
        return healthMetricsDao.getByDate(date.toString())?.toDomain()
    }

    override fun getMetricsForDateRange(start: LocalDate, end: LocalDate): Flow<List<HealthMetrics>> {
        return healthMetricsDao.getForDateRange(start.toString(), end.toString())
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveMetrics(metrics: HealthMetrics) {
        healthMetricsDao.insert(metrics.toEntity())
    }
}
