package com.healthsync.ai.ui.screen.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthsync.ai.domain.model.SportType

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { (uiState.currentStep + 1).toFloat() / uiState.totalSteps },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Step ${uiState.currentStep + 1} of ${uiState.totalSteps}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (uiState.currentStep) {
                0 -> ProfileStep(uiState, viewModel)
                1 -> SportsStep(uiState, viewModel)
                2 -> NutritionStep(uiState, viewModel)
                3 -> PermissionsStep(uiState, viewModel)
                4 -> KeywordsStep(uiState, viewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (uiState.currentStep > 0) {
                OutlinedButton(onClick = { viewModel.previousStep() }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (uiState.currentStep < uiState.totalSteps - 1) {
                Button(onClick = { viewModel.nextStep() }) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = { viewModel.completeOnboarding() },
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text("Complete")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Text("Your Profile", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = state.displayName,
        onValueChange = { viewModel.updateDisplayName(it) },
        label = { Text("Display Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.age,
        onValueChange = { viewModel.updateAge(it.filter { c -> c.isDigit() }) },
        label = { Text("Age") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SportsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val sports = listOf("Soccer", "Volleyball")
    val strengthExercises = listOf("Trap bar deadlifts", "Nordic curls", "Air squats")

    Text("Primary Sports", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sports.forEach { sport ->
            FilterChip(
                selected = sport in state.selectedSports,
                onClick = { viewModel.toggleSport(sport) },
                label = { Text(sport) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text("Strength Focus", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        strengthExercises.forEach { exercise ->
            FilterChip(
                selected = exercise in state.selectedStrengthFocus,
                onClick = { viewModel.toggleStrengthFocus(exercise) },
                label = { Text(exercise) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val cuisines = listOf("Mexican", "American")

    Text("Nutrition Preferences", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Dietary Preferences", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cuisines.forEach { cuisine ->
            FilterChip(
                selected = cuisine in state.dietaryPreferences,
                onClick = { viewModel.toggleDietaryPreference(cuisine) },
                label = { Text(cuisine) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text("Macro Split", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(12.dp))

    MacroSlider("Carbs", state.carbsPercent) { newCarbs ->
        val remaining = 100 - newCarbs
        val protein = (remaining * state.proteinPercent) / maxOf(state.proteinPercent + state.fatPercent, 1)
        val fat = remaining - protein
        viewModel.updateMacroSplit(newCarbs, protein, fat)
    }

    MacroSlider("Protein", state.proteinPercent) { newProtein ->
        val remaining = 100 - newProtein
        val carbs = (remaining * state.carbsPercent) / maxOf(state.carbsPercent + state.fatPercent, 1)
        val fat = remaining - carbs
        viewModel.updateMacroSplit(carbs, newProtein, fat)
    }

    MacroSlider("Fat", state.fatPercent) { newFat ->
        val remaining = 100 - newFat
        val carbs = (remaining * state.carbsPercent) / maxOf(state.carbsPercent + state.proteinPercent, 1)
        val protein = remaining - carbs
        viewModel.updateMacroSplit(carbs, protein, newFat)
    }
}

@Composable
private fun MacroSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: $value%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PermissionsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    Text("Permissions", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "HealthSync AI reads your calendar to automatically detect game days and schedule workouts around them.",
        style = MaterialTheme.typography.bodyLarge
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (state.calendarPermissionGranted) {
        Text(
            text = "✅ Calendar access granted",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Calendar Access")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Text("Sport Keywords", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "These keywords auto-detect sport events from your calendar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    state.sportKeywords.forEach { (sportType, keywords) ->
        Text(
            text = sportType.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keywords.forEach { keyword ->
                InputChip(
                    selected = true,
                    onClick = { viewModel.removeKeyword(sportType, keyword) },
                    label = { Text(keyword) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var newKeyword by remember(sportType) { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it },
                label = { Text("Add keyword") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                viewModel.addKeyword(sportType, newKeyword.trim())
                newKeyword = ""
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
