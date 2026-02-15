package com.healthsync.ai.domain.repository

import com.healthsync.ai.domain.model.CalendarEvent
import com.healthsync.ai.domain.model.SportEventConfig
import com.healthsync.ai.domain.model.WeekSchedule
import java.time.LocalDate

interface CalendarRepository {
    suspend fun getEventsForDateRange(start: LocalDate, end: LocalDate): List<CalendarEvent>
    suspend fun getWeekSchedule(weekStartDate: LocalDate): WeekSchedule
    suspend fun getSportEventConfig(): SportEventConfig
    suspend fun saveSportEventConfig(config: SportEventConfig)
}
