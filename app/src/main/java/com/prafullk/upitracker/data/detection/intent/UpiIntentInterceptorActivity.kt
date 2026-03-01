package com.prafullk.upitracker.data.detection.intent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.prafullk.upitracker.data.detection.accessibility.ScreenStateTracker
import com.prafullk.upitracker.data.detection.accessibility.ScreenType
import org.koin.android.ext.android.inject

class UpiIntentInterceptorActivity : ComponentActivity() {

    private val screenStateTracker: ScreenStateTracker by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Log the hard intent
        screenStateTracker.recordStateChange("intent://proxy", ScreenType.PAYMENT_INITIATION)

        // 2. Clone the incoming intent
        val originalIntent = intent
        val forwardIntent = Intent(Intent.ACTION_VIEW)
        forwardIntent.data = originalIntent.data ?: Uri.parse("upi://pay")

        // 3. Prevent infinite recursion by forcing the chooser to skip this explicit class
        // Android 10+ handles Intent.createChooser gracefully
        val chooser = Intent.createChooser(forwardIntent, "Pay with")

        // 4. Fire the chooser and finish our invisible proxy activity immediately
        startActivity(chooser)
        finish()
    }
}
