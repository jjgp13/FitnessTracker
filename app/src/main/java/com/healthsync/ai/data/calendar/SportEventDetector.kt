package com.healthsync.ai.data.calendar

import com.healthsync.ai.domain.model.SportEventConfig
import com.healthsync.ai.domain.model.SportType
import javax.inject.Inject

class SportEventDetector @Inject constructor() {

    fun detectSportType(eventTitle: String, eventId: Long, config: SportEventConfig): SportType? {
        // Check manual overrides first
        if (config.manualOverrides.containsKey(eventId)) {
            return config.manualOverrides[eventId]
        }

        val titleLower = eventTitle.lowercase()

        // Match in priority order: SOCCER, VOLLEYBALL, OTHER_SPORT
        val orderedTypes = listOf(SportType.SOCCER, SportType.VOLLEYBALL, SportType.OTHER_SPORT)
        for (sportType in orderedTypes) {
            val keywords = config.autoDetectKeywords[sportType] ?: emptyList()
            if (keywords.any { titleLower.contains(it.lowercase()) }) {
                return sportType
            }
        }

        return null
    }
}
