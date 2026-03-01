package com.prafullk.upitracker.presentation.screens.onboarding

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.data.detection.upi.DiscoveredUpiApp
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import com.prafullk.upitracker.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
        val accessibilityEnabled: Boolean = false,
        val notificationsEnabled: Boolean = false,
        val smsEnabled: Boolean = false,
        val discoveredApps: List<DiscoveredUpiApp> = emptyList(),
        val isScanning: Boolean = false
)

class OnboardingViewModel(
        private val discoveryService: UpiAppDiscoveryService,
        private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> =
            _uiState.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = OnboardingUiState()
            )

    fun checkAccessibilityStatus(context: Context) {
        val enabledServices =
                Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
        val isEnabled = enabledServices.contains(context.packageName, ignoreCase = true)
        _uiState.value = _uiState.value.copy(accessibilityEnabled = isEnabled)
    }

    fun scanForApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val apps = runCatching { discoveryService.discoverAndSync() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(isScanning = false, discoveredApps = apps)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { appPreferences.saveOnboardingCompleted(true) }
    }
}
