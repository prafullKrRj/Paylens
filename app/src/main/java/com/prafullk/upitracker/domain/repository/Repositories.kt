package com.prafullk.upitracker.domain.repository

import com.prafullk.upitracker.data.db.dao.GroupSpendingTuple
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByGroup(groupId: String): Flow<List<Transaction>>
    fun observeByEntity(entityId: String): Flow<List<Transaction>>
    fun observeInRange(from: Long, to: Long): Flow<List<Transaction>>
    fun totalDebitsInRange(from: Long, to: Long): Flow<Double?>
    fun spendingByGroup(from: Long, to: Long): Flow<List<GroupSpendingTuple>>

    suspend fun save(transaction: Transaction)
    suspend fun updateClassification(
            id: String,
            groupId: String?,
            entityId: String?,
            userClassified: Boolean
    )
}

interface EntityRepository {
    fun observeAll(): Flow<List<TrackedEntity>>
    suspend fun getById(id: String): TrackedEntity?
    suspend fun findByUpiId(upiId: String): TrackedEntity?
    suspend fun findByName(name: String): List<TrackedEntity>

    suspend fun save(entity: TrackedEntity)
    suspend fun updateLastSeen(id: String, ts: Long)
}

interface GroupRepository {
    fun observeAll(): Flow<List<Group>>
    suspend fun save(group: Group)
    suspend fun insertAll(groups: List<Group>)
}
