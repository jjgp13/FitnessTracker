package com.healthsync.ai.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
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
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
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
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records
    }

    suspend fun readHeartRate(date: LocalDate): Int? {
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
        return sorted.take(count).average().toInt()
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

    suspend fun readWeight(): Double? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(30L * 24 * 60 * 60) // last 30 days
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.maxByOrNull { it.time }?.weight?.inKilograms
    }

    suspend fun readBodyFat(): Double? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(30L * 24 * 60 * 60)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.maxByOrNull { it.time }?.percentage?.value
    }

    suspend fun readBloodPressure(): Pair<Int, Int>? {
        val client = healthConnectClient ?: return null
        val end = Instant.now()
        val start = end.minusSeconds(30L * 24 * 60 * 60)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        val latest = response.records.maxByOrNull { it.time } ?: return null
        return Pair(
            latest.systolic.inMillimetersOfMercury.toInt(),
            latest.diastolic.inMillimetersOfMercury.toInt()
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
}
