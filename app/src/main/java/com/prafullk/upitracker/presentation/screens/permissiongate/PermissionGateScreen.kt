package com.prafullk.upitracker.presentation.screens.permissiongate

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PermissionGateScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun isAccessibilityEnabled(): Boolean {
        val enabledServices =
                Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
        return enabledServices.contains(context.packageName, ignoreCase = true)
    }

    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners =
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                        ?: ""
        return enabledListeners.contains(context.packageName, ignoreCase = true)
    }

    var accessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled()) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = isAccessibilityEnabled()
                notificationGranted = isNotificationListenerEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                    text = "Set Up PayLens",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Grant these permissions so PayLens can detect your UPI payments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            PermissionCard(
                    title = "Accessibility Service",
                    description =
                            "Allows PayLens to detect UPI payment screens and log transactions automatically.",
                    isGranted = accessibilityGranted,
                    onEnable = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                    title = "Notification Listener",
                    description =
                            "Provides a secondary layer to catch payment notifications from UPI apps.",
                    isGranted = notificationGranted,
                    onEnable = {
                        context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (accessibilityGranted) {
                Button(
                        onClick = onAllGranted,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(56.dp)
                                        .padding(bottom = 0.dp),
                        shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                            "Continue to App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                        text = "Enable Accessibility Service to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionCard(
        title: String,
        description: String,
        isGranted: Boolean,
        onEnable: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                    )
                    if (isGranted) {
                        Text(
                                text = " ✓",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF66BB6A),
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isGranted) {
                TextButton(onClick = onEnable) { Text("Enable") }
            }
        }
    }
}
