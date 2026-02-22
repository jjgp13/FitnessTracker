package com.healthsync.ai.ui.screen.morning_briefing

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.healthsync.ai.ui.components.LoadingIndicator
import com.healthsync.ai.ui.components.MetricCard
import com.healthsync.ai.ui.components.MetricStatus
import com.healthsync.ai.ui.components.RecoveryBanner
import com.healthsync.ai.ui.components.ScheduleCard
import com.healthsync.ai.ui.components.determineDayContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MorningBriefingScreen(
    onNavigateToDailyPlan: () -> Unit = {},
    viewModel: MorningBriefingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val allGranted = viewModel.healthConnectDataSource.permissions.all { it in grantedPermissions }
        viewModel.onPermissionsResult(allGranted)
    }

    LaunchedEffect(uiState.needsHealthPermissions) {
        if (uiState.needsHealthPermissions) {
            permissionLauncher.launch(viewModel.healthConnectDataSource.permissions)
        }
    }

    when {
        uiState.isLoading -> {
            LoadingIndicator(message = "Loading...")
        }
        uiState.errorMessage != null && !uiState.isSynced -> {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = {
                    if (uiState.needsHealthPermissions) {
                        permissionLauncher.launch(viewModel.healthConnectDataSource.permissions)
                    }
                }
            )
        }
        else -> {
            MorningBriefingContent(
                uiState = uiState,
                onSyncClick = { viewModel.syncHealthConnect() },
                onGeneratePlanClick = onNavigateToDailyPlan
            )
        }
    }
}

@Composable
private fun MorningBriefingContent(
    uiState: MorningBriefingUiState,
    onSyncClick: () -> Unit,
    onGeneratePlanClick: () -> Unit
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

        // Schedule Card
        uiState.weekSchedule?.let { schedule ->
            val dayContext = determineDayContext(today, schedule.gameDays)
            val todayEvents = schedule.events.filter { event ->
                event.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == today
            }
            ScheduleCard(dayContext = dayContext, todayEvents = todayEvents)
        }

        // Sync Health Connect Button
        Button(
            onClick = onSyncClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSyncing && !uiState.isSynced,
            colors = if (uiState.isSynced)
                ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            else ButtonDefaults.buttonColors()
        ) {
            when {
                uiState.isSyncing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing…")
                }
                uiState.isSynced -> {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synced")
                }
                else -> {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Health Connect")
                }
            }
        }

        // Sync error
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // After sync: Recovery Banner + Health Metrics
        if (uiState.isSynced) {
            uiState.recoveryStatus?.let { status ->
                RecoveryBanner(recoveryStatus = status)
            }

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
            }

            // Generate AI Plan Button
            Button(
                onClick = onGeneratePlanClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Plan")
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

    // HRV — gold standard for readiness
    val hrvStatus = when {
        metrics.hrvMs >= metrics.hrvRolling7DayAvg * 0.95 -> MetricStatus.GOOD
        metrics.hrvMs >= metrics.hrvRolling7DayAvg * 0.90 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    // Resting HR — elevated RHR signals fatigue/dehydration
    val hrStatus = when {
        metrics.restingHeartRate <= 60 -> MetricStatus.GOOD
        metrics.restingHeartRate <= 75 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    // Sleep duration
    val sleepHrs = metrics.sleepDurationMinutes / 60
    val sleepMins = metrics.sleepDurationMinutes % 60
    val sleepHoursDecimal = metrics.sleepDurationMinutes / 60.0
    val sleepStatus = when {
        sleepHoursDecimal >= 7.0 -> MetricStatus.GOOD
        sleepHoursDecimal >= 6.0 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    // Sleep debt — minutes below 8h target; 2+ hours means avoid high-intensity
    val sleepDebtMins = maxOf(0, 480 - metrics.sleepDurationMinutes)
    val sleepDebtHrs = sleepDebtMins / 60
    val sleepDebtRemMins = sleepDebtMins % 60
    val sleepDebtStatus = when {
        sleepDebtMins <= 30 -> MetricStatus.GOOD
        sleepDebtMins <= 120 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    // Deep sleep — <60 min means skip heavy strength, focus on technical work
    val deepSleepStatus = when {
        metrics.deepSleepMinutes >= 60 -> MetricStatus.GOOD
        metrics.deepSleepMinutes >= 45 -> MetricStatus.WARNING
        else -> MetricStatus.ALERT
    }

    val items = mutableListOf(
        MetricItem(Icons.Default.MonitorHeart, "HRV", "%.0f".format(metrics.hrvMs), "ms", formatSourceDate("hrv"), hrvStatus),
        MetricItem(Icons.Default.Favorite, "Resting HR", "${metrics.restingHeartRate}", "bpm", formatSourceDate("restingHr"), hrStatus),
        MetricItem(Icons.Default.Bedtime, "Sleep", "${sleepHrs}h ${sleepMins}m", null, formatSourceDate("sleep"), sleepStatus),
        MetricItem(Icons.Default.Warning, "Sleep Debt", if (sleepDebtMins == 0) "None" else "${sleepDebtHrs}h ${sleepDebtRemMins}m", null, null, sleepDebtStatus),
        MetricItem(Icons.Default.NightsStay, "Deep Sleep", "${metrics.deepSleepMinutes}", "min", formatSourceDate("sleep"), deepSleepStatus)
    )

    // Sleep score from Eight Sleep (quality indicator)
    metrics.sleepScore?.let { score ->
        val scoreStatus = when {
            score >= 80 -> MetricStatus.GOOD
            score >= 60 -> MetricStatus.WARNING
            else -> MetricStatus.ALERT
        }
        items.add(5, MetricItem(Icons.Default.Star, "Sleep Score", "$score", "/100", formatSourceDate("sleep"), scoreStatus))
    }

    metrics.weight?.let {
        items.add(MetricItem(Icons.Default.Person, "Weight", "%.1f".format(it), "kg", formatSourceDate("weight"), MetricStatus.GOOD))
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
