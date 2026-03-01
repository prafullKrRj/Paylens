package com.prafullk.upitracker.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.transaction.ClassifyTransactionUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TransactionListUiState(
        val transactions: List<Transaction> = emptyList(),
        val unclassifiedCount: Int = 0,
        val isLoading: Boolean = true
)

class TransactionViewModel(
        private val observeTransactions: ObserveTransactionsUseCase,
        private val classifyTransaction: ClassifyTransactionUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<TransactionListUiState> =
            combine(observeTransactions(), _isLoading) { transactions, currentLoading ->
                        val unclassified =
                                transactions.count {
                                    it.groupId == "sys_uncategorized" && !it.isUserClassified
                                }

                        TransactionListUiState(
                                transactions = transactions,
                                unclassifiedCount = unclassified,
                                isLoading = currentLoading
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = TransactionListUiState()
                    )
}
