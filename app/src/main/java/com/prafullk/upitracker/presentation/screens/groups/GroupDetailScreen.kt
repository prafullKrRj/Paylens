package com.prafullk.upitracker.presentation.screens.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.GroupChip
import com.prafullk.upitracker.presentation.components.TransactionCard
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
        groupId: String,
        onNavigateBack: () -> Unit,
        onNavigateToTransactionDetail: (String) -> Unit,
        viewModel: GroupDetailViewModel = koinViewModel { parametersOf(groupId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(uiState.group?.name ?: "Category Details") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { padding ->
        val group = uiState.group

        if (group == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (uiState.isLoading) "Loading..." else "Category not found")
            }
            return@Scaffold
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                GroupChip(
                        name = group.name,
                        color = group.color,
                        modifier = Modifier.padding(16.dp)
                )

                Text(text = "Total Spent This Month", style = MaterialTheme.typography.labelMedium)
                AmountText(
                        amount = uiState.totalSpend,
                        direction = "DEBIT",
                        style = MaterialTheme.typography.displayMedium
                )

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()

                Text(
                        text = "Transactions in ${group.name}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                )
            }

            items(
                    count = uiState.transactions.size,
                    key = { index -> uiState.transactions[index].id }
            ) { index ->
                val tx = uiState.transactions[index]
                TransactionCard(
                        transaction = tx,
                        entityName = tx.contactName, // Maps to entity later
                        entityColor = null,
                        groupName = group.name,
                        groupColor = group.color,
                        onClick = { onNavigateToTransactionDetail(tx.id) }
                )
            }
        }
    }
}
