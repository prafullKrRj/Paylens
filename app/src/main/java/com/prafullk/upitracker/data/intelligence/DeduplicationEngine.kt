package com.prafullk.upitracker.data.intelligence

import com.prafullk.upitracker.data.db.dao.AccessibilityLogDao
import com.prafullk.upitracker.data.db.dao.SmsLogDao
import com.prafullk.upitracker.data.db.entities.AccessibilityLogEntity
import java.security.MessageDigest

class DeduplicationEngine(
        private val accessibilityLogDao: AccessibilityLogDao,
        private val smsLogDao: SmsLogDao
) {
    // Time window: 2 transactions from same app with same amount within 90 seconds = duplicate
    private val DEDUP_WINDOW_MS = 90_000L

    fun fingerprint(amount: Double, sourceApp: String): String {
        val windowedTimestamp = System.currentTimeMillis() / DEDUP_WINDOW_MS
        val raw = "$amount|$sourceApp|$windowedTimestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun isDuplicate(fingerprint: String): Boolean {
        return accessibilityLogDao.existsByFingerprint(fingerprint)
    }

    suspend fun markProcessed(fingerprint: String, transactionId: String) {
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
