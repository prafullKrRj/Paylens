package com.prafullk.upitracker.domain.usecase.transaction

import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveTransactionsUseCase(private val transactionRepo: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepo.observeAll()
    }

    fun byGroup(groupId: String): Flow<List<Transaction>> {
        return transactionRepo.observeByGroup(groupId)
    }

    fun byEntity(entityId: String): Flow<List<Transaction>> {
        return transactionRepo.observeByEntity(entityId)
    }
}
