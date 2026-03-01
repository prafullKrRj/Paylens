package com.prafullk.upitracker.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.GetSpendingAnalyticsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
        val monthlyTotal: Double = 0.0,
        val monthlyBudget: Double? = null,
        val recentTransactions: List<Transaction> = emptyList(),
        val topSpendingGroups: List<GroupSpending> = emptyList(),
        val isLoading: Boolean = true
)

data class GroupSpending(
        val groupId: String,
        val groupName: String,
        val groupColor: Int,
        val amount: Double
)

class HomeViewModel(
        private val observeTransactions: ObserveTransactionsUseCase,
        private val observeGroups: ObserveGroupsUseCase,
        private val analyticsUseCase:
                GetSpendingAnalyticsUseCase // To be used for derived insights later
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
            combine(observeTransactions(), observeGroups(), _isLoading) {
                            transactions,
                            groups,
                            currentLoading ->
                        val debits = transactions.filter { it.direction == "DEBIT" }
                        val total = debits.sumOf { it.amount }

                        // Take top 5 recent
                        val recent = transactions.take(5)

                        // Simple mock of top spending groups aggregation logic
                        val spendingByGroup =
                                debits
                                        .groupBy { it.groupId }
                                        .map { entry ->
                                            val group = groups.find { it.id == entry.key }
                                            GroupSpending(
                                                    groupId = entry.key ?: "sys_uncategorized",
                                                    groupName = group?.name ?: "Uncategorized",
                                                    groupColor = group?.color ?: 0xFFE0E0E0.toInt(),
                                                    amount = entry.value.sumOf { it.amount }
                                            )
                                        }
                                        .sortedByDescending { it.amount }
                                        .take(4)

                        HomeUiState(
                                monthlyTotal = total,
                                monthlyBudget = 15000.0, // Stub: pull from DataStore later
                                recentTransactions = recent,
                                topSpendingGroups = spendingByGroup,
                                isLoading = currentLoading
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = HomeUiState()
                    )
}
