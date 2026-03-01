package com.prafullk.upitracker.domain.usecase.transaction

import com.prafullk.upitracker.domain.repository.TransactionRepository

class ClassifyTransactionUseCase(private val transactionRepo: TransactionRepository) {
    suspend operator fun invoke(transactionId: String, groupId: String?, entityId: String?) {
        transactionRepo.updateClassification(
                transactionId,
                groupId,
                entityId,
                userClassified = true
        )
    }
}
