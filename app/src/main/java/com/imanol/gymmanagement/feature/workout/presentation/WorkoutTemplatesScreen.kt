package com.imanol.gymmanagement.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate

@Composable
fun WorkoutTemplatesScreen(
    viewModel: WorkoutTemplatesViewModel,
    onTemplateSelected: (Long) -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }
    LaunchedEffect(uiState) {
        if (uiState is WorkoutTemplatesUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Plantillas de entrenamiento",
                style = MaterialTheme.typography.headlineSmall,
            )
            GymButton(text = "Crear", onClick = onCreateTemplate)
        }
        when (val state = uiState) {
            WorkoutTemplatesUiState.Loading -> GymLoading(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is WorkoutTemplatesUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.templates, key = { it.id }) { template ->
                    WorkoutTemplateItem(
                        template = template,
                        onClick = { onTemplateSelected(template.id) },
                        onEdit = { onEditTemplate(template.id) },
                        onToggleActive = { viewModel.setActive(template) },
                    )
                }
            }
            WorkoutTemplatesUiState.Empty -> Text(text = "No hay plantillas disponibles.")
            is WorkoutTemplatesUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(text = "Reintentar", onClick = viewModel::loadTemplates)
            }
            WorkoutTemplatesUiState.Unauthorized -> Unit
        }
    }
}

@Composable
private fun WorkoutTemplateItem(
    template: WorkoutTemplate,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
) {
    GymCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = template.name, style = MaterialTheme.typography.titleMedium)
            template.description?.let { Text(text = it) }
            Text(text = if (template.active) "Activa" else "Inactiva")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymButton(text = "Editar", onClick = onEdit)
                GymButton(
                    text = if (template.active) "Desactivar" else "Activar",
                    onClick = onToggleActive,
                )
            }
        }
    }
}
