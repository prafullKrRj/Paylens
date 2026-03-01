package com.prafullk.upitracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.prafullk.upitracker.data.db.entities.AccessibilityLogEntity
import com.prafullk.upitracker.data.db.entities.GroupEntity
import com.prafullk.upitracker.data.db.entities.SmsLogEntity
import com.prafullk.upitracker.data.db.entities.TrackedEntityEntity
import com.prafullk.upitracker.data.db.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class GroupSpendingTuple(val groupId: String?, val total: Double)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun observeByGroup(groupId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE entityId = :entityId ORDER BY timestamp DESC")
    fun observeByEntity(entityId: String): Flow<List<TransactionEntity>>

    @Query(
            """
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :from AND :to 
        ORDER BY timestamp DESC
    """
    )
    fun observeInRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query(
            "SELECT SUM(amount) FROM transactions WHERE direction='DEBIT' AND timestamp BETWEEN :from AND :to"
    )
    fun totalDebitsInRange(from: Long, to: Long): Flow<Double?>

    @Query(
            "SELECT groupId, SUM(amount) as total FROM transactions WHERE direction='DEBIT' AND timestamp BETWEEN :from AND :to GROUP BY groupId"
    )
    fun spendingByGroup(from: Long, to: Long): Flow<List<GroupSpendingTuple>>

    @Upsert suspend fun upsert(transaction: TransactionEntity)

    @Query(
            "UPDATE transactions SET groupId = :groupId, entityId = :entityId, isUserClassified = :userClassified WHERE id = :id"
    )
    suspend fun updateClassification(
            id: String,
            groupId: String?,
            entityId: String?,
            userClassified: Boolean
    )
}

@Dao
interface TrackedEntityDao {
    @Query("SELECT * FROM tracked_entities ORDER BY lastTransactionAt DESC")
    fun observeAll(): Flow<List<TrackedEntityEntity>>

    @Query("SELECT * FROM tracked_entities WHERE id = :id")
    suspend fun getById(id: String): TrackedEntityEntity?

    @Query("SELECT * FROM tracked_entities WHERE upiIds LIKE '%' || :upiId || '%'")
    suspend fun findByUpiId(upiId: String): TrackedEntityEntity?

    @Query(
            "SELECT * FROM tracked_entities WHERE displayName LIKE '%' || :name || '%' OR aliases LIKE '%' || :name || '%'"
    )
    suspend fun findByName(name: String): List<TrackedEntityEntity>

    @Upsert suspend fun upsert(entity: TrackedEntityEntity)

    @Query("UPDATE tracked_entities SET lastTransactionAt = :ts WHERE id = :id")
    suspend fun updateLastSeen(id: String, ts: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY sortOrder ASC") fun observeAll(): Flow<List<GroupEntity>>

    @Upsert suspend fun upsert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(groups: List<GroupEntity>)
}

@Dao
interface SmsLogDao {
    @Upsert suspend fun upsert(log: SmsLogEntity)
}

@Dao
interface AccessibilityLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(log: AccessibilityLogEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM accessibility_log WHERE fingerprint = :fingerprint)")
    suspend fun existsByFingerprint(fingerprint: String): Boolean

    @Query("DELETE FROM accessibility_log WHERE capturedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
