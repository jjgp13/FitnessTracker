package com.healthsync.ai.ui.screen.daily_plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.usecase.GenerateDailyPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DailyPlanUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val dailyPlan: DailyPlan? = null
)

@HiltViewModel
class DailyPlanViewModel @Inject constructor(
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyPlanUiState())
    val uiState: StateFlow<DailyPlanUiState> = _uiState.asStateFlow()

    init {
        generatePlan()
    }

    fun retry() {
        generatePlan()
    }

    private fun generatePlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = generateDailyPlanUseCase(LocalDate.now())
            result.onSuccess { plan ->
                _uiState.update { it.copy(isLoading = false, dailyPlan = plan) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to generate plan")
                }
            }
        }
    }
}
