package com.prafullk.upitracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
        primary = Indigo300,
        onPrimary = Surface900,
        primaryContainer = Surface700,
        onPrimaryContainer = Indigo300,
        secondary = Violet400,
        onSecondary = Surface900,
        secondaryContainer = Surface600,
        onSecondaryContainer = Violet400,
        tertiary = AmountGold,
        background = Surface900,
        surface = Surface800,
        surfaceVariant = Surface700,
        onBackground = Color(0xFFE8E8F8),
        onSurface = Color(0xFFE8E8F8),
        onSurfaceVariant = MutedTextDark,
        error = DebitRedDark,
        outline = Surface600
)

private val LightColorScheme = lightColorScheme(
        primary = Indigo500,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8EAF6),
        onPrimaryContainer = Indigo600,
        secondary = Violet600,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDE7F6),
        onSecondaryContainer = Violet600,
        tertiary = AmountGold,
        background = SurfaceLight,
        surface = SurfaceElevatedLight,
        surfaceVariant = Color(0xFFEEEEFF),
        onBackground = Color(0xFF1A1A2E),
        onSurface = Color(0xFF1A1A2E),
        onSurfaceVariant = MutedTextLight,
        error = DebitRed,
        outline = Color(0xFFBBBBCC)
)

@Composable
fun UPITrackerTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false, // Disabled to use our intentional design
        content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
    )
}
