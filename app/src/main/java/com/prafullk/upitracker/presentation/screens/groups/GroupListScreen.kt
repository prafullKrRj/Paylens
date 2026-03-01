package com.prafullk.upitracker.presentation.screens.groups

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                GroupCard(item, onClick = { onNavigateToGroupDetail(item.group.id) })
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
                    GroupCard(item, onClick = { onNavigateToGroupDetail(item.group.id) })
                }
            }
        }
    }
}

@Composable
fun GroupCard(item: GroupWithSpending, onClick: () -> Unit) {
    val ratio = if (item.budget != null && item.budget > 0.0) {
        (item.currentSpend / item.budget).toFloat().coerceIn(0f, 1f)
    } else 0f

    val groupColor = Color(item.group.color)
    val isOverBudget = ratio >= 1f

    Card(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Budget fill watermark using Canvas
            if (ratio > 0f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val fillWidth = size.width * ratio
                    drawRoundRect(
                            color = groupColor.copy(alpha = 0.12f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
            }

            Column(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    GroupChip(name = item.group.name, color = item.group.color)
                    AmountText(amount = item.currentSpend, direction = "DEBIT")
                }

                if (item.budget != null && item.budget > 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                                text = "${(ratio * 100).toInt()}% of ₹${item.budget.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOverBudget) {
                            Text(
                                    text = "Over budget!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

