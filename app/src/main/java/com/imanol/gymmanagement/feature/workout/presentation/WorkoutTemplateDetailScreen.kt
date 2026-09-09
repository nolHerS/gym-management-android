package com.imanol.gymmanagement.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading
import com.imanol.gymmanagement.core.designsystem.component.GymTextField
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateExercise

@Composable
fun WorkoutTemplateDetailScreen(
    templateId: Long,
    viewModel: WorkoutTemplateDetailViewModel,
    onEditTemplate: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var showCatalog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<WorkoutTemplateExercise?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    LaunchedEffect(templateId) {
        viewModel.loadDetail(templateId)
    }
    LaunchedEffect(uiState) {
        if (uiState is WorkoutTemplateDetailUiState.Unauthorized) onUnauthorized()
    }

    when (val state = uiState) {
        WorkoutTemplateDetailUiState.Loading -> GymLoading()
        is WorkoutTemplateDetailUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GymErrorMessage(message = state.message)
            GymButton(text = "Reintentar", onClick = { viewModel.loadDetail(templateId) })
        }
        WorkoutTemplateDetailUiState.Empty,
        WorkoutTemplateDetailUiState.Unauthorized -> Unit
        is WorkoutTemplateDetailUiState.Success -> {
            val detail = state.detail
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(detail.template.name)
                detail.template.description?.let { Text(it) }
                Text(if (detail.template.active) "Activa" else "Inactiva")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymButton(
                        text = "Editar plantilla",
                        onClick = { onEditTemplate(templateId) },
                    )
                    GymButton(
                        text = "Añadir ejercicio",
                        onClick = {
                            showCatalog = true
                            viewModel.loadExerciseCatalog()
                        },
                    )
                }
                actionError?.let { GymErrorMessage(message = it) }
                if (detail.exercises.isEmpty()) {
                    Text("No hay ejercicios en esta plantilla.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(detail.exercises, key = { it.id }) { item ->
                            WorkoutTemplateExerciseItem(
                                item = item,
                                onEdit = { exerciseToEdit = item },
                                onDelete = {
                                    viewModel.deleteExercise(templateId, item.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCatalog) {
        ExerciseCatalogDialog(
            state = catalogState,
            onDismiss = { showCatalog = false },
            onSelected = {
                selectedExercise = it
                showCatalog = false
            },
        )
    }

    selectedExercise?.let { exercise ->
        WorkoutTemplateExerciseDialog(
            exercise = exercise,
            existing = null,
            onDismiss = { selectedExercise = null },
            onSubmit = { order, sets, repetitions, rest ->
                viewModel.addExercise(
                    templateId,
                    exercise.id,
                    order,
                    sets,
                    repetitions,
                    rest,
                )
                selectedExercise = null
            },
        )
    }

    exerciseToEdit?.let { item ->
        WorkoutTemplateExerciseDialog(
            exercise = item.exercise,
            existing = item,
            onDismiss = { exerciseToEdit = null },
            onSubmit = { order, sets, repetitions, rest ->
                viewModel.updateExercise(
                    templateId,
                    item.id,
                    item.exercise.id,
                    order,
                    sets,
                    repetitions,
                    rest,
                )
                exerciseToEdit = null
            },
        )
    }
}

@Composable
private fun WorkoutTemplateExerciseItem(
    item: WorkoutTemplateExercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    GymCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(item.exercise.name)
            Text("Orden: ${item.orderIndex}")
            Text("Series: ${item.sets}")
            Text("Repeticiones: ${item.repetitions}")
            Text("Descanso: ${item.restSeconds} segundos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}

@Composable
private fun ExerciseCatalogDialog(
    state: ExerciseCatalogUiState,
    onDismiss: () -> Unit,
    onSelected: (Exercise) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text("Seleccionar ejercicio") },
        text = {
            when (state) {
                ExerciseCatalogUiState.Idle,
                ExerciseCatalogUiState.Loading -> GymLoading()
                is ExerciseCatalogUiState.Success -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.exercises.forEach { exercise ->
                        TextButton(onClick = { onSelected(exercise) }) {
                            Text(exercise.name)
                        }
                    }
                }
                is ExerciseCatalogUiState.Error -> GymErrorMessage(message = state.message)
            }
        },
    )
}

@Composable
private fun WorkoutTemplateExerciseDialog(
    exercise: Exercise,
    existing: WorkoutTemplateExercise?,
    onDismiss: () -> Unit,
    onSubmit: (orderIndex: Int, sets: Int, repetitions: Int, restSeconds: Int) -> Unit,
) {
    var orderIndex by remember(existing?.id) {
        mutableStateOf(existing?.orderIndex?.toString() ?: "1")
    }
    var sets by remember(existing?.id) {
        mutableStateOf(existing?.sets?.toString() ?: "1")
    }
    var repetitions by remember(existing?.id) {
        mutableStateOf(existing?.repetitions?.toString() ?: "1")
    }
    var restSeconds by remember(existing?.id) {
        mutableStateOf(existing?.restSeconds?.toString() ?: "0")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            TextButton(onClick = {
                val order = orderIndex.toIntOrNull()
                val setsValue = sets.toIntOrNull()
                val repetitionsValue = repetitions.toIntOrNull()
                val restValue = restSeconds.toIntOrNull()
                if (order == null || order < 1 ||
                    setsValue == null || setsValue < 1 ||
                    repetitionsValue == null || repetitionsValue < 1 ||
                    restValue == null || restValue < 0
                ) {
                    error = "Revisa los valores según las reglas del servidor."
                } else {
                    onSubmit(order, setsValue, repetitionsValue, restValue)
                }
            }) {
                Text("Guardar")
            }
        },
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GymTextField(value = orderIndex, onValueChange = { orderIndex = it }, label = "Orden")
                GymTextField(value = sets, onValueChange = { sets = it }, label = "Series")
                GymTextField(
                    value = repetitions,
                    onValueChange = { repetitions = it },
                    label = "Repeticiones",
                )
                GymTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    label = "Descanso (segundos)",
                )
                error?.let { GymErrorMessage(message = it) }
            }
        },
    )
}
