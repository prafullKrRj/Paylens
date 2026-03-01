package com.prafullk.upitracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import com.prafullk.upitracker.data.preferences.AppPreferences
import com.prafullk.upitracker.presentation.navigation.PayLensNavGraph
import com.prafullk.upitracker.presentation.navigation.Route
import com.prafullk.upitracker.ui.theme.UPITrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val upiAppDiscoveryService by inject<UpiAppDiscoveryService>()
    private val appPreferences by inject<AppPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Run UPI app discovery on every launch (fast — PackageManager queries only)
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { upiAppDiscoveryService.discoverAndSync() }
        }

        setContent {
            UPITrackerTheme {
                val isOnboardingCompleted by
                        appPreferences.isOnboardingCompleted.collectAsStateWithLifecycle(
                                initialValue = null
                        )

                if (isOnboardingCompleted != null) {
                    val startDest =
                            if (isOnboardingCompleted == true) Route.PermissionGate.path
                            else Route.Onboarding.path
                    val navController = rememberNavController()

                    PayLensNavGraph(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
