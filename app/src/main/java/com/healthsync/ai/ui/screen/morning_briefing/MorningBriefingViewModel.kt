package com.healthsync.ai.ui.screen.morning_briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.DailyPlan
import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.WeekSchedule
import com.healthsync.ai.domain.repository.UserProfileRepository
import com.healthsync.ai.domain.usecase.DetermineRecoveryStatusUseCase
import com.healthsync.ai.domain.usecase.FetchMorningMetricsUseCase
import com.healthsync.ai.domain.usecase.GenerateDailyPlanUseCase
import com.healthsync.ai.domain.usecase.GetWeekScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class MorningBriefingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val healthMetrics: HealthMetrics? = null,
    val recoveryStatus: RecoveryStatus? = null,
    val dailyPlan: DailyPlan? = null,
    val weekSchedule: WeekSchedule? = null,
    val userName: String? = null
)

@HiltViewModel
class MorningBriefingViewModel @Inject constructor(
    private val fetchMorningMetricsUseCase: FetchMorningMetricsUseCase,
    private val determineRecoveryStatusUseCase: DetermineRecoveryStatusUseCase,
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorningBriefingUiState())
    val uiState: StateFlow<MorningBriefingUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Load user name
                val profile = userProfileRepository.getUserProfile().firstOrNull()
                _uiState.update { it.copy(userName = profile?.displayName) }

                // Fetch metrics
                val metricsResult = fetchMorningMetricsUseCase()
                val metrics = metricsResult.getOrThrow()
                _uiState.update { it.copy(healthMetrics = metrics) }

                // Determine recovery
                val recovery = determineRecoveryStatusUseCase(metrics)
                _uiState.update { it.copy(recoveryStatus = recovery) }

                // Get week schedule
                val today = LocalDate.now()
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val schedule = getWeekScheduleUseCase(weekStart)
                _uiState.update { it.copy(weekSchedule = schedule) }

                // Generate daily plan
                val planResult = generateDailyPlanUseCase(today)
                val plan = planResult.getOrThrow()
                _uiState.update { it.copy(dailyPlan = plan, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
}
