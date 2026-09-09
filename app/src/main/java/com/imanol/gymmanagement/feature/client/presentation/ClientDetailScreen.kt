package com.imanol.gymmanagement.feature.client.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading

@Composable
fun ClientDetailScreen(
    clientId: Long,
    viewModel: ClientDetailViewModel,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }
    LaunchedEffect(uiState) {
        if (uiState is ClientDetailUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Detalle del cliente", style = MaterialTheme.typography.headlineSmall)
        when (val state = uiState) {
            ClientDetailUiState.Loading -> GymLoading()
            is ClientDetailUiState.Success -> {
                Text(text = state.client.fullName, style = MaterialTheme.typography.titleLarge)
                Text(text = "Email: ${state.client.email}")
                Text(text = "Rol: ${state.client.role}")
                Text(text = if (state.client.active) "Activo" else "Inactivo")
            }
            is ClientDetailUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(text = "Reintentar", onClick = { viewModel.loadClient(clientId) })
            }
            ClientDetailUiState.Unauthorized -> Unit
        }
    }
}
