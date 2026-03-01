package com.prafullk.upitracker.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
        val monthlyBudget: Double = 15000.0,
        val currency: String = "INR",
        val isLoading: Boolean = false
)

class SettingsViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
            MutableStateFlow(SettingsUiState()) // Stub DataStore integration
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = SettingsUiState()
                    )

    fun clearData() {
        // Will implement data clearing using Room clearAllTables
    }
}
