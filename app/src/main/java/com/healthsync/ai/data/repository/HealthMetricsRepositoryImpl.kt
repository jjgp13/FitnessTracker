package com.healthsync.ai.data.repository

import android.util.Log
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

private const val TAG = "HealthMetrics"

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

        // === SLEEP: Eight Sleep API first, fallback to Health Connect ===
        var sleepDuration = 0; var deepSleep = 0; var remSleep = 0
        var lightSleep = 0; var awakeMins = 0; var sleepScore: Int? = null
        var hrvMs = 0.0; var restingHr = 0

        Log.d(TAG, "Eight Sleep API authenticated: ${eightSleepDataSource.isAuthenticated()}")

        if (eightSleepDataSource.isAuthenticated()) {
            val result = eightSleepDataSource.getSleepMetrics(today.toString(), today.toString())
            Log.d(TAG, "Eight Sleep API result: success=${result.isSuccess}, sleeps=${result.getOrNull()?.sleeps?.size ?: 0}")
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

        Log.d(TAG, "After Eight Sleep API: sleepDuration=$sleepDuration, hrvMs=$hrvMs, restingHr=$restingHr")

        // Fallback to Health Connect for sleep (detects Eight Sleep / Fitbit via dataOrigin)
        if (sleepDuration == 0) {
            val sleepSessions = healthConnectDataSource.readSleepData(today)
            Log.d(TAG, "Health Connect sleep sessions: ${sleepSessions.size}")
            sleepSessions.forEach { s ->
                Log.d(TAG, "  Session source: ${s.metadata.dataOrigin.packageName}")
            }
            sleepDuration = HealthConnectMapper.mapSleepDuration(sleepSessions)
            deepSleep = HealthConnectMapper.mapDeepSleepMinutes(sleepSessions)
            remSleep = HealthConnectMapper.mapRemSleepMinutes(sleepSessions)
            lightSleep = HealthConnectMapper.mapLightSleepMinutes(sleepSessions)
            awakeMins = HealthConnectMapper.mapAwakeMinutes(sleepSessions)
            if (sleepDuration > 0) {
                val source = sleepSessions.firstOrNull()
                    ?.metadata?.dataOrigin?.packageName
                    ?.let { mapPackageToSource(it) } ?: "Health Connect"
                dataSources["sleep"] = source
                metricDates["sleep"] = today.toString()
            }
        }

        // Fallback HR — query day-by-day going back up to 3 days
        if (restingHr == 0) {
            for (daysBack in 0..2) {
                val queryDate = today.minusDays(daysBack.toLong())
                val hrResult = healthConnectDataSource.readHeartRate(queryDate)
                if (hrResult != null && hrResult.first > 0) {
                    restingHr = hrResult.first
                    dataSources["restingHr"] = mapPackageToSource(hrResult.second)
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
                    val source = hrvRecords.firstOrNull()
                        ?.metadata?.dataOrigin?.packageName
                        ?.let { mapPackageToSource(it) } ?: "Health Connect"
                    dataSources["hrv"] = source
                    metricDates["hrv"] = queryDate.toString()
                    break
                }
            }
        }

        Log.d(TAG, "Final sources: $dataSources")
        Log.d(TAG, "Final metrics: sleep=${sleepDuration}m, deep=${deepSleep}m, hrv=$hrvMs, rhr=$restingHr")

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
            steps = 0,
            weight = weight,
            bodyFatPercentage = bodyFat,
            dataSources = dataSources,
            metricDates = metricDates,
            exerciseSessions = emptyList()
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

    private fun mapPackageToSource(packageName: String): String = when {
        packageName.contains("eightsleep", ignoreCase = true) -> "Eight Sleep"
        packageName.contains("fitbit", ignoreCase = true) -> "Fitbit"
        packageName.contains("withings", ignoreCase = true) -> "Withings"
        packageName.contains("samsung", ignoreCase = true) -> "Samsung Health"
        packageName.contains("google", ignoreCase = true) -> "Google Fit"
        else -> "Health Connect"
    }
}
