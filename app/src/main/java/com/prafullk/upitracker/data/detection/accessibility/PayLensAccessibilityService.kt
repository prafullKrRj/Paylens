package com.prafullk.upitracker.data.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PayLensAccessibilityService : AccessibilityService() {

    private val parser by inject<NodeTreeParser>()
    private val logTransaction by inject<LogTransactionUseCase>()
    private val discoveryService by inject<UpiAppDiscoveryService>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        refreshServiceInfo()
    }

    /** Load active package list from DB and update serviceInfo dynamically. */
    fun refreshServiceInfo() {
        scope.launch {
            val packages = runCatching { discoveryService.loadActivePackageNames() }
                    .getOrDefault(emptyList())
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
                packageNames = packages.toTypedArray()
            }
            serviceInfo = info
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                scope.launch(Dispatchers.Default) {
                    try {
                        parser.extractTransaction(root, pkg)?.let { raw ->
                            logTransaction(raw)
                            discoveryService.recordTransaction(pkg)
                        }
                    } finally {
                        root.recycle()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}

