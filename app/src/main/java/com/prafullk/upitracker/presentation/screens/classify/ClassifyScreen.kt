package com.prafullk.upitracker.presentation.screens.classify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.EmptyState
import com.prafullk.upitracker.presentation.screens.transactions.GroupSelectionSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifyScreen(onNavigateBack: () -> Unit, viewModel: ClassifyViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Classify Transactions") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                )
            }
    ) { padding ->
        val transactions = uiState.unclassifiedTransactions

        if (transactions.isEmpty()) {
            EmptyState(
                    message = "All caught up!\nNo unclassified transactions.",
                    modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        val index = uiState.currentTransactionIndex
        if (index !in transactions.indices) return@Scaffold

        val currentTx = transactions[index]

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val progress = (index + 1).toFloat() / transactions.size
            LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "${index + 1} of ${transactions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(32.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    AmountText(
                            amount = currentTx.amount,
                            direction = currentTx.direction,
                            style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("To / From", style = MaterialTheme.typography.labelSmall)
                    Text(
                            currentTx.contactName ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium
                    )
                    if (currentTx.upiId != null) {
                        Text(
                                currentTx.upiId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Raw SMS/Notification", style = MaterialTheme.typography.labelSmall)
                    Text(
                            currentTx.rawDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.skipCurrent() }) { Text("Skip") }

                Button(onClick = { showGroupSheet = true }) { Text("Select Category") }
            }
        }

        // Setup Bottom Sheet for grouping
        if (showGroupSheet) {
            GroupSelectionSheet(
                    groups = uiState.allGroups,
                    onDismiss = { showGroupSheet = false },
                    onGroupSelected = { group ->
                        viewModel.classifyCurrent(group.id)
                        showGroupSheet = false
                    }
            )
        }
    }
}
