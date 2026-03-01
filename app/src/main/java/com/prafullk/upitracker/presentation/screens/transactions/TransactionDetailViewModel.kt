package com.prafullk.upitracker.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ClassifyTransactionUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
        val transaction: Transaction? = null,
        val allGroups: List<Group> = emptyList(),
        val currentGroupName: String? = null,
        val isLoading: Boolean = true
)

class TransactionDetailViewModel(
        private val transactionId: String,
        private val observeTransactions: ObserveTransactionsUseCase,
        private val observeGroups: ObserveGroupsUseCase,
        private val classifyTransaction: ClassifyTransactionUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<TransactionDetailUiState> =
            combine(observeTransactions(), observeGroups(), _isLoading) {
                            transactions,
                            groups,
                            currentLoading ->
                        val tx = transactions.find { it.id == transactionId }
                        val currentGroup = groups.find { it.id == tx?.groupId }

                        TransactionDetailUiState(
                                transaction = tx,
                                allGroups = groups,
                                currentGroupName = currentGroup?.name,
                                isLoading = currentLoading
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = TransactionDetailUiState()
                    )

    fun updateGroup(newGroupId: String) {
        viewModelScope.launch {
            val tx = uiState.value.transaction ?: return@launch
            classifyTransaction(tx.id, newGroupId, tx.entityId)
        }
    }
}
