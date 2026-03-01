package com.prafullk.upitracker.data.detection.accessibility

enum class ScreenType {
    PAYMENT_INITIATION, // "Send", "Pay", "Enter amount"
    PAYMENT_PROCESSING, // "Processing", "Please wait", NPCI screen
    PAYMENT_SUCCESS, // "Payment successful", etc.
    PAYMENT_FAILED, // "Failed"
    HISTORY_LIST, // History screen
    CHAT_SCREEN, // P2P Chat
    UNKNOWN
}

class ScreenIdentityClassifier {

    fun classify(nodes: List<String>, packageName: String, hasRecentIntent: Boolean): ScreenType {
        val fullText = nodes.joinToString(" ")
        val lowerText = fullText.lowercase()
        val amounts = extractAllAmounts(nodes)
        val timeCount = countTimePatterns(fullText)

        // 1. Check for initiation / processing
        if (isProcessingScreen(lowerText) || packageName == "in.org.npci.upiapp")
                return ScreenType.PAYMENT_PROCESSING
        if (isInitiationScreen(lowerText)) return ScreenType.PAYMENT_INITIATION

        val hasSuccessText = hasStrongSuccessSignal(lowerText)
        val hasTick = hasSuccessGraphics(lowerText)
        val hasDirection =
                lowerText.contains("paid to") ||
                        lowerText.contains("sent to") ||
                        lowerText.contains("money sent") ||
                        lowerText.contains("payment to") ||
                        lowerText.contains("successful")

        // Explicit success always wins (e.g. big "Payment Successful" popup)
        if ((hasSuccessText || hasTick) && hasDirection && amounts.isNotEmpty()) {
            return ScreenType.PAYMENT_SUCCESS
        }

        // Implicit success supported by recent payment intent (user just entered PIN)
        if (hasRecentIntent && hasDirection && amounts.isNotEmpty()) {
            if (!isFailedScreen(lowerText)) {
                return ScreenType.PAYMENT_SUCCESS
            }
        }

        // Error / Rejections
        if (isFailedScreen(lowerText)) return ScreenType.PAYMENT_FAILED
        if (isChatOrHistory(lowerText, amounts.size, timeCount)) {
            val hasPaidTo = lowerText.contains("paid to") || lowerText.contains("sent to")
            val hasReceivedFrom =
                    lowerText.contains("received from") || lowerText.contains("credited")
            // Even if it looks like a history screen, if no 'paid to' or 'received from' exists,
            // default to chat
            if (hasPaidTo && hasReceivedFrom && amounts.size > 1) return ScreenType.HISTORY_LIST
            return ScreenType.CHAT_SCREEN
        }

        return ScreenType.UNKNOWN
    }

    private fun extractAllAmounts(nodes: List<String>): Set<Double> {
        val amountRegex =
                Regex(
                        """(?:[₹]|Rs\.?|INR)\s*((?:\d{1,3}(?:,\d{2,3})+|\d+)(?:\.\d{1,2})?)""",
                        RegexOption.IGNORE_CASE
                )
        val amounts = mutableSetOf<Double>()
        nodes.forEach { text ->
            amountRegex.findAll(text).forEach { match ->
                match.groupValues[1].replace(",", "").toDoubleOrNull()?.let { amounts.add(it) }
            }
        }
        return amounts
    }

    private fun countTimePatterns(text: String): Int {
        val timeRegex = Regex("""\d{1,2}:\d{2}\s*(?:am|pm|AM|PM)""")
        return timeRegex.findAll(text).count()
    }

    private fun isInitiationScreen(text: String): Boolean {
        val initiationKeywords =
                listOf("enter amount", "proceed to pay", "pay ₹", "send ₹", "send money", "pay now")
        return initiationKeywords.any { text.contains(it) } && !isHistoryScreen(text)
    }

    private fun isProcessingScreen(text: String): Boolean {
        val processingKeywords =
                listOf(
                        "processing",
                        "please wait",
                        "initiating",
                        "connecting",
                        "authenticating",
                        "processing payment",
                        "fetching",
                        "secure connection",
                        "verifying"
                )
        return processingKeywords.any { text.contains(it) }
    }

    private fun hasSuccessGraphics(text: String): Boolean {
        val graphicsKeywords =
                listOf(
                        "tick",
                        "check mark",
                        "checkmark",
                        "success icon",
                        "done icon",
                        "success_tick",
                        "ic_success",
                        "animation"
                )
        return graphicsKeywords.any { text.contains(it) }
    }

    private fun hasStrongSuccessSignal(text: String): Boolean {
        val strongSignals =
                listOf(
                        "payment successful",
                        "money sent successfully",
                        "sent successfully",
                        "payment done",
                        "transfer successful",
                        "transaction successful",
                        "payment complete",
                        "paid successfully",
                        "securely paid"
                )
        return strongSignals.any { text.contains(it) }
    }

    private fun isFailedScreen(text: String): Boolean {
        val failureKeywords =
                listOf(
                        "failed",
                        "declined",
                        "cancelled",
                        "timed out",
                        "could not complete",
                        "unsuccessful"
                )
        return failureKeywords.any { text.contains(it) }
    }

    private fun isHistoryScreen(text: String): Boolean {
        val historyIndicators =
                listOf(
                        "transaction history",
                        "all transactions",
                        "payment history",
                        "my statements",
                        "recent transactions",
                        "transaction list",
                        "load more",
                        "see all transactions",
                        "filter by"
                )
        return historyIndicators.any { text.contains(it) }
    }

    private fun isChatOrHistory(text: String, amountCount: Int, timeCount: Int): Boolean {
        if (amountCount >= 3) return true
        if (timeCount >= 3) return true
        if (isHistoryScreen(text)) return true
        return false
    }
}
