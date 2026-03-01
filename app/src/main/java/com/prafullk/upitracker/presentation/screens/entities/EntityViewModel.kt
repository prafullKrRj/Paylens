package com.prafullk.upitracker.presentation.screens.entities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.usecase.entity.ObserveEntitiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EntityListUiState(
        val people: List<TrackedEntity> = emptyList(),
        val merchants: List<TrackedEntity> = emptyList(),
        val isLoading: Boolean = true
)

class EntityViewModel(private val observeEntities: ObserveEntitiesUseCase) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<EntityListUiState> =
            combine(observeEntities(), _isLoading) { entities, currentLoading ->
                        val sorted = entities.sortedByDescending { it.lastTransactionAt ?: 0 }

                        EntityListUiState(
                                people = sorted.filter { it.type == "PERSON" },
                                merchants = sorted.filter { it.type != "PERSON" },
                                isLoading = currentLoading
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = EntityListUiState()
                    )
}
