package com.prafullk.upitracker.data.detection.accessibility.strategy

/** Generic fallback strategy for any UPI app not specifically handled. */
class GenericUpiParserStrategy : UpiParserStrategy {
    override val supportedPackage: String = "*"

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText)
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)

        return ParsedScreenData(
                amount = amount,
                upiId = upiId,
                contactName = contactName,
                hasSuccessKeyword = hasSuccess,
                hasFailureKeyword = hasFailure
        )
    }
}

class GPayParserStrategy : UpiParserStrategy {
    override val supportedPackage = "com.google.android.apps.nbu.paisa.user"

    // GPay-specific success patterns
    private val gpaySuccessKeywords = setOf(
            "payment successful",
            "paid",
            "₹",
            "sent to"
    )

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText) ||
                gpaySuccessKeywords.any { fullText.contains(it) }
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)
                ?: extractGPayContactName(nodes)

        return ParsedScreenData(amount, upiId, contactName, hasSuccess, hasFailure)
    }

    private fun extractGPayContactName(nodes: List<String>): String? {
        // GPay shows name prominently before amount
        for (text in nodes) {
            if (text.length in 2..40 && text.first().isLetter() && !text.contains("₹") &&
                    !text.contains("@") && !text.all { it.isUpperCase() }) {
                return text.trim()
            }
        }
        return null
    }
}

class PhonePeParserStrategy : UpiParserStrategy {
    override val supportedPackage = "com.phonepe.app"

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText) ||
                fullText.contains("transfer successful") ||
                fullText.contains("money sent")
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)

        return ParsedScreenData(amount, upiId, contactName, hasSuccess, hasFailure)
    }
}

class PaytmParserStrategy : UpiParserStrategy {
    override val supportedPackage = "net.one97.paytm"

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText) ||
                fullText.contains("order placed") ||
                fullText.contains("payment done")
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)

        return ParsedScreenData(amount, upiId, contactName, hasSuccess, hasFailure)
    }
}

class BhimParserStrategy : UpiParserStrategy {
    override val supportedPackage = "in.org.npci.upiapp"

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText) ||
                fullText.contains("transaction successful")
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)

        return ParsedScreenData(amount, upiId, contactName, hasSuccess, hasFailure)
    }
}

class AmazonPayParserStrategy : UpiParserStrategy {
    override val supportedPackage = "com.amazon.mShop.android.shopping"

    override fun extract(nodes: List<String>): ParsedScreenData? {
        val fullText = nodes.joinToString(" ").lowercase()
        val hasSuccess = ParserPatterns.hasSuccess(fullText) ||
                fullText.contains("payment complete") ||
                fullText.contains("order placed")
        val hasFailure = ParserPatterns.hasFailure(fullText)

        val amount = ParserPatterns.extractAmount(nodes)
        val upiId = ParserPatterns.extractUpiVpa(nodes)
        val contactName = ParserPatterns.extractContactName(nodes, upiId)

        return ParsedScreenData(amount, upiId, contactName, hasSuccess, hasFailure)
    }
}

