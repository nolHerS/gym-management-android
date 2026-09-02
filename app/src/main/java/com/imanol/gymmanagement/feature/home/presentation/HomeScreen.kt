package com.imanol.gymmanagement.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Unauthorized) onLogout()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            HomeUiState.Loading -> GymLoading()
            is HomeUiState.Success -> {
                Text(text = "Bienvenido, ${state.user.name}")
                Text(text = "Email: ${state.user.email}")
                Text(text = "Rol: ${state.user.role}")
            }
            is HomeUiState.Error -> {
                GymErrorMessage(message = state.message)
                Button(onClick = viewModel::loadUser) {
                    Text(text = "Reintentar")
                }
            }
            HomeUiState.Unauthorized -> Unit
        }
        Button(onClick = onLogout) {
            Text(text = "Cerrar sesión")
        }
    }
}
