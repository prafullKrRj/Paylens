package com.prafullk.upitracker.data.detection.accessibility

class ScreenStateTracker {
    private var lastInitiationTime = 0L
    private var lastProcessingTime = 0L
    private var lastSuccessTime = 0L

    fun recordPackage(packageName: String, className: String?) {
        val now = System.currentTimeMillis()
        if (packageName == "in.org.npci.upiapp" ||
                        className?.contains("npci", ignoreCase = true) == true
        ) {
            lastProcessingTime = now
        }
    }

    fun recordStateChange(packageName: String, screenType: ScreenType) {
        val now = System.currentTimeMillis()
        when (screenType) {
            ScreenType.PAYMENT_INITIATION -> lastInitiationTime = now
            ScreenType.PAYMENT_PROCESSING -> lastProcessingTime = now
            ScreenType.PAYMENT_SUCCESS -> {
                lastSuccessTime = now
                // End infinite intent loop: success immediately consumes and wipes the preceding
                // intents
                lastInitiationTime = 0L
                lastProcessingTime = 0L
            }
            else -> {}
        }
    }

    fun hasRecentPaymentIntent(): Boolean {
        val now = System.currentTimeMillis()
        // Strong intent if we saw initiation or processing in the last 60 seconds
        return (now - lastInitiationTime < 60_000) || (now - lastProcessingTime < 60_000)
    }

    fun isCurrentScreenRelevant(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        // Content events only relevant if we had a success state just now (within 5 seconds)
        return (now - lastSuccessTime) < 5_000
    }
}
