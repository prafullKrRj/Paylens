package com.prafullk.upitracker.data.intelligence

import com.prafullk.upitracker.data.db.dao.AccessibilityLogDao
import com.prafullk.upitracker.data.db.dao.SmsLogDao
import com.prafullk.upitracker.data.db.entities.AccessibilityLogEntity
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class DeduplicationEngine(
        private val accessibilityLogDao: AccessibilityLogDao,
        private val smsLogDao: SmsLogDao
) {
    // In-memory cache to prevent race conditions from back-to-back accessibility events
    private val recentlyProcessed = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun fingerprint(amount: Double, sourceApp: String, timestamp: Long): String {
        val raw = "$amount|$sourceApp|$timestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun isDuplicate(fingerprint: String): Boolean {
        if (recentlyProcessed.contains(fingerprint)) return true
        return accessibilityLogDao.existsByFingerprint(fingerprint)
    }

    suspend fun markProcessed(fingerprint: String, transactionId: String) {
        recentlyProcessed.add(fingerprint)
        accessibilityLogDao.insert(
                AccessibilityLogEntity(
                        fingerprint = fingerprint,
                        capturedAt = System.currentTimeMillis(),
                        transactionId = transactionId
                )
        )
    }

    // Nightly cleanup job (via WorkManager)
    suspend fun purgeOldEntries() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        accessibilityLogDao.deleteOlderThan(cutoff)
    }
}
