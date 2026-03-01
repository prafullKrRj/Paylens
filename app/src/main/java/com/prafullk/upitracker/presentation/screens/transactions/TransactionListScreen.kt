package com.prafullk.upitracker.presentation.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.EmptyState
import com.prafullk.upitracker.presentation.components.TransactionCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionListScreen(
        onNavigateToTransactionDetail: (String) -> Unit,
        onNavigateToClassify: () -> Unit,
        viewModel: TransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.unclassifiedCount > 0) {
            UnclassifiedBanner(count = uiState.unclassifiedCount, onClick = onNavigateToClassify)
        }

        if (uiState.transactions.isEmpty()) {
            EmptyState(message = "No transactions found", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                        count = uiState.transactions.size,
                        key = { index -> uiState.transactions[index].id }
                ) { index ->
                    val tx = uiState.transactions[index]
                    TransactionCard(
                            transaction = tx,
                            entityName = tx.contactName, // Map domain names properly later
                            entityColor = null,
                            groupName = null, // Map domain groups properly later
                            groupColor = null,
                            onClick = { onNavigateToTransactionDetail(tx.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun UnclassifiedBanner(count: Int, onClick: () -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .clickable(onClick = onClick)
                            .padding(16.dp),
            contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                    text = "⚠️ $count unclassified transactions",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = "Classify now →",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
