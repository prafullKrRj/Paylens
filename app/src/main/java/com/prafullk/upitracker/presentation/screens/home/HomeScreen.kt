package com.prafullk.upitracker.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.EmptyState
import com.prafullk.upitracker.presentation.components.TransactionCard
import com.prafullk.upitracker.ui.theme.AmountGold
import com.prafullk.upitracker.ui.theme.Indigo400
import com.prafullk.upitracker.ui.theme.Violet400
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        onNavigateToAllTransactions: () -> Unit,
        onNavigateToTransactionDetail: (String) -> Unit,
        onNavigateToSettings: () -> Unit = {},
        viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("PayLens") },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                )
            }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            GradientSummaryCard(
                    monthlyTotal = uiState.monthlyTotal,
                    monthlyBudget = uiState.monthlyBudget,
                    todayTotal = uiState.todayTotal,
                    weekTotal = uiState.weekTotal
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitleRow(
                    title = "Recent Transactions",
                    action = "See All",
                    onActionClick = onNavigateToAllTransactions
            )
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn()) {
                    EmptyState(
                            message =
                                    "No transactions detected yet.\nMake a UPI payment to see it here.",
                            modifier = Modifier.height(200.dp)
                    )
                }
            }
        } else {
            items(
                    count = uiState.recentTransactions.size,
                    key = { index -> uiState.recentTransactions[index].id }
            ) { index ->
                val tx = uiState.recentTransactions[index]
                TransactionCard(
                        transaction = tx,
                        entityName = tx.contactName,
                        entityColor = null,
                        groupName = null,
                        groupColor = null,
                        onClick = { onNavigateToTransactionDetail(tx.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitleRow(title = "Top Spends This Month", action = null, onActionClick = {})
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(
                count = uiState.topSpendingGroups.size,
                key = { index -> uiState.topSpendingGroups[index].groupId }
        ) { index ->
            val group = uiState.topSpendingGroups[index]
            TopSpendRow(group = group)
        }

        item {
            Spacer(modifier = Modifier.height(32.dp)) // padding for bottom nav
        }
    }
    }
}

@Composable
fun GradientSummaryCard(
        monthlyTotal: Double,
        monthlyBudget: Double?,
        todayTotal: Double,
        weekTotal: Double
) {
    val gradientBrush = Brush.linearGradient(colors = listOf(Indigo400, Violet400))
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                    text = "This Month",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = format.format(monthlyTotal),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
            )

            if (monthlyBudget != null && monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (monthlyTotal / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AmountGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text =
                                    "${(progress * 100).toInt()}% of ₹${monthlyBudget.toInt()} budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                    )
                    if (progress >= 0.9f) {
                        Text(
                                text = "⚠ Near limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmountGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stat pills
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatPill(label = "Today", amount = todayTotal, modifier = Modifier.weight(1f))
                QuickStatPill(
                        label = "This Week",
                        amount = weekTotal,
                        modifier = Modifier.weight(1f)
                )
                val avgPerDay = if (monthlyTotal > 0) monthlyTotal / 30 else 0.0
                QuickStatPill(label = "Avg/day", amount = avgPerDay, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuickStatPill(label: String, amount: Double, modifier: Modifier = Modifier) {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = 0

    Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = format.format(amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
            )
        }
    }
}

@Composable
fun SectionTitleRow(title: String, action: String?, onActionClick: () -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
        )
        if (action != null) {
            TextButton(onClick = onActionClick) { Text(text = action) }
        }
    }
}

@Composable
fun TopSpendRow(group: GroupSpending) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Group color strip
        Box(
                modifier =
                        Modifier.width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(group.groupColor))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
                text = group.groupName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
        )
        AmountText(
                amount = group.amount,
                direction = "DEBIT",
                style = MaterialTheme.typography.bodyLarge
        )
    }
}
