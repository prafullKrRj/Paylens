package com.prafullk.upitracker.data.repository

import com.prafullk.upitracker.data.db.entities.GroupEntity
import com.prafullk.upitracker.data.db.entities.TrackedEntityEntity
import com.prafullk.upitracker.data.db.entities.TransactionEntity
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.model.Transaction
import org.json.JSONArray

fun String.toJsonList(): List<String> {
    if (this.isBlank()) return emptyList()
    val list = mutableListOf<String>()
    try {
        val array = JSONArray(this)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
    } catch (e: Exception) {
        list.add(this) // Fallback for simple comma-separated or plain text
    }
    return list
}

fun List<String>.toJsonString(): String {
    val array = JSONArray()
    this.forEach { array.put(it) }
    return array.toString()
}

fun TransactionEntity.toDomain(): Transaction =
        Transaction(
                id = id,
                amount = amount,
                timestamp = timestamp,
                upiId = upiId,
                contactName = contactName,
                rawDescription = rawDescription,
                source = source,
                sourceApp = sourceApp,
                direction = direction,
                status = status,
                groupId = groupId,
                entityId = entityId,
                isUserClassified = isUserClassified,
                note = note,
                createdAt = createdAt
        )

fun Transaction.toEntity(): TransactionEntity =
        TransactionEntity(
                id = id,
                amount = amount,
                timestamp = timestamp,
                upiId = upiId,
                contactName = contactName,
                rawDescription = rawDescription,
                source = source,
                sourceApp = sourceApp,
                direction = direction,
                status = status,
                groupId = groupId,
                entityId = entityId,
                isUserClassified = isUserClassified,
                note = note,
                createdAt = createdAt
        )

fun TrackedEntityEntity.toDomain(): TrackedEntity =
        TrackedEntity(
                id = id,
                displayName = displayName,
                type = type,
                upiIds = upiIds.toJsonList(),
                phoneNumbers = phoneNumbers.toJsonList(),
                aliases = aliases.toJsonList(),
                defaultGroupId = defaultGroupId,
                avatarColor = avatarColor,
                isUserCreated = isUserCreated,
                createdAt = createdAt,
                lastTransactionAt = lastTransactionAt
        )

fun TrackedEntity.toEntity(): TrackedEntityEntity =
        TrackedEntityEntity(
                id = id,
                displayName = displayName,
                type = type,
                upiIds = upiIds.toJsonString(),
                phoneNumbers = phoneNumbers.toJsonString(),
                aliases = aliases.toJsonString(),
                defaultGroupId = defaultGroupId,
                avatarColor = avatarColor,
                isUserCreated = isUserCreated,
                createdAt = createdAt,
                lastTransactionAt = lastTransactionAt
        )

fun GroupEntity.toDomain(): Group =
        Group(
                id = id,
                name = name,
                icon = icon,
                color = color,
                type = type,
                isSystem = isSystem,
                sortOrder = sortOrder,
                monthlyBudget = monthlyBudget
        )

fun Group.toEntity(): GroupEntity =
        GroupEntity(
                id = id,
                name = name,
                icon = icon,
                color = color,
                type = type,
                isSystem = isSystem,
                sortOrder = sortOrder,
                monthlyBudget = monthlyBudget
        )
