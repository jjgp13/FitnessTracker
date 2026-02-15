package com.healthsync.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthsync.ai.domain.model.Workout
import com.healthsync.ai.domain.model.WorkoutType

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (workout.type) {
        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
        WorkoutType.SOCCER_DRILLS -> Icons.Default.SportsSoccer
        WorkoutType.VOLLEYBALL -> Icons.Default.SportsVolleyball
        WorkoutType.RECOVERY -> Icons.Default.SelfImprovement
        WorkoutType.REST -> Icons.AutoMirrored.Filled.DirectionsRun
    }

    val typeName = when (workout.type) {
        WorkoutType.STRENGTH -> "Strength Training"
        WorkoutType.SOCCER_DRILLS -> "Soccer Drills"
        WorkoutType.VOLLEYBALL -> "Volleyball"
        WorkoutType.RECOVERY -> "Recovery"
        WorkoutType.REST -> "Rest Day"
    }

    val exerciseCount = workout.warmUp.size + workout.mainBlock.size + workout.coolDown.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$exerciseCount exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("${workout.estimatedDurationMinutes} min") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("$exerciseCount exercises") }
                )
            }
        }
    }
}
