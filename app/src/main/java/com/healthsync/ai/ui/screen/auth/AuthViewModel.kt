package com.healthsync.ai.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthsync.ai.domain.repository.AuthRepository
import com.healthsync.ai.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val status: AuthStatus = AuthStatus.IDLE,
    val errorMessage: String? = null
)

enum class AuthStatus { IDLE, LOADING, SUCCESS, ERROR }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationTarget = MutableStateFlow<NavigationTarget?>(null)
    val navigationTarget: StateFlow<NavigationTarget?> = _navigationTarget.asStateFlow()

    init {
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated.first()
            if (isAuthenticated) {
                val onboarded = userProfileRepository.isOnboardingComplete()
                _navigationTarget.value = if (onboarded) {
                    NavigationTarget.MORNING_BRIEFING
                } else {
                    NavigationTarget.ONBOARDING
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(status = AuthStatus.LOADING)
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(status = AuthStatus.SUCCESS)
                val onboarded = userProfileRepository.isOnboardingComplete()
                _navigationTarget.value = if (onboarded) {
                    NavigationTarget.MORNING_BRIEFING
                } else {
                    NavigationTarget.ONBOARDING
                }
            } else {
                _uiState.value = AuthUiState(
                    status = AuthStatus.ERROR,
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign-in failed"
                )
            }
        }
    }

    fun onNavigationHandled() {
        _navigationTarget.value = null
    }

    enum class NavigationTarget { ONBOARDING, MORNING_BRIEFING }
}
