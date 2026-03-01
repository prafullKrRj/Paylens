package com.prafullk.upitracker.domain.usecase.transaction

import com.prafullk.upitracker.data.intelligence.DeduplicationEngine
import com.prafullk.upitracker.data.intelligence.TransactionClassifier
import com.prafullk.upitracker.domain.model.RawTransactionData
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.model.TransactionStatus
import com.prafullk.upitracker.domain.repository.EntityRepository
import com.prafullk.upitracker.domain.repository.TransactionRepository
import java.util.UUID

class LogTransactionUseCase(
        private val dedup: DeduplicationEngine,
        private val classifier: TransactionClassifier,
        private val transactionRepo: TransactionRepository,
        private val entityRepo: EntityRepository
) {
    suspend operator fun invoke(raw: RawTransactionData) {
        // 1. Deduplication check
        if (dedup.isDuplicate(raw.fingerprint)) return

        // 2. Classify
        val classification = classifier.classify(raw)

        // 3. Build entity
        val transaction =
                Transaction(
                        id = UUID.randomUUID().toString(),
                        amount = raw.amount,
                        timestamp = raw.timestamp,
                        upiId = raw.upiId,
                        contactName = raw.contactName,
                        rawDescription = raw.rawText,
                        source = raw.source,
                        sourceApp = raw.sourceApp,
                        direction = raw.direction,
                        status = TransactionStatus.SUCCESS,
                        groupId = classification.groupId,
                        entityId = classification.entityId,
                        isUserClassified = false,
                        createdAt = System.currentTimeMillis()
                )

        // 4. Persist
        transactionRepo.save(transaction)

        // 5. Update entity lastTransactionAt
        classification.entityId?.let { entityRepo.updateLastSeen(it, raw.timestamp) }

        // 6. Mark dedup
        dedup.markProcessed(raw.fingerprint, transaction.id)
    }
}
