package com.healthsync.ai.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.model.SportEventConfig
import com.healthsync.ai.domain.model.SportType
import com.healthsync.ai.domain.model.UserProfile
import com.healthsync.ai.domain.repository.AuthRepository
import com.healthsync.ai.domain.repository.CalendarRepository
import com.healthsync.ai.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userProfile: UserProfile? = null,
    val sportEventConfig: SportEventConfig? = null,
    val isSignedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val calendarRepository: CalendarRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val config = calendarRepository.getSportEventConfig()
                _uiState.update { it.copy(sportEventConfig = config) }
            } catch (_: Exception) { }

            viewModelScope.launch {
                userProfileRepository.getUserProfile().collect { profile ->
                    _uiState.update {
                        it.copy(isLoading = false, userProfile = profile)
                    }
                }
            }
        }
    }

    fun updateSportKeywords(sportType: SportType, keywords: List<String>) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.sportEventConfig ?: SportEventConfig()
                val updated = current.copy(
                    autoDetectKeywords = current.autoDetectKeywords + (sportType to keywords)
                )
                calendarRepository.saveSportEventConfig(updated)
                _uiState.update { it.copy(sportEventConfig = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.update { it.copy(isSignedOut = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
