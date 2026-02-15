package com.healthsync.ai.ui.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthsync.ai.domain.model.CalendarEvent
import com.healthsync.ai.domain.model.SportType
import com.healthsync.ai.ui.components.LoadingIndicator
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            LoadingIndicator(message = "Loading schedule...")
        }
        uiState.errorMessage != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        uiState.weekSchedule != null -> {
            ScheduleContent(uiState = uiState)
        }
    }
}

@Composable
private fun ScheduleContent(uiState: ScheduleUiState) {
    val schedule = uiState.weekSchedule!!
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 7 days of the week
        for (i in 0L..6L) {
            val date = schedule.weekStartDate.plusDays(i)
            val isToday = date == today
            val isGameDay = date in schedule.gameDays
            val isTrainingSlot = date in schedule.availableTrainingSlots
            val dayEvents = schedule.events.filter { event ->
                event.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == date
            }

            DayCard(
                date = date,
                isToday = isToday,
                isGameDay = isGameDay,
                isTrainingSlot = isTrainingSlot,
                events = dayEvents
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    isToday: Boolean,
    isGameDay: Boolean,
    isTrainingSlot: Boolean,
    events: List<CalendarEvent>
) {
    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isGameDay -> Color(0xFFFF5722).copy(alpha = 0.1f)
        isTrainingSlot -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isToday) {
                        Badge("Today", MaterialTheme.colorScheme.primary)
                    }
                    if (isGameDay) {
                        Badge("Game Day", Color(0xFFFF5722))
                    } else if (isTrainingSlot) {
                        Badge("Training", Color(0xFF4CAF50))
                    }
                }
            }

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                events.forEach { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    val time = event.startTime
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    val sportIcon = when (event.sportType) {
        SportType.SOCCER -> Icons.Default.SportsSoccer
        SportType.VOLLEYBALL -> Icons.Default.SportsVolleyball
        SportType.OTHER_SPORT -> Icons.Default.FitnessCenter
        null -> null
    }

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sportIcon != null) {
            Icon(
                imageVector = sportIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = "$time — ${event.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
