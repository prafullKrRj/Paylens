package com.prafullk.upitracker.data.detection.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.prafullk.upitracker.data.intelligence.DeduplicationEngine
import com.prafullk.upitracker.domain.model.DetectionSource
import com.prafullk.upitracker.domain.model.RawTransactionData
import com.prafullk.upitracker.domain.model.TransactionDirection

class NodeTreeParser(private val dedup: DeduplicationEngine) {

    // Success indicators across apps (keep updated)
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
    private val failureKeywords = setOf("failed", "declined", "cancelled", "timeout")

    // Amount regex: handles ₹500, ₹1,500.00, Rs. 500
    private val amountRegex = Regex("""[₹Rs.]+\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")

    // UPI VPA regex
    private val upiVpaRegex = Regex("""[\w.\-+]+@[\w]+""")

    suspend fun extractTransaction(
            root: AccessibilityNodeInfo,
            sourcePackage: String
    ): RawTransactionData? {

        val allText = mutableListOf<String>()
        collectTextNodes(root, allText)

        val fullText = allText.joinToString(" ").lowercase()

        // Only proceed on success screens
        val hasSuccess = successKeywords.any { fullText.contains(it) }
        val hasFailure = failureKeywords.any { fullText.contains(it) }
        if (!hasSuccess || hasFailure) return null

        val amount = extractAmount(allText) ?: return null
        val upiId = extractUpiVpa(allText)
        val contactName = extractContactName(allText, upiId)

        val fingerprint = dedup.fingerprint(amount, sourcePackage)
        if (dedup.isDuplicate(fingerprint)) return null

        return RawTransactionData(
                amount = amount,
                upiId = upiId,
                contactName = contactName,
                rawText = allText.joinToString("|"),
                source = DetectionSource.ACCESSIBILITY,
                sourceApp = sourcePackage,
                direction = TransactionDirection.DEBIT,
                fingerprint = fingerprint,
                timestamp = System.currentTimeMillis()
        )
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectTextNodes(child, out)
                child.recycle()
            }
        }
    }

    private fun extractAmount(texts: List<String>): Double? {
        for (text in texts) {
            amountRegex.find(text)?.let { match ->
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractUpiVpa(texts: List<String>): String? {
        for (text in texts) {
            upiVpaRegex.find(text)?.let {
                return it.value
            }
        }
        return null
    }

    private fun extractContactName(texts: List<String>, upiId: String?): String? {
        // "Paid to Prafull Kumar" → "Prafull Kumar"
        val paidToRegex =
                Regex("""(?:paid to|sent to|to)\s+([A-Z][a-zA-Z\s]+)""", RegexOption.IGNORE_CASE)
        for (text in texts) {
            paidToRegex.find(text)?.let {
                return it.groupValues[1].trim()
            }
        }
        // Fallback: extract from UPI VPA prefix
        return upiId?.substringBefore("@")?.replace(Regex("[^a-zA-Z]"), " ")?.trim()
    }
}
