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
        val metricDates = mutableMapOf<String, String>()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // === SLEEP: Eight Sleep first, fallback to Health Connect (Fitbit) ===
        var sleepDuration = 0; var deepSleep = 0; var remSleep = 0; var sleepScore: Int? = null
        var hrvMs = 0.0; var restingHr = 0

        if (eightSleepDataSource.isAuthenticated()) {
            val result = eightSleepDataSource.getSleepMetrics(today.toString(), today.toString())
            result.getOrNull()?.sleeps?.firstOrNull()?.let { session ->
                if (session.startTime != null && session.endTime != null) {
                    try {
                        val start = java.time.Instant.parse(session.startTime)
                        val end = java.time.Instant.parse(session.endTime)
                        sleepDuration = ((end.epochSecond - start.epochSecond) / 60).toInt()
                    } catch (_: Exception) {}
                }
                session.stages.forEach { stage ->
                    when (stage.stage?.lowercase()) {
                        "deep" -> deepSleep += ((stage.duration ?: 0) / 60).toInt()
                        "rem" -> remSleep += ((stage.duration ?: 0) / 60).toInt()
                    }
                }
                sleepScore = session.score
                val hrvValues = session.timeseries?.hrv?.mapNotNull { it.value }
                if (!hrvValues.isNullOrEmpty()) {
                    hrvMs = hrvValues.average()
                    dataSources["hrv"] = "Eight Sleep"
                    metricDates["hrv"] = today.toString()
                }
                val hrValues = session.timeseries?.heartRate?.mapNotNull { it.value }
                if (!hrValues.isNullOrEmpty()) {
                    restingHr = hrValues.min().toInt()
                    dataSources["restingHr"] = "Eight Sleep"
                    metricDates["restingHr"] = today.toString()
                }
                if (sleepDuration > 0) {
                    dataSources["sleep"] = "Eight Sleep"
                    metricDates["sleep"] = today.toString()
                }
            }
        }

        // Fallback to Health Connect (Fitbit) for sleep — try today, then yesterday
        if (sleepDuration == 0) {
            var sleepSessions = healthConnectDataSource.readSleepData(today)
            var sleepDate = today
            if (sleepSessions.isEmpty()) {
                sleepSessions = healthConnectDataSource.readSleepData(yesterday)
                sleepDate = yesterday
            }
            sleepDuration = HealthConnectMapper.mapSleepDuration(sleepSessions)
            deepSleep = HealthConnectMapper.mapDeepSleepMinutes(sleepSessions)
            remSleep = HealthConnectMapper.mapRemSleepMinutes(sleepSessions)
            if (sleepDuration > 0) {
                dataSources["sleep"] = "Fitbit"
                metricDates["sleep"] = sleepDate.toString()
            }
        }

        // Fallback HR — try today, then last 3 days
        if (restingHr == 0) {
            restingHr = healthConnectDataSource.readHeartRate(today) ?: 0
            if (restingHr > 0) {
                dataSources["restingHr"] = "Fitbit"
                metricDates["restingHr"] = today.toString()
            } else {
                val fallbackHr = healthConnectDataSource.readHeartRateRange(today.minusDays(3), yesterday)
                if (fallbackHr != null && fallbackHr > 0) {
                    restingHr = fallbackHr
                    dataSources["restingHr"] = "Fitbit"
                    metricDates["restingHr"] = yesterday.toString()
                }
            }
        }

        // Fallback HRV — try today, then last 3 days
        if (hrvMs == 0.0) {
            val hrvRecords = healthConnectDataSource.readHrvData(today)
            hrvMs = HealthConnectMapper.mapHrvAverage(hrvRecords)
            if (hrvMs > 0) {
                dataSources["hrv"] = "Fitbit"
                metricDates["hrv"] = today.toString()
            } else {
                val fallbackHrv = healthConnectDataSource.readHrvDataRange(today.minusDays(3), yesterday)
                hrvMs = HealthConnectMapper.mapHrvAverage(fallbackHrv)
                if (hrvMs > 0) {
                    dataSources["hrv"] = "Fitbit"
                    metricDates["hrv"] = yesterday.toString()
                }
            }
        }

        // STEPS: Yesterday's data from Health Connect (Fitbit)
        val steps = healthConnectDataSource.readSteps(yesterday)
        if (steps > 0) {
            dataSources["steps"] = "Fitbit"
            metricDates["steps"] = yesterday.toString()
        }

        // Weight: from Health Connect (Withings) — already looks back 90 days
        val weightResult = healthConnectDataSource.readWeight()
        val weight = weightResult?.first
        if (weightResult != null) {
            dataSources["weight"] = "Withings"
            metricDates["weight"] = weightResult.second.toString()
        }

        // Body Fat: from Health Connect (Withings)
        val bodyFatResult = healthConnectDataSource.readBodyFat()
        val bodyFat = bodyFatResult?.first
        if (bodyFatResult != null) {
            dataSources["bodyFat"] = "Withings"
            metricDates["bodyFat"] = bodyFatResult.second.toString()
        }

        // Blood Pressure: from Health Connect (Withings)
        val bpResult = healthConnectDataSource.readBloodPressure()
        val bpSystolic = bpResult?.first
        val bpDiastolic = bpResult?.second
        if (bpResult != null) {
            dataSources["bloodPressure"] = "Withings"
            metricDates["bloodPressure"] = bpResult.third.toString()
        }

        // 7-day rolling HRV
        val recentMetrics = healthMetricsDao.getRecent(7).map { it.toDomain() }
        val hrvValuesList = recentMetrics.map { it.hrvMs }.filter { it > 0 }
        val hrvRolling7DayAvg = if (hrvValuesList.isNotEmpty()) {
            (hrvValuesList + hrvMs).filter { it > 0 }.average()
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
            bloodPressureSystolic = bpSystolic,
            bloodPressureDiastolic = bpDiastolic,
            steps = steps,
            weight = weight,
            bodyFatPercentage = bodyFat,
            dataSources = dataSources,
            metricDates = metricDates
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
