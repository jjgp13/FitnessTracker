package com.healthsync.ai.ui.screen.morning_briefing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
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

    // Health Connect permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val allGranted = viewModel.healthConnectDataSource.permissions.all { it in grantedPermissions }
        viewModel.onPermissionsResult(allGranted)
    }

    // Automatically launch permission request when needed
    LaunchedEffect(uiState.needsHealthPermissions) {
        if (uiState.needsHealthPermissions) {
            permissionLauncher.launch(viewModel.healthConnectDataSource.permissions)
        }
    }

    when {
        uiState.isLoading -> {
            LoadingIndicator(message = "Preparing your morning briefing...")
        }
        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = {
                    if (uiState.needsHealthPermissions) {
                        permissionLauncher.launch(viewModel.healthConnectDataSource.permissions)
                    } else {
                        viewModel.refresh()
                    }
                }
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

        // AI Warning Banner (quota exceeded, etc.)
        uiState.aiWarning?.let { warning ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buildMetricItems(metrics).forEach { item ->
                    MetricCard(
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        unit = item.unit,
                        sourceInfo = item.sourceInfo,
                        status = item.status
                    )
                }
            }

            // Exercise Sessions (Yesterday)
            if (metrics.exerciseSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Yesterday's Activity",
                    style = MaterialTheme.typography.titleMedium
                )
                metrics.exerciseSessions.forEach { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = exercise.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                exercise.notes?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "${exercise.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
    val sourceInfo: String?,
    val status: MetricStatus
)

private fun buildMetricItems(metrics: HealthMetrics): List<MetricItem> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    fun formatSourceDate(key: String): String? {
        val source = metrics.dataSources[key]
        val dateStr = metrics.metricDates[key]
        if (source == null) return null
        val dateLabel = when {
            dateStr == null -> ""
            dateStr == today.toString() -> "Today"
            dateStr == yesterday.toString() -> "Yesterday"
            else -> try {
                LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("MMM d"))
            } catch (_: Exception) { dateStr }
        }
        return if (dateLabel.isNotEmpty()) "$source · $dateLabel" else source
    }

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

    val items = mutableListOf(
        MetricItem(Icons.Default.Bedtime, "Sleep", "%.1f".format(sleepHours), "hrs", formatSourceDate("sleep"), sleepStatus),
        MetricItem(Icons.Default.MonitorHeart, "HRV", "%.0f".format(metrics.hrvMs), "ms", formatSourceDate("hrv"), hrvStatus),
        MetricItem(Icons.Default.Favorite, "Resting HR", "${metrics.restingHeartRate}", "bpm", formatSourceDate("restingHr"), hrStatus),
        MetricItem(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps (Yesterday)", "%,d".format(metrics.steps), null, formatSourceDate("steps"), MetricStatus.GOOD)
    )

    metrics.weight?.let {
        items.add(MetricItem(Icons.Default.FitnessCenter, "Weight", "%.1f".format(it), "kg", formatSourceDate("weight"), MetricStatus.GOOD))
    }
    metrics.bodyFatPercentage?.let {
        items.add(MetricItem(Icons.Default.Person, "Body Fat", "%.1f".format(it), "%", formatSourceDate("bodyFat"), MetricStatus.GOOD))
    }
    if (metrics.bloodPressureSystolic != null && metrics.bloodPressureDiastolic != null) {
        val bpStatus = when {
            metrics.bloodPressureSystolic <= 120 && metrics.bloodPressureDiastolic <= 80 -> MetricStatus.GOOD
            metrics.bloodPressureSystolic <= 140 && metrics.bloodPressureDiastolic <= 90 -> MetricStatus.WARNING
            else -> MetricStatus.ALERT
        }
        items.add(MetricItem(Icons.Default.Speed, "Blood Pressure", "${metrics.bloodPressureSystolic}/${metrics.bloodPressureDiastolic}", "mmHg", formatSourceDate("bloodPressure"), bpStatus))
    }

    return items
}
