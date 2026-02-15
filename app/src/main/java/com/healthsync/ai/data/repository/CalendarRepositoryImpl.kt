package com.healthsync.ai.data.repository

import android.content.SharedPreferences
import com.healthsync.ai.data.calendar.CalendarDataSource
import com.healthsync.ai.data.calendar.SportEventDetector
import com.healthsync.ai.domain.model.CalendarEvent
import com.healthsync.ai.domain.model.SportEventConfig
import com.healthsync.ai.domain.model.SportType
import com.healthsync.ai.domain.model.WeekSchedule
import com.healthsync.ai.domain.repository.CalendarRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val calendarDataSource: CalendarDataSource,
    private val sportEventDetector: SportEventDetector,
    @Named("calendar") private val sharedPreferences: SharedPreferences
) : CalendarRepository {

    companion object {
        private const val KEY_SPORT_EVENT_CONFIG = "sport_event_config"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getEventsForDateRange(start: LocalDate, end: LocalDate): List<CalendarEvent> {
        val config = getSportEventConfig()
        val rawEvents = calendarDataSource.getEvents(start, end)
        return rawEvents.map { event ->
            val sportType = sportEventDetector.detectSportType(event.title, event.id, config)
            event.copy(sportType = sportType)
        }
    }

    override suspend fun getWeekSchedule(weekStartDate: LocalDate): WeekSchedule {
        val weekEnd = weekStartDate.plusDays(6)
        val events = getEventsForDateRange(weekStartDate, weekEnd)
        val zone = ZoneId.systemDefault()

        val gameDays = events
            .filter { it.sportType == SportType.SOCCER || it.sportType == SportType.VOLLEYBALL }
            .map { it.startTime.atZone(zone).toLocalDate() }
            .distinct()

        val sportEventDays = events
            .filter { it.sportType != null }
            .map { it.startTime.atZone(zone).toLocalDate() }
            .distinct()
            .toSet()

        val allDaysInWeek = (0L..6L).map { weekStartDate.plusDays(it) }
        val availableTrainingSlots = allDaysInWeek.filter { it !in sportEventDays }

        return WeekSchedule(
            weekStartDate = weekStartDate,
            events = events,
            gameDays = gameDays,
            availableTrainingSlots = availableTrainingSlots
        )
    }

    override suspend fun getSportEventConfig(): SportEventConfig {
        val configJson = sharedPreferences.getString(KEY_SPORT_EVENT_CONFIG, null)
            ?: return SportEventConfig()
        return try {
            json.decodeFromString<SportEventConfigDto>(configJson).toDomain()
        } catch (e: Exception) {
            SportEventConfig()
        }
    }

    override suspend fun saveSportEventConfig(config: SportEventConfig) {
        val dto = SportEventConfigDto.fromDomain(config)
        val configJson = json.encodeToString(dto)
        sharedPreferences.edit().putString(KEY_SPORT_EVENT_CONFIG, configJson).apply()
    }
}

@kotlinx.serialization.Serializable
private data class SportEventConfigDto(
    val autoDetectKeywords: Map<String, List<String>> = emptyMap(),
    val manualOverrides: Map<String, String?> = emptyMap()
) {
    fun toDomain(): SportEventConfig = SportEventConfig(
        autoDetectKeywords = autoDetectKeywords.mapKeys { SportType.valueOf(it.key) },
        manualOverrides = manualOverrides.mapKeys { it.key.toLong() }
            .mapValues { it.value?.let { v -> SportType.valueOf(v) } }
    )

    companion object {
        fun fromDomain(config: SportEventConfig): SportEventConfigDto = SportEventConfigDto(
            autoDetectKeywords = config.autoDetectKeywords.mapKeys { it.key.name },
            manualOverrides = config.manualOverrides.mapKeys { it.key.toString() }
                .mapValues { it.value?.name }
        )
    }
}
