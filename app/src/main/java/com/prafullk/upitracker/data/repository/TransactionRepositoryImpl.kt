package com.prafullk.upitracker.data.repository

import com.prafullk.upitracker.data.db.dao.GroupSpendingTuple
import com.prafullk.upitracker.data.db.dao.TransactionDao
import com.prafullk.upitracker.domain.model.Transaction
import com.prafullk.upitracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(private val dao: TransactionDao) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> =
            dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByGroup(groupId: String): Flow<List<Transaction>> =
            dao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun observeByEntity(entityId: String): Flow<List<Transaction>> =
            dao.observeByEntity(entityId).map { list -> list.map { it.toDomain() } }

    override fun observeInRange(from: Long, to: Long): Flow<List<Transaction>> =
            dao.observeInRange(from, to).map { list -> list.map { it.toDomain() } }

    override fun totalDebitsInRange(from: Long, to: Long): Flow<Double?> =
            dao.totalDebitsInRange(from, to)

    override fun spendingByGroup(from: Long, to: Long): Flow<List<GroupSpendingTuple>> =
            dao.spendingByGroup(from, to)

    override suspend fun save(transaction: Transaction) {
        dao.upsert(transaction.toEntity())
    }

    override suspend fun updateClassification(
            id: String,
            groupId: String?,
            entityId: String?,
            userClassified: Boolean
    ) {
        dao.updateClassification(id, groupId, entityId, userClassified)
    }
}
