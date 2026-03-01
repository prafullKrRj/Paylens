package com.prafullk.upitracker.presentation.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GroupListUiState(
        val topGroups: List<GroupWithSpending> = emptyList(),
        val otherGroups: List<GroupWithSpending> = emptyList(),
        val isLoading: Boolean = true
)

data class GroupWithSpending(val group: Group, val currentSpend: Double, val budget: Double?)

class GroupViewModel(
        private val observeGroups: ObserveGroupsUseCase,
        private val observeTransactions: ObserveTransactionsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<GroupListUiState> =
            combine(observeGroups(), observeTransactions(), _isLoading) {
                            groups,
                            transactions,
                            isLoad ->
                        val mapped =
                                groups
                                        .map { group ->
                                            val spending =
                                                    transactions
                                                            .filter {
                                                                it.groupId == group.id &&
                                                                        it.direction == "DEBIT"
                                                            }
                                                            .sumOf { it.amount }

                                            GroupWithSpending(group, spending, group.monthlyBudget)
                                        }
                                        .sortedByDescending { it.currentSpend }

                        GroupListUiState(
                                topGroups = mapped.take(5),
                                otherGroups = mapped.drop(5),
                                isLoading = isLoad
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = GroupListUiState()
                    )
}
