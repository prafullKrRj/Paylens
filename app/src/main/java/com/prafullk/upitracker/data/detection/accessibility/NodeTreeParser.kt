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

private const val MIN_CONFIDENCE = 0.6f

class NodeTreeParser(private val dedup: DeduplicationEngine) {

    private val strategies: Map<String, UpiParserStrategy> = listOf(
            GPayParserStrategy(),
            PhonePeParserStrategy(),
            PaytmParserStrategy(),
            BhimParserStrategy(),
            AmazonPayParserStrategy()
    ).associateBy { it.supportedPackage }

    private val genericStrategy = GenericUpiParserStrategy()

    suspend fun extractTransaction(
            root: AccessibilityNodeInfo,
            sourcePackage: String
    ): RawTransactionData? {

        val allText = mutableListOf<String>()
        collectTextNodes(root, allText)

        if (allText.isEmpty()) return null

        val strategy = strategies[sourcePackage] ?: genericStrategy
        val parsed = strategy.extract(allText) ?: return null

        // Confidence gate: skip low-signal screens
        if (parsed.confidence < MIN_CONFIDENCE) return null

        val amount = parsed.amount ?: return null
        val fingerprint = dedup.fingerprint(amount, sourcePackage)
        if (dedup.isDuplicate(fingerprint)) return null

        return RawTransactionData(
                amount = amount,
                upiId = parsed.upiId,
                contactName = parsed.contactName,
                rawText = allText.joinToString("|"),
                source = DetectionSource.ACCESSIBILITY,
                sourceApp = sourcePackage,
                direction = parsed.direction,
                fingerprint = fingerprint,
                timestamp = System.currentTimeMillis()
        )
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            if (child != null) {
                try {
                    collectTextNodes(child, out)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
            }
        }
    }
}


