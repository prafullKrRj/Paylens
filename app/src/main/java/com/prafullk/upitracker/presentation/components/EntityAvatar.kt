package com.prafullk.upitracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prafullk.upitracker.ui.theme.toAvatarColor

@Composable
fun EntityAvatar(
        name: String,
        color: Int,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
) {
    val avatarColor = if (color != 0) Color(color) else name.toAvatarColor()
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val fontSize = (size.value * 0.45f).sp
    Box(
            modifier = modifier.size(size).clip(CircleShape).background(avatarColor),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
        )
    }
}

