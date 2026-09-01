package com.imanol.gymmanagement.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymPasswordField
import com.imanol.gymmanagement.core.designsystem.component.GymTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.success) {
        if (uiState.success != null) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Login")
        Spacer(modifier = Modifier.height(16.dp))
        GymTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            modifier = Modifier.fillMaxWidth(),
            label = "Email",
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        GymPasswordField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            modifier = Modifier.fillMaxWidth(),
            label = "Contraseña",
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
        )
        Spacer(modifier = Modifier.height(16.dp))
        GymButton(
            text = "Iniciar sesión",
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth(),
            loading = uiState.isLoading,
        )
        uiState.errorMessage?.let { errorMessage ->
            Spacer(modifier = Modifier.height(12.dp))
            GymErrorMessage(message = errorMessage)
        }
        uiState.success?.let { success ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Login correcto")
            Text(text = "Tipo de token: ${success.tokenType}")
            Text(text = "Expira en: ${success.expiresIn} ms")
        }
    }
}
