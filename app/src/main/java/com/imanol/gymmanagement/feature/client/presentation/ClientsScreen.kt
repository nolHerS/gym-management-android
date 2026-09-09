package com.imanol.gymmanagement.feature.client.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading
import com.imanol.gymmanagement.feature.client.domain.Client

@Composable
fun ClientsScreen(
    viewModel: ClientsViewModel,
    onClientSelected: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadClients()
    }
    LaunchedEffect(uiState) {
        if (uiState is ClientsUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Mis clientes", style = MaterialTheme.typography.headlineSmall)
        when (val state = uiState) {
            ClientsUiState.Loading -> GymLoading(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is ClientsUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.clients, key = { it.id }) { client ->
                    ClientItem(client, onClick = { onClientSelected(client.id) })
                }
            }
            ClientsUiState.Empty -> Text(text = "No tienes clientes asignados.")
            is ClientsUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(text = "Reintentar", onClick = viewModel::loadClients)
            }
            ClientsUiState.Unauthorized -> Unit
        }
    }
}

@Composable
private fun ClientItem(
    client: Client,
    onClick: () -> Unit,
) {
    GymCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = client.fullName, style = MaterialTheme.typography.titleMedium)
            Text(text = client.email)
            Text(
                text = if (client.active) "Activo" else "Inactivo",
                color = if (client.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
