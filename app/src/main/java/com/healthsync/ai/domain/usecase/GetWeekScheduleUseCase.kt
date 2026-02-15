package com.healthsync.ai.domain.usecase

import com.healthsync.ai.domain.model.WeekSchedule
import com.healthsync.ai.domain.repository.CalendarRepository
import java.time.LocalDate
import javax.inject.Inject

class GetWeekScheduleUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(weekStartDate: LocalDate): WeekSchedule {
        return calendarRepository.getWeekSchedule(weekStartDate)
    }
}
