package com.prafullk.upitracker.data.intelligence

import com.prafullk.upitracker.data.db.dao.GroupDao
import com.prafullk.upitracker.domain.model.ClassificationResult
import com.prafullk.upitracker.domain.model.Confidence
import com.prafullk.upitracker.domain.model.RawTransactionData

class TransactionClassifier(
        private val entityMatcher: EntityMatcher,
        private val groupDao: GroupDao
) {
    suspend fun classify(raw: RawTransactionData): ClassificationResult {
        val entity = entityMatcher.findBestMatch(raw)

        // If entity found AND has a defaultGroupId → use it
        val groupId =
                entity?.defaultGroupId ?: inferGroupFromText(raw.rawText) ?: "sys_uncategorized"

        return ClassificationResult(
                entityId = entity?.id,
                groupId = groupId,
                confidence = if (entity != null) Confidence.HIGH else Confidence.LOW
        )
    }

    // Simple keyword-based group inference when no entity match
    private fun inferGroupFromText(text: String): String? {
        val lower = text.lowercase()
        return when {
            swiggyZomatoKeywords.any { lower.contains(it) } -> "sys_food"
            transportKeywords.any { lower.contains(it) } -> "sys_transport"
            entertainmentKeywords.any { lower.contains(it) } -> "sys_entertainment"
            utilityKeywords.any { lower.contains(it) } -> "sys_utilities"
            else -> null
        }
    }

    private val swiggyZomatoKeywords = setOf("swiggy", "zomato", "blinkit", "restaurant", "cafe")
    private val transportKeywords = setOf("uber", "ola", "rapido", "metro", "irctc", "fuel")
    private val entertainmentKeywords = setOf("netflix", "spotify", "bookmyshow", "prime")
    private val utilityKeywords = setOf("electricity", "jio", "airtel", "broadband", "gas")
}
