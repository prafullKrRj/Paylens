package com.prafullk.upitracker.presentation.screens.entities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.usecase.entity.ObserveEntitiesUseCase
import com.prafullk.upitracker.domain.usecase.group.ObserveGroupsUseCase
import com.prafullk.upitracker.domain.usecase.transaction.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EntityDetailUiState(
        val entity: TrackedEntity? = null,
        val transactions: List<Transaction> = emptyList(),
        val defaultGroup: Group? = null,
        val isLoading: Boolean = true
)

class EntityDetailViewModel(
        private val entityId: String,
        private val observeEntities: ObserveEntitiesUseCase,
        private val observeTransactions: ObserveTransactionsUseCase,
        private val observeGroups: ObserveGroupsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<EntityDetailUiState> =
            combine(observeEntities(), observeTransactions(), observeGroups(), _isLoading) {
                            entities,
                            transactions,
                            groups,
                            isLoad ->
                        val entity = entities.find { it.id == entityId }
                        val entityTransactions = transactions.filter { it.entityId == entityId }
                        val defaultGroup = groups.find { it.id == entity?.defaultGroupId }

                        EntityDetailUiState(
                                entity = entity,
                                transactions = entityTransactions,
                                defaultGroup = defaultGroup,
                                isLoading = isLoad
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = EntityDetailUiState()
                    )
}
