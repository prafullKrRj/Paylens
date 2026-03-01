package com.prafullk.upitracker.data.intelligence

import com.prafullk.upitracker.data.db.dao.TrackedEntityDao
import com.prafullk.upitracker.data.db.entities.TrackedEntityEntity
import com.prafullk.upitracker.domain.model.RawTransactionData

class EntityMatcher(private val entityDao: TrackedEntityDao) {

    suspend fun findBestMatch(raw: RawTransactionData): TrackedEntityEntity? {
        // Strategy 1: Exact UPI VPA match (highest confidence)
        raw.upiId?.let { vpa ->
            entityDao.findByUpiId(vpa)?.let {
                return it
            }
        }

        // Strategy 2: Name similarity
        raw.contactName?.let { name ->
            val candidates = entityDao.findByName(name)
            if (candidates.isNotEmpty()) return candidates.first()

            // Fuzzy: check each word token of contactName
            name.split(" ").forEach { token ->
                if (token.length > 2) {
                    entityDao.findByName(token).firstOrNull()?.let {
                        return it
                    }
                }
            }
        }

        return null
    }
}
