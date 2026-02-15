package com.healthsync.ai.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.Macros
import com.healthsync.ai.domain.model.SportType
import com.healthsync.ai.domain.model.UserProfile
import com.healthsync.ai.domain.model.defaultKeywords
import com.healthsync.ai.domain.repository.AuthRepository
import com.healthsync.ai.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val displayName: String = "",
    val age: String = "",
    val selectedSports: Set<String> = emptySet(),
    val selectedStrengthFocus: Set<String> = emptySet(),
    val dietaryPreferences: Set<String> = emptySet(),
    val carbsPercent: Int = 40,
    val proteinPercent: Int = 30,
    val fatPercent: Int = 30,
    val calendarPermissionGranted: Boolean = false,
    val sportKeywords: Map<SportType, List<String>> = defaultKeywords(),
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun updateAge(age: String) {
        _uiState.update { it.copy(age = age) }
    }

    fun toggleSport(sport: String) {
        _uiState.update { state ->
            val updated = state.selectedSports.toMutableSet()
            if (sport in updated) updated.remove(sport) else updated.add(sport)
            state.copy(selectedSports = updated)
        }
    }

    fun toggleStrengthFocus(exercise: String) {
        _uiState.update { state ->
            val updated = state.selectedStrengthFocus.toMutableSet()
            if (exercise in updated) updated.remove(exercise) else updated.add(exercise)
            state.copy(selectedStrengthFocus = updated)
        }
    }

    fun toggleDietaryPreference(pref: String) {
        _uiState.update { state ->
            val updated = state.dietaryPreferences.toMutableSet()
            if (pref in updated) updated.remove(pref) else updated.add(pref)
            state.copy(dietaryPreferences = updated)
        }
    }

    fun updateMacroSplit(carbs: Int, protein: Int, fat: Int) {
        _uiState.update { it.copy(carbsPercent = carbs, proteinPercent = protein, fatPercent = fat) }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(calendarPermissionGranted = granted) }
    }

    fun addKeyword(sportType: SportType, keyword: String) {
        if (keyword.isBlank()) return
        _uiState.update { state ->
            val updated = state.sportKeywords.toMutableMap()
            val existing = updated[sportType].orEmpty().toMutableList()
            if (keyword.lowercase() !in existing.map { it.lowercase() }) {
                existing.add(keyword)
                updated[sportType] = existing
            }
            state.copy(sportKeywords = updated)
        }
    }

    fun removeKeyword(sportType: SportType, keyword: String) {
        _uiState.update { state ->
            val updated = state.sportKeywords.toMutableMap()
            val existing = updated[sportType].orEmpty().toMutableList()
            existing.remove(keyword)
            updated[sportType] = existing
            state.copy(sportKeywords = updated)
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.totalSteps - 1) {
                state.copy(currentStep = state.currentStep + 1)
            } else {
                state
            }
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 0) {
                state.copy(currentStep = state.currentStep - 1)
            } else {
                state
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val profile = UserProfile(
                id = userId,
                displayName = state.displayName,
                age = state.age.toIntOrNull() ?: 0,
                primarySports = state.selectedSports.toList(),
                strengthFocus = state.selectedStrengthFocus.toList(),
                dietaryPreferences = state.dietaryPreferences.toList(),
                macroSplit = Macros(
                    carbsPercent = state.carbsPercent,
                    proteinPercent = state.proteinPercent,
                    fatPercent = state.fatPercent
                )
            )
            userProfileRepository.saveUserProfile(profile)
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
