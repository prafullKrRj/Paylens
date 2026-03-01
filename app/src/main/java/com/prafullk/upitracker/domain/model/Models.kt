package com.prafullk.upitracker.domain.model

data class Transaction(
        val id: String,
        val amount: Double,
        val timestamp: Long,
        val upiId: String?,
        val contactName: String?,
        val rawDescription: String,
        val source: String,
        val sourceApp: String?,
        val direction: String,
        val status: String,
        val groupId: String?,
        val entityId: String?,
        val isUserClassified: Boolean = false,
        val note: String? = null,
        val createdAt: Long
)

data class TrackedEntity(
        val id: String,
        val displayName: String,
        val type: String,
        val upiIds: List<String>,
        val phoneNumbers: List<String>,
        val aliases: List<String>,
        val defaultGroupId: String?,
        val avatarColor: Int,
        val isUserCreated: Boolean = true,
        val createdAt: Long,
        val lastTransactionAt: Long? = null
)

data class Group(
        val id: String,
        val name: String,
        val icon: String,
        val color: Int,
        val type: String,
        val isSystem: Boolean = false,
        val sortOrder: Int = 0,
        val monthlyBudget: Double? = null
)

data class RawTransactionData(
        val amount: Double,
        val upiId: String?,
        val contactName: String?,
        val rawText: String,
        val source: String,
        val sourceApp: String?,
        val direction: String,
        val fingerprint: String,
        val timestamp: Long
)

data class ClassificationResult(
        val entityId: String?,
        val groupId: String?,
        val confidence: Confidence
)

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW
}

object DetectionSource {
    const val ACCESSIBILITY = "ACCESSIBILITY"
    const val NOTIFICATION = "NOTIFICATION"
    const val SMS = "SMS"
}

object TransactionDirection {
    const val DEBIT = "DEBIT"
    const val CREDIT = "CREDIT"
}

object TransactionStatus {
    const val SUCCESS = "SUCCESS"
    const val PENDING = "PENDING"
    const val FAILED = "FAILED"
}

object EntityType {
    const val PERSON = "PERSON"
    const val MERCHANT = "MERCHANT"
    const val SERVICE = "SERVICE"
    const val UNKNOWN = "UNKNOWN"
}
