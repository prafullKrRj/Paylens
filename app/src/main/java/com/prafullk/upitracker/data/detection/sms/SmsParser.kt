package com.prafullk.upitracker.data.detection.sms

import android.telephony.SmsMessage
import com.prafullk.upitracker.domain.model.DetectionSource
import com.prafullk.upitracker.domain.model.RawTransactionData
import com.prafullk.upitracker.domain.model.TransactionDirection
import java.security.MessageDigest

object SmsParser {
    // HDFC: "Rs.500.00 debited from ac XX1234 on 01-06-25 to VPA prafull@upi"
    // SBI:  "INR 500.00 debited from SB A/C XXXXXX1234. UPI Ref No 123456789"
    // ICICI:"Dear Customer, INR 500.00 has been debited..."

    private val patterns =
            listOf(
                    Regex(
                            """(?:Rs\.|INR|₹)\s*(\d+(?:\.\d{2})?)\s+debited.*?(?:to VPA\s+([\w.@]+))?""",
                            RegexOption.IGNORE_CASE
                    ),
                    Regex(
                            """debited.*?(?:Rs\.|INR|₹)\s*(\d+(?:\.\d{2})?)""",
                            RegexOption.IGNORE_CASE
                    )
            )

    fun parse(sms: SmsMessage): RawTransactionData? {
        val body = sms.messageBody ?: return null
        val lowerBody = body.lowercase()

        // Very rough heuristic for Phase 3 checks
        if (!lowerBody.contains("debited")) return null

        var amount: Double? = null
        var upiId: String? = null

        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                // Determine which group is amount based on regex pattern specifics
                // If there are 2 groups extracted
                if (match.groupValues.size > 2) {
                    amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                    val potentialUpi = match.groupValues[2]
                    if (potentialUpi.isNotBlank() && potentialUpi.contains("@")) {
                        upiId = potentialUpi
                    }
                } else if (match.groupValues.size > 1) {
                    amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                }
                if (amount != null) break
            }
        }

        if (amount == null) return null

        // Calculate a simple fingerprint for SMS dedup
        val raw = "$amount|$body"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val fingerprint = bytes.joinToString("") { "%02x".format(it) }

        return RawTransactionData(
                amount = amount,
                upiId = upiId,
                contactName = upiId?.substringBefore("@")?.replace(Regex("[^a-zA-Z]"), " ")?.trim(),
                rawText = body,
                source = DetectionSource.SMS,
                sourceApp = sms.originatingAddress ?: "Bank SMS",
                direction = TransactionDirection.DEBIT,
                fingerprint = fingerprint,
                timestamp = sms.timestampMillis
        )
    }
}
