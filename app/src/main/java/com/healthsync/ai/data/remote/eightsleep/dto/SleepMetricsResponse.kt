package com.healthsync.ai.data.remote.eightsleep.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SleepMetricsResponse(
    val sleeps: List<SleepSession> = emptyList()
)

@Serializable
data class SleepSession(
    val id: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("stages") val stages: List<SleepStage> = emptyList(),
    @SerialName("timeseries") val timeseries: SleepTimeseries? = null
)

@Serializable
data class SleepStage(
    val stage: String? = null,
    val duration: Long? = null
)

@Serializable
data class SleepTimeseries(
    val hrv: List<TimeseriesPoint> = emptyList(),
    @SerialName("heartRate") val heartRate: List<TimeseriesPoint> = emptyList(),
    @SerialName("respiratoryRate") val respiratoryRate: List<TimeseriesPoint> = emptyList()
)

@Serializable
data class TimeseriesPoint(
    val time: String? = null,
    val value: Double? = null
)
