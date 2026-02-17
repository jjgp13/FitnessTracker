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
        val dataSources = mutableMapOf<String, String>()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // === SLEEP: Eight Sleep first, fallback to Health Connect (Fitbit) ===
        var sleepDuration = 0; var deepSleep = 0; var remSleep = 0; var sleepScore: Int? = null
        var hrvMs = 0.0; var restingHr = 0

        if (eightSleepDataSource.isAuthenticated()) {
            val result = eightSleepDataSource.getSleepMetrics(today.toString(), today.toString())
            result.getOrNull()?.sleeps?.firstOrNull()?.let { session ->
                // Calculate sleep duration from startTime/endTime
                if (session.startTime != null && session.endTime != null) {
                    try {
                        val start = java.time.Instant.parse(session.startTime)
                        val end = java.time.Instant.parse(session.endTime)
                        sleepDuration = ((end.epochSecond - start.epochSecond) / 60).toInt()
                    } catch (_: Exception) {}
                }
                // Deep and REM from stages
                session.stages.forEach { stage ->
                    when (stage.stage?.lowercase()) {
                        "deep" -> deepSleep += ((stage.duration ?: 0) / 60).toInt()
                        "rem" -> remSleep += ((stage.duration ?: 0) / 60).toInt()
                    }
                }
                sleepScore = session.score
                // HRV from timeseries
                val hrvValues = session.timeseries?.hrv?.mapNotNull { it.value }
                if (!hrvValues.isNullOrEmpty()) {
                    hrvMs = hrvValues.average()
                    dataSources["hrv"] = "Eight Sleep"
                }
                // Resting HR from timeseries (use minimum as resting)
                val hrValues = session.timeseries?.heartRate?.mapNotNull { it.value }
                if (!hrValues.isNullOrEmpty()) {
                    restingHr = hrValues.min().toInt()
                    dataSources["restingHr"] = "Eight Sleep"
                }
                if (sleepDuration > 0) dataSources["sleep"] = "Eight Sleep"
            }
        }

        // Fallback to Health Connect (Fitbit) for sleep if Eight Sleep had no data
        if (sleepDuration == 0) {
            val sleepSessions = healthConnectDataSource.readSleepData(today)
            sleepDuration = HealthConnectMapper.mapSleepDuration(sleepSessions)
            deepSleep = HealthConnectMapper.mapDeepSleepMinutes(sleepSessions)
            remSleep = HealthConnectMapper.mapRemSleepMinutes(sleepSessions)
            if (sleepDuration > 0) dataSources["sleep"] = "Fitbit"
        }

        // Fallback HR from Health Connect (Fitbit)
        if (restingHr == 0) {
            restingHr = healthConnectDataSource.readHeartRate(today) ?: 0
            if (restingHr > 0) dataSources["restingHr"] = "Fitbit"
        }

        // Fallback HRV from Health Connect (Fitbit)
        if (hrvMs == 0.0) {
            val hrvRecords = healthConnectDataSource.readHrvData(today)
            hrvMs = HealthConnectMapper.mapHrvAverage(hrvRecords)
            if (hrvMs > 0) dataSources["hrv"] = "Fitbit"
        }

        // STEPS: Yesterday's data from Health Connect (Fitbit)
        val steps = healthConnectDataSource.readSteps(yesterday)
        if (steps > 0) dataSources["steps"] = "Fitbit"

        // Weight & Body Fat: from Health Connect (Withings)
        val weight = healthConnectDataSource.readWeight()
        if (weight != null) dataSources["weight"] = "Withings"
        val bodyFat = healthConnectDataSource.readBodyFat()
        if (bodyFat != null) dataSources["bodyFat"] = "Withings"

        // Blood Pressure: from Health Connect (Withings)
        val bp = healthConnectDataSource.readBloodPressure()
        if (bp != null) dataSources["bloodPressure"] = "Withings"

        // 7-day rolling HRV
        val recentMetrics = healthMetricsDao.getRecent(7).map { it.toDomain() }
        val hrvValues = recentMetrics.map { it.hrvMs }.filter { it > 0 }
        val hrvRolling7DayAvg = if (hrvValues.isNotEmpty()) {
            (hrvValues + hrvMs).filter { it > 0 }.average()
        } else { hrvMs }

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
            bodyFatPercentage = bodyFat,
            dataSources = dataSources
        )

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
