package com.prafullk.upitracker.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.prafullk.upitracker.domain.model.TransactionDirection
import com.prafullk.upitracker.ui.theme.CreditGreen
import com.prafullk.upitracker.ui.theme.DebitRed
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun AmountText(
        amount: Double,
        direction: String,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.bodyLarge,
        fontWeight: FontWeight = FontWeight.Bold
) {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    val formattedAmount = format.format(amount)

    val color =
            when (direction) {
                TransactionDirection.DEBIT -> DebitRed
                TransactionDirection.CREDIT -> CreditGreen
                else -> MaterialTheme.colorScheme.onSurface
            }

    val prefix =
            when (direction) {
                TransactionDirection.DEBIT -> "-"
                TransactionDirection.CREDIT -> "+"
                else -> ""
            }

    Text(
            text = "$prefix$formattedAmount",
            color = color,
            style = style,
            fontWeight = fontWeight,
            modifier = modifier
    )
}

