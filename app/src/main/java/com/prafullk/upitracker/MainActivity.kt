package com.prafullk.upitracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import com.prafullk.upitracker.presentation.navigation.PayLensNavGraph
import com.prafullk.upitracker.ui.theme.UPITrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val upiAppDiscoveryService by inject<UpiAppDiscoveryService>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Run UPI app discovery on every launch (fast — PackageManager queries only)
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { upiAppDiscoveryService.discoverAndSync() }
        }

        setContent {
            UPITrackerTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PayLensNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
