package com.prafullk.upitracker.presentation.screens.classify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.entity.ObserveEntitiesUseCase
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ClassifyTransactionUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClassifyUiState(
        val unclassifiedTransactions: List<Transaction> = emptyList(),
        val allGroups: List<Group> = emptyList(),
        val currentTransactionIndex: Int = 0,
        val isLoading: Boolean = true
)

class ClassifyViewModel(
        private val observeTransactions: ObserveTransactionsUseCase,
        private val observeGroups: ObserveGroupsUseCase,
        private val observeEntities: ObserveEntitiesUseCase,
        private val classifyTransaction: ClassifyTransactionUseCase
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)

    val uiState: StateFlow<ClassifyUiState> =
            combine(observeTransactions(), observeGroups(), _currentIndex) {
                            transactions,
                            groups,
                            index ->
                        val unclassified =
                                transactions.filter {
                                    it.groupId == "sys_uncategorized" && !it.isUserClassified
                                }

                        ClassifyUiState(
                                unclassifiedTransactions = unclassified,
                                allGroups = groups,
                                currentTransactionIndex =
                                        index.coerceAtMost(unclassified.size - 1).coerceAtLeast(0),
                                isLoading = false
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = ClassifyUiState()
                    )

    fun skipCurrent() {
        _currentIndex.value += 1
    }

    fun classifyCurrent(groupId: String) {
        viewModelScope.launch {
            val transactions = uiState.value.unclassifiedTransactions
            val index = uiState.value.currentTransactionIndex
            if (index in transactions.indices) {
                val tx = transactions[index]
                classifyTransaction(tx.id, groupId, tx.entityId)
            }
        }
    }
}
