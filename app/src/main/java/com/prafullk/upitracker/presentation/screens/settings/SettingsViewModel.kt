package com.prafullk.upitracker.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.data.preferences.AppPreferences
import com.prafullk.upitracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
        val monthlyBudget: Double = 15000.0,
        val currency: String = "INR",
        val isLoading: Boolean = false
)

class SettingsViewModel(
        private val transactionRepository: TransactionRepository,
        private val appPreferences: AppPreferences
) : ViewModel() {

    val showBudgetDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
            appPreferences.monthlyBudget
                    .map { budget -> SettingsUiState(monthlyBudget = budget) }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = SettingsUiState()
                    )

    fun updateBudget(amount: Double) {
        viewModelScope.launch { appPreferences.saveMonthlyBudget(amount) }
    }

    fun clearData() {
        // Will implement data clearing using Room clearAllTables
    }
}
