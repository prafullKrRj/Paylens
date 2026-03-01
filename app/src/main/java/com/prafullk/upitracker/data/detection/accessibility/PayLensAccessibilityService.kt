package com.prafullk.upitracker.data.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PayLensAccessibilityService : AccessibilityService() {

    private val parser by inject<NodeTreeParser>()
    private val logTransaction by inject<LogTransactionUseCase>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val WATCHED_PACKAGES =
            setOf(
                    "com.google.android.apps.nbu.paisa.user",
                    "net.one97.paytm",
                    "com.phonepe.app",
                    "in.org.npci.upiapp",
                    "com.amazon.mShop.android.shopping",
                    "com.freecharge.android",
                    "com.mobikwik_new"
            )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!WATCHED_PACKAGES.contains(pkg)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                scope.launch {
                    parser.extractTransaction(root, pkg)?.let { raw -> logTransaction(raw) }
                    root.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {}
}
