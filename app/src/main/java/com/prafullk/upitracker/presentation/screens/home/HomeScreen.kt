package com.prafullk.upitracker.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.EmptyState
import com.prafullk.upitracker.presentation.components.TransactionCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
        onNavigateToAllTransactions: () -> Unit,
        onNavigateToTransactionDetail: (String) -> Unit,
        viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                BudgetCard(
                        monthlyTotal = uiState.monthlyTotal,
                        monthlyBudget = uiState.monthlyBudget
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
                    EmptyState(
                            message =
                                    "No transactions detected yet.\nMake a UPI payment to see it here.",
                            modifier = Modifier.height(200.dp)
                    )
                }
            } else {
                items(
                        count = uiState.recentTransactions.size,
                        key = { index -> uiState.recentTransactions[index].id }
                ) { index ->
                    val tx = uiState.recentTransactions[index]
                    TransactionCard(
                            transaction = tx,
                            entityName = tx.contactName, // Would come from joined data in real app
                            entityColor = null,
                            groupName = null, // Would come from joined data in real app
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
fun BudgetCard(monthlyTotal: Double, monthlyBudget: Double?) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = "This Month",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            AmountText(
                    amount = monthlyTotal,
                    direction = "DEBIT",
                    style = MaterialTheme.typography.headlineLarge
            )

            if (monthlyBudget != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (monthlyTotal / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "${(progress * 100).toInt()}% of budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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
