package com.prafullk.upitracker.presentation.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.usecase.transaction.GetSpendingAnalyticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// A simplified ui state for initial launch
data class AnalyticsUiState(val monthlySpend: Double = 0.0, val isLoading: Boolean = false)

class AnalyticsViewModel(private val analyticsUseCase: GetSpendingAnalyticsUseCase) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<AnalyticsUiState> =
            MutableStateFlow(AnalyticsUiState()) // Stub for now
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = AnalyticsUiState()
                    )
}
