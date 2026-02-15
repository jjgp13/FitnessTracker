package com.healthsync.ai.ui.screen.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.NutritionPlan
import com.healthsync.ai.domain.repository.DailyPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nutritionPlan: NutritionPlan? = null
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val dailyPlanRepository: DailyPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadNutrition()
    }

    private fun loadNutrition() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val plan = dailyPlanRepository.getPlanForDate(LocalDate.now())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        nutritionPlan = plan?.nutritionPlan
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load nutrition plan"
                    )
                }
            }
        }
    }
}
