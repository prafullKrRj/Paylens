package com.prafullk.upitracker.data.detection.upi

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.prafullk.upitracker.data.db.dao.UpiAppDao
import com.prafullk.upitracker.data.db.entities.UpiAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runtime model for a discovered UPI app (icon not persisted to DB). */
data class DiscoveredUpiApp(
        val packageName: String,
        val displayName: String,
        val icon: Drawable,
        val isActive: Boolean = true
)

/** Master list of known UPI package names to cross-reference against. */
val KNOWN_UPI_PACKAGES: Map<String, String> = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "net.one97.paytm" to "Paytm",
        "com.phonepe.app" to "PhonePe",
        "in.org.npci.upiapp" to "BHIM",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.freecharge.android" to "FreeCharge",
        "com.mobikwik_new" to "MobiKwik",
        "com.snapwork.hdfc" to "HDFC PayZapp",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.sbi.lotusintouch" to "SBI YONO",
        "com.axis.mobile" to "Axis Mobile",
        "com.kotak.mahindra.kotak811" to "Kotak811",
        "com.dbs.ind.digibank" to "DBS Digibank",
        "com.rbl.rblmobilebanking" to "RBL Mobi",
        "com.indusind.mobile" to "IndusInd",
        "com.jupiter" to "Jupiter",
        "com.niyo.goals" to "Niyo",
        "com.slice.app" to "Slice",
        "com.cred.club" to "CRED",
        "com.dreamplug.androidapp" to "CRED",
        "in.juspay.hyperpay" to "JusPay",
        "com.lcode.bhimaxis" to "Axis BHIM",
        "com.whatsapp" to "WhatsApp Pay"
)

/**
 * Discovers installed UPI apps by querying PackageManager and cross-referencing
 * against the known master list. Caches icons in-memory via LruCache.
 */
class UpiAppDiscoveryService(
        private val context: Context,
        private val upiAppDao: UpiAppDao
) {
    // LRU icon cache keyed by package name — max 50 entries
    private val iconCache = LruCache<String, Drawable>(50)

    /** Run discovery and persist results to DB. Returns discovered apps with live icons. */
    suspend fun discoverAndSync(): List<DiscoveredUpiApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val existing = upiAppDao.getActivePackageNames().associateBy { it.packageName }
        val discovered = mutableListOf<DiscoveredUpiApp>()
        val toUpsert = mutableListOf<UpiAppEntity>()

        for ((pkg, fallbackName) in KNOWN_UPI_PACKAGES) {
            val appInfo = runCatching {
                pm.getApplicationInfo(pkg, 0)
            }.getOrNull() ?: continue

            val label = pm.getApplicationLabel(appInfo).toString().ifBlank { fallbackName }
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull() ?: continue

            // Cache icon in memory
            iconCache.put(pkg, icon)

            val isActive = existing[pkg]?.isActive ?: true
            discovered.add(DiscoveredUpiApp(pkg, label, icon, isActive))

            toUpsert.add(
                    UpiAppEntity(
                            packageName = pkg,
                            displayName = label,
                            isActive = isActive,
                            discoveredAt = existing[pkg]?.discoveredAt ?: System.currentTimeMillis(),
                            lastTransactionAt = existing[pkg]?.lastTransactionAt
                    )
            )
        }

        upiAppDao.upsertAll(toUpsert)
        discovered
    }

    /** Get icon from cache (fast path — does NOT query PackageManager). */
    fun getCachedIcon(packageName: String): Drawable? = iconCache.get(packageName)

    /** Get icon, falling back to PackageManager if not cached. */
    fun getIcon(packageName: String): Drawable? {
        iconCache.get(packageName)?.let { return it }
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationIcon(appInfo).also { iconCache.put(packageName, it) }
        }.getOrNull()
    }

    /** Load active package names from DB for AccessibilityService filtering. */
    suspend fun loadActivePackageNames(): List<String> = withContext(Dispatchers.IO) {
        upiAppDao.getActivePackageNames().map { it.packageName }
    }

    /** Toggle tracking for a specific app and update the DB. */
    suspend fun setAppActive(packageName: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        upiAppDao.setActive(packageName, isActive)
    }

    /** Record a transaction event for an app (updates lastTransactionAt). */
    suspend fun recordTransaction(packageName: String) = withContext(Dispatchers.IO) {
        upiAppDao.updateLastTransaction(packageName, System.currentTimeMillis())
    }
}
