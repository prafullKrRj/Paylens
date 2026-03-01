package com.prafullk.upitracker.data.repository

import com.prafullk.upitracker.data.db.dao.GroupDao
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl(private val dao: GroupDao) : GroupRepository {
    override fun observeAll(): Flow<List<Group>> =
            dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(group: Group) {
        dao.upsert(group.toEntity())
    }

    override suspend fun insertAll(groups: List<Group>) {
        dao.insertAll(groups.map { it.toEntity() })
    }
}
