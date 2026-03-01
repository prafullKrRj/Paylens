package com.prafullk.upitracker.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.data.preferences.AppPreferences
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.GetSpendingAnalyticsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
        val monthlyTotal: Double = 0.0,
        val monthlyBudget: Double? = null,
        val todayTotal: Double = 0.0,
        val weekTotal: Double = 0.0,
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
        private val analyticsUseCase: GetSpendingAnalyticsUseCase,
        private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
            combine(
                            observeTransactions(),
                            observeGroups(),
                            _isLoading,
                            appPreferences.monthlyBudget
                    ) { transactions, groups, currentLoading, budget ->
                        val now = System.currentTimeMillis()
                        val cal = Calendar.getInstance()

                        // Start of today
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val startOfDay = cal.timeInMillis

                        // Start of this week (Monday in most locales)
                        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                        val startOfWeek = cal.timeInMillis

                        // Start of this month
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val startOfMonth = cal.timeInMillis

                        val debits = transactions.filter { it.direction == "DEBIT" }
                        val monthlyDebits = debits.filter { it.timestamp >= startOfMonth }
                        val total = monthlyDebits.sumOf { it.amount }
                        val todayTotal = debits.filter { it.timestamp >= startOfDay }.sumOf { it.amount }
                        val weekTotal = debits.filter { it.timestamp >= startOfWeek }.sumOf { it.amount }

                        val recent = transactions.take(5)

                        val spendingByGroup =
                                monthlyDebits
                                        .groupBy { it.groupId }
                                        .map { entry ->
                                            val group = groups.find { it.id == entry.key }
                                            GroupSpending(
                                                    groupId = entry.key ?: "sys_uncategorized",
                                                    groupName = group?.name ?: "Uncategorized",
                                                    groupColor = group?.color ?: 0xFF9E9E9E.toInt(),
                                                    amount = entry.value.sumOf { it.amount }
                                            )
                                        }
                                        .sortedByDescending { it.amount }
                                        .take(4)

                        HomeUiState(
                                monthlyTotal = total,
                                monthlyBudget = budget,
                                todayTotal = todayTotal,
                                weekTotal = weekTotal,
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
