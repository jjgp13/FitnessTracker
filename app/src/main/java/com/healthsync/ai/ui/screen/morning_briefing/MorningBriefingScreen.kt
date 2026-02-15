package com.healthsync.ai.ui.screen.morning_briefing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.ui.components.DayContext
import com.healthsync.ai.ui.components.LoadingIndicator
import com.healthsync.ai.ui.components.MetricCard
import com.healthsync.ai.ui.components.MetricStatus
import com.healthsync.ai.ui.components.RecoveryBanner
import com.healthsync.ai.ui.components.ScheduleCard
import com.healthsync.ai.ui.components.WorkoutCard
import com.healthsync.ai.ui.components.determineDayContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MorningBriefingScreen(
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
    viewModel: MorningBriefingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            LoadingIndicator(message = "Preparing your morning briefing...")
        }
        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = viewModel::refresh
            )
        }
        else -> {
            MorningBriefingContent(
                uiState = uiState,
                onNavigateToWorkout = onNavigateToWorkout,
                onNavigateToNutrition = onNavigateToNutrition
            )
        }
    }
}

@Composable
private fun MorningBriefingContent(
    uiState: MorningBriefingUiState,
    onNavigateToWorkout: () -> Unit,
    onNavigateToNutrition: () -> Unit
) {
    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting
        Text(
            text = "Good morning, ${uiState.userName ?: "Athlete"}",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = dateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Recovery Banner
        uiState.recoveryStatus?.let { status ->
            RecoveryBanner(recoveryStatus = status)
        }

        // Schedule Card
        uiState.weekSchedule?.let { schedule ->
            val dayContext = determineDayContext(today, schedule.gameDays)
            val todayEvents = schedule.events.filter { event ->
                event.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == today
            }
            ScheduleCard(dayContext = dayContext, todayEvents = todayEvents)
        }

        // Health Metrics
        uiState.healthMetrics?.let { metrics ->
            Text(
                text = "Health Metrics",
                style = MaterialTheme.typography.titleMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(buildMetricItems(metrics)) { item ->
                    MetricCard(
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        unit = item.unit,
                        status = item.status
                    )
                }
            }
        }

        // Workout Card
        uiState.dailyPlan?.let { plan ->
            Text(
                text = "Today's Workout",
                style = MaterialTheme.typography.titleMedium
            )
            WorkoutCard(
                workout = plan.workout,
                onClick = onNavigateToWorkout
            )
        }

        // Nutrition Summary
        uiState.dailyPlan?.let { plan ->
            Text(
                text = "Nutrition",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToNutrition),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${plan.nutritionPlan.targetCalories} kcal target",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Carbs ${plan.nutritionPlan.macros.carbsPercent}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Protein ${plan.nutritionPlan.macros.proteinPercent}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Fat ${plan.nutritionPlan.macros.fatPercent}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${plan.nutritionPlan.meals.size} meals · ${plan.nutritionPlan.snacks.size} snacks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Coach Notes
        uiState.dailyPlan?.let { plan ->
            if (plan.coachNotes.isNotBlank()) {
                Text(
                    text = "Coach Notes",
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = plan.coachNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private data class MetricItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String,
    val unit: String?,
    val status: MetricStatus
)

private fun buildMetricItems(metrics: HealthMetrics): List<MetricItem> {
    val sleepHours = metrics.sleepDurationMinutes / 60.0
    val sleepStatus = when {
        sleepHours >= 7.0 -> MetricStatus.GOOD
        sleepHours >= 6.0 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    val hrvStatus = when {
        metrics.hrvMs >= metrics.hrvRolling7DayAvg * 0.95 -> MetricStatus.GOOD
        metrics.hrvMs >= metrics.hrvRolling7DayAvg * 0.90 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    val hrStatus = when {
        metrics.restingHeartRate <= 60 -> MetricStatus.GOOD
        metrics.restingHeartRate <= 75 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    return listOf(
        MetricItem(Icons.Default.MonitorHeart, "HRV", "%.0f".format(metrics.hrvMs), "ms", hrvStatus),
        MetricItem(Icons.Default.Bedtime, "Sleep", "%.1f".format(sleepHours), "hrs", sleepStatus),
        MetricItem(Icons.Default.Favorite, "Resting HR", "${metrics.restingHeartRate}", "bpm", hrStatus),
        MetricItem(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps", "%,d".format(metrics.steps), null, MetricStatus.GOOD)
    )
}
