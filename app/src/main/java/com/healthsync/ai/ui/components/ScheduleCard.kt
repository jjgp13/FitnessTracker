package com.healthsync.ai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healthsync.ai.domain.model.CalendarEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DayContext {
    GAME_DAY, PRE_GAME, POST_GAME, TRAINING_DAY, REST_DAY
}

@Composable
fun ScheduleCard(
    dayContext: DayContext,
    todayEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    val (contextColor, contextIcon, contextLabel) = when (dayContext) {
        DayContext.GAME_DAY -> Triple(Color(0xFFFF5722), Icons.Default.EmojiEvents, "Game Day")
        DayContext.PRE_GAME -> Triple(Color(0xFFFF9800), Icons.Default.Schedule, "Pre-Game Day")
        DayContext.POST_GAME -> Triple(Color(0xFFFF9800), Icons.Default.Schedule, "Post-Game Day")
        DayContext.TRAINING_DAY -> Triple(Color(0xFF4CAF50), Icons.Default.FitnessCenter, "Training Day")
        DayContext.REST_DAY -> Triple(Color(0xFF2196F3), Icons.Default.Hotel, "Rest Day")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = contextColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = contextIcon,
                    contentDescription = contextLabel,
                    tint = contextColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = contextColor
                )
            }
            if (todayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                todayEvents.forEach { event ->
                    val time = event.startTime
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("h:mm a"))
                    Text(
                        text = "$time — ${event.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun determineDayContext(
    today: LocalDate,
    gameDays: List<LocalDate>
): DayContext {
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    return when {
        today in gameDays -> DayContext.GAME_DAY
        tomorrow in gameDays -> DayContext.PRE_GAME
        yesterday in gameDays -> DayContext.POST_GAME
        else -> DayContext.TRAINING_DAY
    }
}
