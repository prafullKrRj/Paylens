package com.prafullk.upitracker.data.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.prafullk.upitracker.data.db.dao.UpiAppDao
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PayLensAccessibilityService : AccessibilityService() {

    private val parser by inject<NodeTreeParser>()
    private val logTransaction by inject<LogTransactionUseCase>()
    private val discoveryService by inject<UpiAppDiscoveryService>()
    private val upiAppDao by inject<UpiAppDao>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val screenIdentityClassifier = ScreenIdentityClassifier()
    private val screenStateTracker: ScreenStateTracker by inject()

    override fun onServiceConnected() {
        refreshServiceInfo()
        // Observe DB changes and refresh package filter automatically
        scope.launch {
            upiAppDao.observeAll().collectLatest { apps ->
                val activePackages = apps.filter { it.isActive }.map { it.packageName }
                applyPackageFilter(activePackages)
            }
        }
    }

    /** Load active package list from DB and update serviceInfo dynamically. */
    fun refreshServiceInfo() {
        scope.launch {
            val packages =
                    runCatching { discoveryService.loadActivePackageNames() }
                            .getOrDefault(emptyList())
            applyPackageFilter(packages)
        }
    }

    private fun applyPackageFilter(packages: List<String>) {
        val info =
                AccessibilityServiceInfo().apply {
                    eventTypes =
                            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                    flags =
                            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    notificationTimeout = 100
                    packageNames = packages.toTypedArray()
                }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Record package/className to detect NPCI overlay presence without reading nodes
        screenStateTracker.recordPackage(pkg, event.className?.toString())

        if (!AppScreenRules.shouldProcess(event, pkg)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val root = rootInActiveWindow ?: return
                scope.launch(Dispatchers.Default) {
                    try {
                        val allText = mutableListOf<String>()
                        parser.collectTextNodes(root, allText)

                        val hasIntent = screenStateTracker.hasRecentPaymentIntent()
                        val screenType = screenIdentityClassifier.classify(allText, pkg, hasIntent)
                        screenStateTracker.recordStateChange(pkg, screenType)

                        if (screenType == ScreenType.PAYMENT_SUCCESS && screenStateTracker.canLogTransaction()) {
                            parser.extractTransaction(root, pkg, hasIntent)?.let { raw ->
                                logTransaction(raw)
                                screenStateTracker.consumeIntent()
                                discoveryService.recordTransaction(pkg)
                            }
                        }
                    } finally {
                        root.recycle()
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!screenStateTracker.isCurrentScreenRelevant(pkg)) return
                if (!screenStateTracker.canLogTransaction()) return
                val root = rootInActiveWindow ?: return
                scope.launch(Dispatchers.Default) {
                    try {
                        val hasIntent = screenStateTracker.hasRecentPaymentIntent()
                        parser.extractTransaction(root, pkg, hasIntent)?.let { raw ->
                            logTransaction(raw)
                            screenStateTracker.consumeIntent()
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
