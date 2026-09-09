package com.imanol.gymmanagement.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.imanol.gymmanagement.core.designsystem.component.GymTextField

@Composable
fun WorkoutTemplateFormScreen(
    templateId: Long?,
    viewModel: WorkoutTemplatesViewModel,
    onSaved: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) {
        if (templateId == null) {
            viewModel.prepareCreate()
        } else {
            viewModel.loadTemplateForEdit(templateId)
        }
    }
    LaunchedEffect(formState.saved) {
        if (formState.saved) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (templateId == null) "Crear plantilla" else "Editar plantilla",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (formState.isLoading) {
            GymLoading()
        } else {
            GymTextField(
                value = formState.name,
                onValueChange = viewModel::updateName,
                modifier = Modifier.fillMaxWidth(),
                label = "Nombre",
                isError = formState.errorMessage != null,
            )
            GymTextField(
                value = formState.description,
                onValueChange = viewModel::updateDescription,
                modifier = Modifier.fillMaxWidth(),
                label = "Descripción",
                singleLine = false,
            )
            formState.errorMessage?.let { message ->
                GymErrorMessage(message = message)
                if (message == "No autorizado.") {
                    LaunchedEffect(Unit) {
                        onUnauthorized()
                    }
                }
            }
            GymButton(
                text = "Guardar",
                onClick = { viewModel.saveTemplate(templateId) },
                loading = formState.isSaving,
            )
        }
    }
}
