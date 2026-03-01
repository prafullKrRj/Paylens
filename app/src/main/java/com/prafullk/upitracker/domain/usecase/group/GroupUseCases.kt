package com.prafullk.upitracker.domain.usecase.group

import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupsUseCase(private val groupRepo: GroupRepository) {
    operator fun invoke(): Flow<List<Group>> {
        return groupRepo.observeAll()
    }
}

class ManageGroupUseCase(private val groupRepo: GroupRepository) {
    suspend operator fun invoke(group: Group) {
        groupRepo.save(group)
    }

    suspend fun updateBudget(groupId: String, budget: Double?) {
        // Fetch group, map to domain, and save budget
    }
}
