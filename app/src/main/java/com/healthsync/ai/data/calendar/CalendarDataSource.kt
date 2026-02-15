package com.healthsync.ai.data.calendar

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import com.healthsync.ai.domain.model.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) {
    companion object {
        private val PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )
    }

    fun getEvents(start: LocalDate, end: LocalDate): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        val cursor: Cursor? = try {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
        } catch (e: SecurityException) {
            // Calendar permission not granted
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1) ?: ""
                val dtStart = it.getLong(2)
                val dtEnd = it.getLong(3).let { end -> if (end > 0) end else dtStart + 3600000 }
                val allDay = it.getInt(4) == 1
                val calendarName = it.getString(5) ?: ""

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        startTime = Instant.ofEpochMilli(dtStart),
                        endTime = Instant.ofEpochMilli(dtEnd),
                        sportType = null, // detection applied later
                        isAllDay = allDay,
                        calendarName = calendarName
                    )
                )
            }
        }
        return events
    }
}
