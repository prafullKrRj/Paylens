package com.prafullk.upitracker.presentation.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.screens.settings.SettingsRow
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
        onNavigateToHome: () -> Unit,
        viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                    text = "PayLens",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
            )
            Text(
                    text = "Track every rupee you spend automatically via Accessibility and SMS.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 48.dp),
                    textAlign = TextAlign.Center
            )

            Text("Required Permissions:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                    title = "Accessibility Service",
                    subtitle =
                            if (uiState.accessibilityEnabled) "Enabled"
                            else "Tap to enable (reads payment screens)",
                    onClick = { /* deep link to settings would go here */}
            )

            SettingsRow(
                    title = "Notification Access",
                    subtitle =
                            if (uiState.notificationsEnabled) "Enabled"
                            else "Tap to enable (redundancy layer)",
                    onClick = { /* deep link */}
            )

            SettingsRow(
                    title = "SMS Permission",
                    subtitle = if (uiState.smsEnabled) "Enabled" else "Tap to enable (bank alerts)",
                    onClick = { /* deep link */}
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                    onClick = {
                        viewModel.completeOnboarding()
                        onNavigateToHome()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(text = "Continue to Home") }
        }
    }
}
