package com.prafullk.upitracker.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

// === Base Palette ===
val Indigo300 = Color(0xFF7986CB)
val Indigo400 = Color(0xFF5C6BC0)
val Indigo500 = Color(0xFF3F51B5)
val Indigo600 = Color(0xFF3949AB)
val Violet400 = Color(0xFF7E57C2)
val Violet600 = Color(0xFF5E35B1)

// Dark theme surface
val Surface900 = Color(0xFF0F0F1A)
val Surface800 = Color(0xFF161627)
val Surface700 = Color(0xFF1E1E33)
val Surface600 = Color(0xFF25253E)

// Light theme surface
val SurfaceLight = Color(0xFFF5F5FF)
val SurfaceElevatedLight = Color(0xFFFFFFFF)
val SurfaceElevatedDark = Color(0xFF252540)

// === Semantic Colors ===
val DebitRed = Color(0xFFEF5350)        // warm coral red
val DebitRedDark = Color(0xFFCF4545)    // slightly desaturated in dark
val CreditGreen = Color(0xFF66BB6A)
val AmountGold = Color(0xFFFFB300)

val MutedTextLight = Color(0xFF9E9E9E)
val MutedTextDark = Color(0xFF7A7A9A)

// Legacy compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/** Deterministically generate an avatar background color from an entity name. */
fun String.toAvatarColor(): Color {
    val hue = (this.hashCode().absoluteValue % 360).toFloat()
    return Color.hsl(hue, 0.45f, 0.55f)
}
