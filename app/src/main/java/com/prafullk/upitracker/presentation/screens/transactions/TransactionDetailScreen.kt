package com.prafullk.upitracker.presentation.screens.transactions

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.domain.model.Group
import com.prafullk.upitracker.presentation.components.AmountText
import com.prafullk.upitracker.presentation.components.EntityAvatar
import com.prafullk.upitracker.presentation.components.GroupChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
        transactionId: String,
        onNavigateBack: () -> Unit,
        onNavigateToEntity: (String) -> Unit,
        viewModel: TransactionDetailViewModel = koinViewModel { parametersOf(transactionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Transaction Details") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { padding ->
        val tx = uiState.transaction

        if (tx == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (uiState.isLoading) "Loading..." else "Transaction not found")
            }
            return@Scaffold
        }

        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AmountText(
                    amount = tx.amount,
                    direction = tx.direction,
                    style = MaterialTheme.typography.displayMedium
            )

            Text(
                    text = "${tx.direction} • ${tx.status}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val df = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            Text(text = df.format(Date(tx.timestamp)), style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // To / From section
            val contactName = tx.contactName ?: "Unknown Entity"
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { tx.entityId?.let(onNavigateToEntity) }
                                    .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EntityAvatar(name = contactName, color = 0xFF9E9E9E.toInt())
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sent To", style = MaterialTheme.typography.labelSmall)
                    Text(text = contactName, style = MaterialTheme.typography.bodyLarge)
                    if (tx.upiId != null) {
                        Text(
                                text = tx.upiId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (tx.entityId != null) {
                    Text(
                            "View →",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            HorizontalDivider()

            // Category section
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { showGroupSheet = true }
                                    .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = "Category (Tap to change)",
                            style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GroupChip(
                            name = uiState.currentGroupName ?: "Uncategorized",
                            color = 0xFFE0E0E0.toInt()
                    )
                }
                Text(
                        "Edit →",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                )
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(32.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = "Detection Info",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = "Source: ${tx.sourceApp ?: tx.source}",
                        style = MaterialTheme.typography.bodySmall
                )
                Text(
                        text = "Raw Text Captured: ${tx.rawDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showGroupSheet) {
            GroupSelectionSheet(
                    groups = uiState.allGroups,
                    onDismiss = { showGroupSheet = false },
                    onGroupSelected = {
                        viewModel.updateGroup(it.id)
                        showGroupSheet = false
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionSheet(
        groups: List<Group>,
        onDismiss: () -> Unit,
        onGroupSelected: (Group) -> Unit
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text(
                        "Select Category",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(groups.size) { index ->
                val group = groups[index]
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { onGroupSelected(group) }
                                        .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) { GroupChip(name = group.name, color = group.color) }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}
