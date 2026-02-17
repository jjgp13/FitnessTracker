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
        var sleepDuration = 0; var deepSleep = 0; var remSleep = 0
        var lightSleep = 0; var awakeMins = 0; var sleepScore: Int? = null
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
                    val mins = ((stage.duration ?: 0) / 60).toInt()
                    when (stage.stage?.lowercase()) {
                        "deep" -> deepSleep += mins
                        "rem" -> remSleep += mins
                        "light" -> lightSleep += mins
                        "awake" -> awakeMins += mins
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

        // Fallback to Health Connect (Fitbit) for sleep
        if (sleepDuration == 0) {
            // readSleepData now uses "last night" window (yesterday 6pm → today noon)
            val sleepSessions = healthConnectDataSource.readSleepData(today)
            sleepDuration = HealthConnectMapper.mapSleepDuration(sleepSessions)
            deepSleep = HealthConnectMapper.mapDeepSleepMinutes(sleepSessions)
            remSleep = HealthConnectMapper.mapRemSleepMinutes(sleepSessions)
            lightSleep = HealthConnectMapper.mapLightSleepMinutes(sleepSessions)
            awakeMins = HealthConnectMapper.mapAwakeMinutes(sleepSessions)
            if (sleepDuration > 0) {
                dataSources["sleep"] = "Fitbit"
                metricDates["sleep"] = today.toString()
            }
        }

        // Fallback HR — query day-by-day going back up to 3 days
        if (restingHr == 0) {
            for (daysBack in 0..2) {
                val queryDate = today.minusDays(daysBack.toLong())
                val hr = healthConnectDataSource.readHeartRate(queryDate)
                if (hr != null && hr > 0) {
                    restingHr = hr
                    dataSources["restingHr"] = "Fitbit"
                    metricDates["restingHr"] = queryDate.toString()
                    break
                }
            }
        }

        // Fallback HRV — query day-by-day going back up to 3 days
        if (hrvMs == 0.0) {
            for (daysBack in 0..2) {
                val queryDate = today.minusDays(daysBack.toLong())
                val hrvRecords = healthConnectDataSource.readHrvData(queryDate)
                val avg = HealthConnectMapper.mapHrvAverage(hrvRecords)
                if (avg > 0) {
                    hrvMs = avg
                    dataSources["hrv"] = "Fitbit"
                    metricDates["hrv"] = queryDate.toString()
                    break
                }
            }
        }

        // STEPS: Yesterday's data from Health Connect (Fitbit)
        val steps = healthConnectDataSource.readSteps(yesterday)
        if (steps > 0) {
            dataSources["steps"] = "Fitbit"
            metricDates["steps"] = yesterday.toString()
        }

        // EXERCISE: Yesterday's sessions from Health Connect (Fitbit)
        val exerciseRecords = healthConnectDataSource.readExerciseSessions(yesterday)
        val exerciseSessions = HealthConnectMapper.mapExerciseSessions(exerciseRecords)
        if (exerciseSessions.isNotEmpty()) {
            dataSources["exercise"] = "Fitbit"
            metricDates["exercise"] = yesterday.toString()
        }

        // Weight: from Health Connect (Withings)
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
            lightSleepMinutes = lightSleep,
            awakeMinutes = awakeMins,
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
            metricDates = metricDates,
            exerciseSessions = exerciseSessions.map {
                com.healthsync.ai.domain.model.ExerciseSummaryDomain(
                    type = it.type, title = it.title,
                    durationMinutes = it.durationMinutes,
                    startTime = it.startTime, notes = it.notes
                )
            }
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
