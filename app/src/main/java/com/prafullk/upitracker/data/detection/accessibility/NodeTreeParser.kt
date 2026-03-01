package com.prafullk.upitracker.data.detection.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.prafullk.upitracker.data.detection.accessibility.strategy.AmazonPayParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.BhimParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.GPayParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.GenericUpiParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.PaytmParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.PhonePeParserStrategy
import com.prafullk.upitracker.data.detection.accessibility.strategy.UpiParserStrategy
import com.prafullk.upitracker.data.intelligence.DeduplicationEngine
import com.prafullk.upitracker.domain.model.DetectionSource
import com.prafullk.upitracker.domain.model.RawTransactionData
import com.prafullk.upitracker.domain.model.TransactionDirection

private const val MIN_CONFIDENCE = 0.6f

class NodeTreeParser(private val dedup: DeduplicationEngine) {

    private val screenIdentityClassifier = ScreenIdentityClassifier()

    private val strategies: Map<String, UpiParserStrategy> =
            listOf(
                            GPayParserStrategy(),
                            PhonePeParserStrategy(),
                            PaytmParserStrategy(),
                            BhimParserStrategy(),
                            AmazonPayParserStrategy()
                    )
                    .associateBy { it.supportedPackage }

    private val genericStrategy = GenericUpiParserStrategy()

    suspend fun extractTransaction(
            root: AccessibilityNodeInfo,
            sourcePackage: String,
            hasRecentIntent: Boolean
    ): RawTransactionData? {

        val allText = mutableListOf<String>()
        collectTextNodes(root, allText)

        if (allText.isEmpty()) return null

        val screenType = screenIdentityClassifier.classify(allText, sourcePackage, hasRecentIntent)
        if (screenType != ScreenType.PAYMENT_SUCCESS) return null

        val strategy = strategies[sourcePackage] ?: genericStrategy
        val parsed = strategy.extract(allText) ?: return null

        if (parsed.confidence < MIN_CONFIDENCE) return null

        val direction = detectDirection(allText)
        if (direction == TransactionDirection.CREDIT) return null

        val amount = parsed.amount ?: return null
        val timestampMs = System.currentTimeMillis()

        val fingerprint = dedup.fingerprint(amount, sourcePackage, timestampMs)
        if (dedup.isDuplicate(fingerprint)) return null

        return RawTransactionData(
                amount = amount,
                upiId = parsed.upiId,
                contactName = parsed.contactName,
                rawText = allText.joinToString("|"),
                source = DetectionSource.ACCESSIBILITY,
                sourceApp = sourcePackage,
                direction = direction,
                fingerprint = fingerprint,
                timestamp = timestampMs
        )
    }

    private fun detectDirection(nodes: List<String>): String {
        val fullText = nodes.joinToString(" ").lowercase()
        val debitSignals =
                listOf(
                        "paid to",
                        "sent to",
                        "money sent",
                        "you paid",
                        "debited",
                        "payment to",
                        "transferred to",
                        "you sent"
                )
        val creditSignals =
                listOf(
                        "received from",
                        "money received",
                        "credited to your",
                        "you received",
                        "payment received",
                        "added to wallet",
                        "received",
                        "credit"
                )

        val debitScore = debitSignals.count { fullText.contains(it) }
        val creditScore = creditSignals.count { fullText.contains(it) }

        return when {
            debitScore > creditScore -> TransactionDirection.DEBIT
            creditScore > debitScore -> TransactionDirection.CREDIT
            else -> TransactionDirection.DEBIT
        }
    }

    fun collectTextNodes(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child =
                    try {
                        node.getChild(i)
                    } catch (e: Exception) {
                        null
                    }
            if (child != null) {
                try {
                    collectTextNodes(child, out)
                } finally {
                    try {
                        child.recycle()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
