package com.healthsync.ai.ui.screen.morning_briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.data.healthconnect.HealthConnectDataSource
import com.healthsync.ai.domain.model.HealthMetrics
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.domain.model.WeekSchedule
import com.healthsync.ai.domain.repository.UserProfileRepository
import com.healthsync.ai.domain.usecase.DetermineRecoveryStatusUseCase
import com.healthsync.ai.domain.usecase.FetchMorningMetricsUseCase
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
    val isSyncing: Boolean = false,
    val isSynced: Boolean = false,
    val errorMessage: String? = null,
    val healthMetrics: HealthMetrics? = null,
    val recoveryStatus: RecoveryStatus? = null,
    val weekSchedule: WeekSchedule? = null,
    val userName: String? = null,
    val needsHealthPermissions: Boolean = false
)

@HiltViewModel
class MorningBriefingViewModel @Inject constructor(
    private val fetchMorningMetricsUseCase: FetchMorningMetricsUseCase,
    private val determineRecoveryStatusUseCase: DetermineRecoveryStatusUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val userProfileRepository: UserProfileRepository,
    val healthConnectDataSource: HealthConnectDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorningBriefingUiState())
    val uiState: StateFlow<MorningBriefingUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(needsHealthPermissions = false, isLoading = false) }
        } else {
            _uiState.update {
                it.copy(
                    needsHealthPermissions = true,
                    isLoading = false,
                    errorMessage = "Health Connect permissions are required. Please grant them and tap Retry."
                )
            }
        }
    }

    fun syncHealthConnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                val metricsResult = fetchMorningMetricsUseCase()
                val metrics = metricsResult.getOrThrow()
                val recovery = determineRecoveryStatusUseCase(metrics)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isSynced = true,
                        healthMetrics = metrics,
                        recoveryStatus = recovery
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            try {
                val hasPermissions = healthConnectDataSource.checkPermissions()
                if (!hasPermissions) {
                    _uiState.update { it.copy(needsHealthPermissions = true, isLoading = false) }
                    return@launch
                }

                val profile = userProfileRepository.getUserProfile().firstOrNull()
                _uiState.update { it.copy(userName = profile?.displayName) }

                val today = LocalDate.now()
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val schedule = getWeekScheduleUseCase(weekStart)
                _uiState.update { it.copy(weekSchedule = schedule, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load"
                    )
                }
            }
        }
    }
}
