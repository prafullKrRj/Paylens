package com.prafullk.upitracker.data.detection.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.prafullk.upitracker.data.intelligence.DeduplicationEngine
import com.prafullk.upitracker.domain.model.DetectionSource
import com.prafullk.upitracker.domain.model.RawTransactionData
import com.prafullk.upitracker.domain.model.TransactionDirection
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PayLensNotificationListener : NotificationListenerService() {

    private val logTransaction by inject<LogTransactionUseCase>()
    private val dedup by inject<DeduplicationEngine>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val WATCHED_PACKAGES =
            setOf(
                    "com.google.android.apps.nbu.paisa.user",
                    "net.one97.paytm",
                    "com.phonepe.app",
                    "in.org.npci.upiapp",
                    "com.amazon.mShop.android.shopping",
                    "com.freecharge.android",
                    "com.mobikwik_new"
            )

    // Similar to NodeTreeParser
    private val successKeywords =
            setOf(
                    "payment successful",
                    "paid",
                    "money sent",
                    "sent successfully",
                    "payment done",
                    "transaction successful",
                    "debit",
                    "debited"
            )
    private val amountRegex = Regex("""[₹Rs.]+\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
    private val upiVpaRegex = Regex("""[\w.\-+]+@[\w]+""")
    private val paidToRegex =
            Regex("""(?:paid to|sent to|to)\s+([A-Z][a-zA-Z\s]+)""", RegexOption.IGNORE_CASE)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!WATCHED_PACKAGES.contains(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""

        val combinedText = "$title $text $bigText"
        scope.launch { parseNotificationText(combinedText, pkg)?.let { logTransaction(it) } }
    }

    private suspend fun parseNotificationText(text: String, pkg: String): RawTransactionData? {
        val lowerText = text.lowercase()

        if (!successKeywords.any { lowerText.contains(it) }) return null

        val amount =
                amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                        ?: return null
        val upiId = upiVpaRegex.find(text)?.value
        val contactName =
                paidToRegex.find(text)?.groupValues?.get(1)?.trim()
                        ?: upiId?.substringBefore("@")?.replace(Regex("[^a-zA-Z]"), " ")?.trim()

        val fingerprint = dedup.fingerprint(amount, pkg)
        if (dedup.isDuplicate(fingerprint)) return null

        return RawTransactionData(
                amount = amount,
                upiId = upiId,
                contactName = contactName,
                rawText = text,
                source = DetectionSource.NOTIFICATION,
                sourceApp = pkg,
                direction =
                        TransactionDirection.DEBIT, // Simplified logic for phase 3, expands later
                fingerprint = fingerprint,
                timestamp = System.currentTimeMillis()
        )
    }
}
