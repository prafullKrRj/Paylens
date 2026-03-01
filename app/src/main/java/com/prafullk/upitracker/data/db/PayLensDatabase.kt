package com.prafullk.upitracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prafullk.upitracker.data.db.dao.AccessibilityLogDao
import com.prafullk.upitracker.data.db.dao.GroupDao
import com.prafullk.upitracker.data.db.dao.SmsLogDao
import com.prafullk.upitracker.data.db.dao.TrackedEntityDao
import com.prafullk.upitracker.data.db.dao.TransactionDao
import com.prafullk.upitracker.data.db.entities.AccessibilityLogEntity
import com.prafullk.upitracker.data.db.entities.GroupEntity
import com.prafullk.upitracker.data.db.entities.SmsLogEntity
import com.prafullk.upitracker.data.db.entities.TrackedEntityEntity
import com.prafullk.upitracker.data.db.entities.TransactionEntity

@Database(
        entities =
                [
                        TransactionEntity::class,
                        TrackedEntityEntity::class,
                        GroupEntity::class,
                        SmsLogEntity::class,
                        AccessibilityLogEntity::class],
        version = 1,
        exportSchema = false
)
abstract class PayLensDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun trackedEntityDao(): TrackedEntityDao
    abstract fun groupDao(): GroupDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun accessibilityLogDao(): AccessibilityLogDao
}
