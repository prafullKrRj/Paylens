package com.prafullk.upitracker.presentation.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        onNavigateBack: () -> Unit,
        onNavigateToUpiApps: () -> Unit = {},
        viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showBudgetDialog by viewModel.showBudgetDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (showBudgetDialog) {
        var budgetText by remember(uiState.monthlyBudget) {
            mutableStateOf(uiState.monthlyBudget.toBigDecimal().stripTrailingZeros().toPlainString())
        }
        AlertDialog(
                onDismissRequest = { viewModel.showBudgetDialog.value = false },
                title = { Text("Monthly Budget") },
                text = {
                    OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                budgetText.toDoubleOrNull()?.let { viewModel.updateBudget(it) }
                                viewModel.showBudgetDialog.value = false
                            }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showBudgetDialog.value = false }) {
                        Text("Cancel")
                    }
                }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SectionTitle("Detection")
                SettingsRow(
                        title = "Tracked UPI Apps",
                        subtitle = "Manage which apps PayLens monitors",
                        onClick = onNavigateToUpiApps
                )
                SettingsRow(
                        title = "Detection Services",
                        subtitle = "Accessibility & Notifications",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                )
                HorizontalDivider()

                SectionTitle("Preferences")
                SettingsRow(
                        title = "Monthly Budget",
                        subtitle = "₹${uiState.monthlyBudget.toInt()}",
                        onClick = { viewModel.showBudgetDialog.value = true }
                )
                SettingsRow(title = "Currency", subtitle = uiState.currency, onClick = {})
                HorizontalDivider()

                SectionTitle("Data")
                SettingsRow(title = "Export to CSV", onClick = {})
                SettingsRow(title = "Export to JSON", onClick = {})
                SettingsRow(
                        title = "Clear All Data",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.clearData() }
                )
                HorizontalDivider()

                SectionTitle("About")
                SettingsRow(title = "Version", subtitle = "1.0.0", onClick = {})
                SettingsRow(title = "Privacy Policy", onClick = {})

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(
        title: String,
        subtitle: String? = null,
        textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
        onClick: () -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = textColor, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
