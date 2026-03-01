package com.prafullk.upitracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prafullk.upitracker.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
        transaction: Transaction,
        entityName: String?,
        entityColor: Int?,
        groupName: String?,
        groupColor: Int?,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    val df = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = df.format(Date(transaction.timestamp))

    val displayName = entityName ?: transaction.contactName ?: "Unknown"
    val avatarColor = entityColor ?: 0xFF9E9E9E.toInt()

    Row(
            modifier =
                    modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        EntityAvatar(name = displayName, color = avatarColor)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (groupName != null) {
                    GroupChip(name = groupName, color = groupColor ?: 0xFFE0E0E0.toInt())
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                        text = "${transaction.sourceApp ?: "Bank"} • $timeString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            AmountText(amount = transaction.amount, direction = transaction.direction)
        }
    }
}
