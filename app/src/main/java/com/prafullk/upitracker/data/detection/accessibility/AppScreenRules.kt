package com.prafullk.upitracker.data.detection.accessibility

import android.view.accessibility.AccessibilityEvent

object AppScreenRules {

        // Screens that should ALWAYS be ignored regardless of text content
        val ALWAYS_IGNORE_PACKAGES =
                setOf<String>(
                        // "com.whatsapp"  // WhatsApp payment bubbles look like success screens but
                        // are
                        // chat history
                        )

        fun shouldProcess(event: AccessibilityEvent, packageName: String): Boolean {
                if (packageName in ALWAYS_IGNORE_PACKAGES) return false
                return true
        }
}
