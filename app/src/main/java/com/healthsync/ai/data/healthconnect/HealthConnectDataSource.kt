package com.healthsync.ai.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun checkPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    fun requestPermissions(): Set<String> = permissions

    suspend fun readSleepData(date: LocalDate): List<SleepSessionRecord> {
        val client = healthConnectClient ?: return emptyList()
        val zone = ZoneId.systemDefault()
        // "Last night" window: previous day 6pm to this day noon
        val start = date.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
        val end = date.atTime(12, 0).atZone(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records
    }

    suspend fun readHeartRate(date: LocalDate): Pair<Int, String>? {
        val client = healthConnectClient ?: return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val allSamples = response.records.flatMap { it.samples }
        if (allSamples.isEmpty()) return null
        // Resting HR approximated as the lowest 10% average
        val sorted = allSamples.map { it.beatsPerMinute }.sorted()
        val count = maxOf(1, sorted.size / 10)
        val sourcePackage = response.records.firstOrNull()?.metadata?.dataOrigin?.packageName ?: ""
        return Pair(sorted.take(count).average().toInt(), sourcePackage)
    }

    suspend fun readSteps(date: LocalDate): Int {
        val client = healthConnectClient ?: return 0
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.sumOf { it.count }.toInt()
    }

    suspend fun readWeight(): Pair<Double, LocalDate>? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(90L * 24 * 60 * 60) // last 90 days
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val record = response.records.maxByOrNull { it.time } ?: return null
        return Pair(
            record.weight.inKilograms,
            record.time.atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    suspend fun readBodyFat(): Pair<Double, LocalDate>? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(90L * 24 * 60 * 60) // last 90 days
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val record = response.records.maxByOrNull { it.time } ?: return null
        return Pair(
            record.percentage.value,
            record.time.atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    suspend fun readBloodPressure(): Triple<Int, Int, LocalDate>? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(90L * 24 * 60 * 60) // last 90 days
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val latest = response.records.maxByOrNull { it.time } ?: return null
        return Triple(
            latest.systolic.inMillimetersOfMercury.toInt(),
            latest.diastolic.inMillimetersOfMercury.toInt(),
            latest.time.atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    suspend fun readHrvData(date: LocalDate): List<HeartRateVariabilityRmssdRecord> {
        val client = healthConnectClient ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records
    }

    suspend fun readExerciseSessions(date: LocalDate): List<ExerciseSessionRecord> {
        val client = healthConnectClient ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records
    }
}
