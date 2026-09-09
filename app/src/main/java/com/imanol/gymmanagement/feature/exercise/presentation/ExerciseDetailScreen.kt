package com.imanol.gymmanagement.feature.exercise.presentation

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
fun ExerciseDetailScreen(
    exerciseId: Long,
    viewModel: ExerciseDetailViewModel,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }
    LaunchedEffect(uiState) {
        if (uiState is ExerciseDetailUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Detalle del ejercicio",
            style = MaterialTheme.typography.headlineSmall,
        )
        when (val state = uiState) {
            ExerciseDetailUiState.Loading -> GymLoading()
            is ExerciseDetailUiState.Success -> {
                Text(text = state.exercise.name, style = MaterialTheme.typography.titleLarge)
                state.exercise.description?.let { Text(text = it) }
                Text(text = "Categoría: ${state.exercise.categoryName}")
                Text(text = if (state.exercise.active) "Activo" else "Inactivo")
            }
            is ExerciseDetailUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(
                    text = "Reintentar",
                    onClick = { viewModel.loadExercise(exerciseId) },
                )
            }
            ExerciseDetailUiState.Unauthorized -> Unit
        }
    }
}
