package com.prafullk.upitracker.presentation.screens.analytics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prafullk.upitracker.presentation.components.EmptyState
import org.koin.androidx.compose.koinViewModel

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = koinViewModel()) {
    Scaffold { padding ->
        EmptyState(
                message =
                        "Analytics features coming soon!\nVico charts will be placed here to show your spending trends.",
                modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
