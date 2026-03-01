package com.prafullk.upitracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupChip(
        name: String,
        icon: ImageVector = Icons.Default.HelpOutline,
        color: Int = 0xFFE0E0E0.toInt(),
        modifier: Modifier = Modifier
) {
    val chipColor = Color(color)
    Row(
            modifier =
                    modifier.clip(RoundedCornerShape(12.dp))
                            .background(chipColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = chipColor.copy(alpha = 0.8f),
                fontSize = 12.sp
        )
    }
}
