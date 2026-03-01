package com.prafullk.upitracker.presentation.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.GroupChip
import org.koin.androidx.compose.koinViewModel

@Composable
fun GroupListScreen(
        onNavigateToGroupDetail: (String) -> Unit,
        viewModel: GroupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                )
            }

            items(uiState.topGroups, key = { it.group.id }) { item ->
                GroupRow(item, onClick = { onNavigateToGroupDetail(item.group.id) })
            }

            if (uiState.otherGroups.isNotEmpty()) {
                item {
                    Text(
                            text = "Other Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                    )
                }

                items(uiState.otherGroups, key = { it.group.id }) { item ->
                    GroupRow(item, onClick = { onNavigateToGroupDetail(item.group.id) })
                }
            }
        }
    }
}

@Composable
fun GroupRow(item: GroupWithSpending, onClick: () -> Unit) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GroupChip(name = item.group.name, color = item.group.color)
            AmountText(amount = item.currentSpend, direction = "DEBIT")
        }

        Spacer(Modifier.height(8.dp))

        if (item.budget != null && item.budget > 0.0) {
            val ratio = (item.currentSpend / item.budget).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color =
                            if (ratio > 0.9f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                    text = "${(ratio * 100).toInt()}% of budget (₹${item.budget})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
