package com.prafullk.upitracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
        @PrimaryKey val id: String,
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
        val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracked_entities")
data class TrackedEntityEntity(
        @PrimaryKey val id: String,
        val displayName: String,
        val type: String,
        val upiIds: String,
        val phoneNumbers: String,
        val aliases: String,
        val defaultGroupId: String?,
        val avatarColor: Int,
        val isUserCreated: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
        val lastTransactionAt: Long? = null
)

@Entity(tableName = "groups")
data class GroupEntity(
        @PrimaryKey val id: String,
        val name: String,
        val icon: String,
        val color: Int,
        val type: String,
        val isSystem: Boolean = false,
        val sortOrder: Int = 0,
        val monthlyBudget: Double? = null
)

@Entity(tableName = "sms_log")
data class SmsLogEntity(
        @PrimaryKey val smsId: Long,
        val processedAt: Long,
        val transactionId: String?
)

@Entity(tableName = "accessibility_log")
data class AccessibilityLogEntity(
        @PrimaryKey val fingerprint: String,
        val capturedAt: Long,
        val transactionId: String?
)
