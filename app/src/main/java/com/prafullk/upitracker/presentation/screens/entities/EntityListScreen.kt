package com.prafullk.upitracker.presentation.screens.entities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prafullk.upitracker.domain.model.TrackedEntity
import com.prafullk.upitracker.presentation.components.EmptyState
import com.prafullk.upitracker.presentation.components.EntityAvatar
import org.koin.androidx.compose.koinViewModel

@Composable
fun EntityListScreen(
        onNavigateToEntityDetail: (String) -> Unit,
        viewModel: EntityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val allEntities = uiState.people + uiState.merchants

            if (allEntities.isEmpty()) {
                EmptyState(
                        message = "No people or merchants tracked yet",
                        modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (uiState.people.isNotEmpty()) {
                        item {
                            Text(
                                    "People",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.people, key = { it.id }) { entity ->
                            EntityRow(entity, onClick = { onNavigateToEntityDetail(entity.id) })
                        }
                    }

                    if (uiState.merchants.isNotEmpty()) {
                        item {
                            Text(
                                    "Merchants & Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.merchants, key = { it.id }) { entity ->
                            EntityRow(entity, onClick = { onNavigateToEntityDetail(entity.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntityRow(entity: TrackedEntity, onClick: () -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        EntityAvatar(name = entity.displayName, color = entity.avatarColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entity.displayName, style = MaterialTheme.typography.bodyLarge)
            val upiCount = entity.upiIds.size
            if (upiCount > 0) {
                Text(
                        text = "$upiCount UPI ID(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
