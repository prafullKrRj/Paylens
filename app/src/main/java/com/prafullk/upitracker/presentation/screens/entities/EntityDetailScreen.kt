package com.prafullk.upitracker.presentation.screens.entities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.prafullk.upitracker.presentation.components.EntityAvatar
import com.prafullk.upitracker.presentation.components.TransactionCard
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityDetailScreen(
        entityId: String,
        onNavigateBack: () -> Unit,
        onNavigateToTransactionDetail: (String) -> Unit,
        viewModel: EntityDetailViewModel = koinViewModel { parametersOf(entityId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(uiState.entity?.displayName ?: "Entity Details") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { padding ->
        val entity = uiState.entity

        if (entity == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (uiState.isLoading) "Loading..." else "Entity not found")
            }
            return@Scaffold
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                EntityAvatar(
                        name = entity.displayName,
                        color = entity.avatarColor,
                        modifier = Modifier.padding(16.dp)
                )

                Text(text = entity.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(text = entity.type, style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("UPI IDs Linked", style = MaterialTheme.typography.labelLarge)
                    if (entity.upiIds.isEmpty()) {
                        Text("None", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        entity.upiIds.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "Transaction History",
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
                        entityName = entity.displayName,
                        entityColor = entity.avatarColor,
                        groupName = null, // Can map the group later
                        groupColor = null,
                        onClick = { onNavigateToTransactionDetail(tx.id) }
                )
            }
        }
    }
}
