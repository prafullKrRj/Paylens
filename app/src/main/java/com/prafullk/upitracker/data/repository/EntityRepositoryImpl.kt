package com.prafullk.upitracker.data.repository

import com.prafullk.upitracker.data.db.dao.TrackedEntityDao
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.repository.EntityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntityRepositoryImpl(private val dao: TrackedEntityDao) : EntityRepository {
    override fun observeAll(): Flow<List<TrackedEntity>> =
            dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): TrackedEntity? = dao.getById(id)?.toDomain()

    override suspend fun findByUpiId(upiId: String): TrackedEntity? =
            dao.findByUpiId(upiId)?.toDomain()

    override suspend fun findByName(name: String): List<TrackedEntity> =
            dao.findByName(name).map { it.toDomain() }

    override suspend fun save(entity: TrackedEntity) {
        dao.upsert(entity.toEntity())
    }

    override suspend fun updateLastSeen(id: String, ts: Long) {
        dao.updateLastSeen(id, ts)
    }
}
