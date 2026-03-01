package com.prafullk.upitracker.data.detection.accessibility

class ScreenStateTracker {
    private var lastInitiationTime = 0L
    private var lastProcessingTime = 0L
    private var lastSuccessTime = 0L

    // True when a PAYMENT_INITIATION has been detected and no transaction has been logged yet for
    // that session.  Starts as false so no logging occurs until an initiation is observed.
    // Set to true on PAYMENT_INITIATION, reset to false after a transaction is logged.
    private var intentAvailable = false

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
            ScreenType.PAYMENT_INITIATION -> {
                lastInitiationTime = now
                // A new payment session has started – allow exactly one transaction to be logged.
                intentAvailable = true
            }
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

    /**
     * Returns true when the current payment session still allows a transaction to be logged.
     * Only one transaction is permitted per payment initiation; further screens (e.g. transaction
     * details) are ignored until a new initiation is detected.
     */
    fun canLogTransaction(): Boolean = intentAvailable

    /**
     * Mark the current payment session as having produced a transaction. Subsequent success-like
     * screens will be ignored until the next PAYMENT_INITIATION resets the session.
     */
    fun consumeIntent() {
        intentAvailable = false
    }
}
