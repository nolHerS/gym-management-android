package com.imanol.gymmanagement.feature.exercise.presentation

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
import com.imanol.gymmanagement.feature.exercise.domain.Exercise

@Composable
fun ExercisesScreen(
    categoryId: Long,
    viewModel: ExercisesViewModel,
    onExerciseSelected: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId) {
        viewModel.loadExercises(categoryId)
    }
    LaunchedEffect(uiState) {
        if (uiState is ExercisesUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Ejercicios",
            style = MaterialTheme.typography.headlineSmall,
        )
        when (val state = uiState) {
            ExercisesUiState.Loading -> GymLoading(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is ExercisesUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseItem(exercise, onClick = { onExerciseSelected(exercise.id) })
                }
            }
            ExercisesUiState.Empty -> Text(text = "No hay ejercicios disponibles.")
            is ExercisesUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(text = "Reintentar", onClick = { viewModel.loadExercises(categoryId) })
            }
            ExercisesUiState.Unauthorized -> Unit
        }
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    GymCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (exercise.active) "Activo" else "Inactivo",
                color = if (exercise.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
