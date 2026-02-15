package com.healthsync.ai.ui.screen.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.Workout
import com.healthsync.ai.domain.repository.DailyPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val workout: Workout? = null
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val dailyPlanRepository: DailyPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val plan = dailyPlanRepository.getPlanForDate(LocalDate.now())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        workout = plan?.workout
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load workout"
                    )
                }
            }
        }
    }
}
