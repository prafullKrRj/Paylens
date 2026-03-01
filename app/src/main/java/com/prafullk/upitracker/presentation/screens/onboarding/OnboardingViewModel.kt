package com.prafullk.upitracker.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class OnboardingUiState(
        val accessibilityEnabled: Boolean = false,
        val notificationsEnabled: Boolean = false,
        val smsEnabled: Boolean = false
)

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> =
            _uiState.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = OnboardingUiState()
            )

    fun completeOnboarding() {
        // Here we would use DataStore to save the onboarding flag
    }
}
