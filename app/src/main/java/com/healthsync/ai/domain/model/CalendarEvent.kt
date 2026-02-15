package com.healthsync.ai.domain.model

import java.time.Instant
import java.time.LocalDate

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val sportType: SportType?,
    val isAllDay: Boolean,
    val calendarName: String
)

enum class SportType {
    SOCCER,
    VOLLEYBALL,
    OTHER_SPORT
}

data class WeekSchedule(
    val weekStartDate: LocalDate,
    val events: List<CalendarEvent>,
    val gameDays: List<LocalDate>,
    val availableTrainingSlots: List<LocalDate>
)

data class SportEventConfig(
    val autoDetectKeywords: Map<SportType, List<String>> = defaultKeywords(),
    val manualOverrides: Map<Long, SportType?> = emptyMap()
)

fun defaultKeywords(): Map<SportType, List<String>> = mapOf(
    SportType.SOCCER to listOf("soccer", "futbol", "fútbol", "football"),
    SportType.VOLLEYBALL to listOf("volleyball", "volley", "vball"),
    SportType.OTHER_SPORT to listOf("game", "match", "practice", "training", "workout")
)
