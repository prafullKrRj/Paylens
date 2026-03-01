package com.prafullk.upitracker.domain.usecase.transaction

import com.prafullk.upitracker.domain.repository.TransactionRepository

class GetSpendingAnalyticsUseCase(private val transactionRepo: TransactionRepository) {
    // Stub for Phase 4. Will implement full analytics engine in Phase 7
    suspend operator fun invoke() {
        // Analytics computation Logic goes here
    }
}
