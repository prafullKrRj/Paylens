package com.prafullk.upitracker.data.db

import com.prafullk.upitracker.data.db.dao.GroupDao
import com.prafullk.upitracker.data.db.entities.GroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {

    private val systemGroups =
            listOf(
                    GroupEntity(
                            "sys_food",
                            "Food & Dining",
                            "restaurant",
                            0xFFE57373.toInt(),
                            "EXPENSE",
                            true,
                            0
                    ),
                    GroupEntity(
                            "sys_transport",
                            "Transport",
                            "directions_car",
                            0xFF64B5F6.toInt(),
                            "EXPENSE",
                            true,
                            1
                    ),
                    GroupEntity(
                            "sys_shopping",
                            "Shopping",
                            "shopping_bag",
                            0xFFFFB74D.toInt(),
                            "EXPENSE",
                            true,
                            2
                    ),
                    GroupEntity(
                            "sys_entertainment",
                            "Entertainment",
                            "movie",
                            0xFF9575CD.toInt(),
                            "EXPENSE",
                            true,
                            3
                    ),
                    GroupEntity(
                            "sys_health",
                            "Health",
                            "medical_services",
                            0xFF4DB6AC.toInt(),
                            "EXPENSE",
                            true,
                            4
                    ),
                    GroupEntity(
                            "sys_utilities",
                            "Bills & Utilities",
                            "receipt",
                            0xFFFF8A65.toInt(),
                            "EXPENSE",
                            true,
                            5
                    ),
                    GroupEntity(
                            "sys_education",
                            "Education",
                            "school",
                            0xFF4DD0E1.toInt(),
                            "EXPENSE",
                            true,
                            6
                    ),
                    GroupEntity(
                            "sys_friends",
                            "Friends & Family",
                            "people",
                            0xFFF06292.toInt(),
                            "EXPENSE",
                            true,
                            7
                    ),
                    GroupEntity(
                            "sys_income",
                            "Income",
                            "payments",
                            0xFF81C784.toInt(),
                            "INCOME",
                            true,
                            8
                    ),
                    GroupEntity(
                            "sys_transfer",
                            "Self Transfer",
                            "swap_horiz",
                            0xFFB0BEC5.toInt(),
                            "TRANSFER",
                            true,
                            9
                    ),
                    GroupEntity(
                            "sys_uncategorized",
                            "Uncategorized",
                            "help_outline",
                            0xFFE0E0E0.toInt(),
                            "EXPENSE",
                            true,
                            10
                    )
            )

    fun seedDatabase(groupDao: GroupDao) {
        CoroutineScope(Dispatchers.IO).launch { groupDao.insertAll(systemGroups) }
    }
}
