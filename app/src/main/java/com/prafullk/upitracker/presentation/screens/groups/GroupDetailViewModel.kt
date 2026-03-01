package com.prafullk.upitracker.presentation.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GroupDetailUiState(
        val group: Group? = null,
        val transactions: List<Transaction> = emptyList(),
        val totalSpend: Double = 0.0,
        val isLoading: Boolean = true
)

class GroupDetailViewModel(
        private val groupId: String,
        private val observeGroups: ObserveGroupsUseCase,
        private val observeTransactions: ObserveTransactionsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<GroupDetailUiState> =
            combine(observeGroups(), observeTransactions(), _isLoading) {
                            groups,
                            transactions,
                            isLoad ->
                        val group = groups.find { it.id == groupId }
                        val groupTransactions = transactions.filter { it.groupId == groupId }
                        val spend =
                                groupTransactions.filter { it.direction == "DEBIT" }.sumOf {
                                    it.amount
                                }

                        GroupDetailUiState(
                                group = group,
                                transactions = groupTransactions,
                                totalSpend = spend,
                                isLoading = isLoad
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = GroupDetailUiState()
                    )
}
