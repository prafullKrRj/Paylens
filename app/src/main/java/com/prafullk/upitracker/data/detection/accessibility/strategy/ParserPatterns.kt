package com.prafullk.upitracker.data.detection.accessibility.strategy

/**
 * Shared regex patterns and keyword sets used across parser strategies.
 */
object ParserPatterns {

    /** Handles: ₹500, ₹1,500, ₹1,500.00, Rs. 500, Rs 500.00, INR 500,
     *           500.00 ₹ (amount before symbol), ₹ 5,00,000 (Indian lakh) */
    val amountRegex = Regex(
            """(?:(?:[₹]|Rs\.?|INR)\s*(\d{1,3}(?:,\d{2,3})*(?:\.\d{1,2})?)|(\d{1,3}(?:,\d{2,3})*(?:\.\d{1,2})?)\s*[₹])""",
            RegexOption.IGNORE_CASE
    )

    val upiVpaRegex = Regex("""[\w.\-+]+@[\w]+""")

    val successKeywords = setOf(
            "payment successful",
            "paid successfully",
            "money sent",
            "sent successfully",
            "payment done",
            "transaction successful",
            "transaction complete",
            "transfer successful",
            "payment complete",
            "paid",
            "debited",
            "success"
    )

    val failureKeywords = setOf(
            "failed",
            "declined",
            "cancelled",
            "timeout",
            "error",
            "pending",
            "try again"
    )

    val paidToRegex = Regex(
            """(?:paid to|sent to|transferred to|to)\s+([A-Z][a-zA-Z\s]{1,40})""",
            RegexOption.IGNORE_CASE
    )

    fun extractAmount(texts: List<String>): Double? {
        for (text in texts) {
            val match = amountRegex.find(text) ?: continue
            val raw = (match.groupValues[1].ifBlank { match.groupValues[2] })
                    .replace(",", "")
            val value = raw.toDoubleOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }

    fun extractUpiVpa(texts: List<String>): String? {
        for (text in texts) {
            val vpa = upiVpaRegex.find(text)?.value
            if (vpa != null) return vpa
        }
        return null
    }

    fun extractContactName(texts: List<String>, upiId: String?): String? {
        for (text in texts) {
            paidToRegex.find(text)?.let { return it.groupValues[1].trim() }
        }
        return upiId?.substringBefore("@")?.replace(Regex("[^a-zA-Z]"), " ")?.trim()
    }

    fun hasSuccess(fullText: String) = successKeywords.any { fullText.contains(it) }
    fun hasFailure(fullText: String) = failureKeywords.any { fullText.contains(it) }
}
