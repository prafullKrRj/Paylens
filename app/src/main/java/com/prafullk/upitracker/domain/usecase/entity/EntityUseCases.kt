package com.prafullk.upitracker.domain.usecase.entity

import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.domain.repository.EntityRepository
import kotlinx.coroutines.flow.Flow

class ObserveEntitiesUseCase(private val entityRepo: EntityRepository) {
    operator fun invoke(): Flow<List<TrackedEntity>> {
        return entityRepo.observeAll()
    }
}

class CreateEntityUseCase(private val entityRepo: EntityRepository) {
    suspend operator fun invoke(entity: TrackedEntity) {
        entityRepo.save(entity)
    }
}

class MergeEntitiesUseCase(private val entityRepo: EntityRepository) {
    suspend operator fun invoke(primaryId: String, secondaryId: String) {
        // Real implementation would gather secondary's vpAs/aliases to primary,
        // upvdate txns pointing to secondary -> primary, then delete secondary.
    }
}
