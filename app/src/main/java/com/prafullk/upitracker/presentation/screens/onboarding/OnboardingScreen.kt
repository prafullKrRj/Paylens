package com.prafullk.upitracker.presentation.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.screens.upiapps.AndroidDrawableImage
import com.prafullk.upitracker.ui.theme.AmountGold
import com.prafullk.upitracker.ui.theme.Indigo400
import com.prafullk.upitracker.ui.theme.Violet400
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
        onNavigateToHome: () -> Unit,
        viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> AccessibilityPage(isEnabled = uiState.accessibilityEnabled)
                2 -> DiscoveryPage(
                        uiState = uiState,
                        onScan = { viewModel.scanForApps() }
                )
            }
        }

        // Dot indicators
        Row(
                modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                        modifier = Modifier
                                .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                        if (pagerState.currentPage == index) Indigo400
                                        else MaterialTheme.colorScheme.outline
                                )
                )
            }
        }

        // Bottom CTA button
        Box(
                modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                        .fillMaxWidth()
        ) {
            Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            1 -> {
                                if (uiState.accessibilityEnabled) {
                                    if (uiState.discoveredApps.isEmpty()) {
                                        viewModel.scanForApps()
                                    }
                                    scope.launch { pagerState.animateScrollToPage(2) }
                                } else {
                                    context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    )
                                }
                            }
                            2 -> {
                                viewModel.completeOnboarding()
                                onNavigateToHome()
                            }
                            else -> scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo400)
            ) {
                Text(
                        text = when (pagerState.currentPage) {
                            0 -> "Get Started"
                            1 -> if (uiState.accessibilityEnabled) "Continue" else "Enable Accessibility"
                            else -> "Start Tracking"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                )
            }
        }
    }
}

@Composable
fun WelcomePage() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(600)) + fadeIn(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo placeholder
                Box(
                        modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(listOf(Indigo400, Violet400))),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = "₹",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                        text = "PayLens",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text = "Every rupee. Accounted for.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(200.dp)) // Space for bottom button
    }
}

@Composable
fun AccessibilityPage(isEnabled: Boolean = false) {
    Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Illustrative "phone showing payment" using Canvas-style drawing
        Box(
                modifier = Modifier
                        .size(160.dp, 240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
        ) {
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
            ) {
                Text(
                        text = "✓",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color(0xFF66BB6A),
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = "Payment\nSuccessful",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "₹500",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AmountGold,
                        fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
                text = "How PayLens Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = "PayLens uses Android's Accessibility Service to read payment success screens — the same way screen readers work. It never reads passwords or sensitive data. It only detects \"Payment Successful\" screens to log your transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
        )
        if (isEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = "✓ Accessibility Service is enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF66BB6A),
                    fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
fun DiscoveryPage(
        uiState: OnboardingUiState,
        onScan: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (uiState.discoveredApps.isEmpty() && !uiState.isScanning) {
            onScan()
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
                text = "Found on your device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = "PayLens will track these UPI apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isScanning) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scanning…", style = MaterialTheme.typography.bodyMedium)
        } else if (uiState.discoveredApps.isEmpty()) {
            Text(
                    "No UPI apps found.\nInstall GPay, PhonePe, or Paytm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.discoveredApps, key = { it.packageName }) { app ->
                    Row(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        AndroidDrawableImage(
                                drawable = app.icon,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                                text = app.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

