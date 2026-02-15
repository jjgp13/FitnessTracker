package com.healthsync.ai.ui.screen.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.WeekSchedule
import com.healthsync.ai.domain.usecase.GetWeekScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val weekSchedule: WeekSchedule? = null
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val today = LocalDate.now()
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val schedule = getWeekScheduleUseCase(weekStart)
                _uiState.update {
                    it.copy(isLoading = false, weekSchedule = schedule)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load schedule"
                    )
                }
            }
        }
    }
}
