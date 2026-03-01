package com.prafullk.upitracker.data.detection.accessibility.strategy

/** Structured data from screen parsing before finalisation. */
data class ParsedScreenData(
        val amount: Double?,
        val upiId: String?,
        val contactName: String?,
        val hasSuccessKeyword: Boolean,
        val hasFailureKeyword: Boolean,
        val direction: String = "DEBIT"
) {
    /**
     * Confidence score 0.0–1.0 based on how many signals are present.
     * Only transactions with confidence ≥ 0.6 should be logged.
     */
    val confidence: Float get() {
        var score = 0f
        if (amount != null && amount > 0) score += 0.4f
        if (hasSuccessKeyword && !hasFailureKeyword) score += 0.3f
        if (upiId != null) score += 0.15f
        if (contactName != null) score += 0.15f
        return score
    }
}

/** Strategy interface — each UPI app has its own concrete implementation. */
interface UpiParserStrategy {
    val supportedPackage: String

    /**
     * Given all text nodes from the screen, attempt to extract transaction data.
     * Returns null if this screen doesn't appear to be a payment success screen.
     */
    fun extract(nodes: List<String>): ParsedScreenData?
}

