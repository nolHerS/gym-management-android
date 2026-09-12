package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest

data class WorkoutPlanExerciseEditorInitialState(
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val sourceTemplateExerciseId: Long? = null,
    val sets: String = "",
    val repetitions: String = "",
    val restSeconds: String = "",
    val orderIndex: String = "",
)

@Composable
fun WorkoutPlanDaySelectorDialog(
    title: String,
    availableDays: List<Int> = (1..7).toList(),
    dayLabel: (Int) -> String,
    onDismiss: () -> Unit,
    onDaySelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                availableDays.forEach { day ->
                    TextButton(
                        onClick = { onDaySelected(day) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(dayLabel(day)) }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun WorkoutPlanExerciseEditorDialog(
    title: String,
    catalog: WorkoutPlanExerciseCatalogState,
    isSubmitting: Boolean,
    initialState: WorkoutPlanExerciseEditorInitialState = WorkoutPlanExerciseEditorInitialState(),
    onLoadCatalog: () -> Unit,
    onLoadCategory: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (WorkoutPlanExerciseRequest, String?) -> Unit,
) {
    var selectedExerciseId by remember(initialState) { mutableStateOf(initialState.exerciseId) }
    var selectedExerciseName by remember(initialState) { mutableStateOf(initialState.exerciseName) }
    var sets by remember(initialState) { mutableStateOf(initialState.sets) }
    var repetitions by remember(initialState) { mutableStateOf(initialState.repetitions) }
    var rest by remember(initialState) { mutableStateOf(initialState.restSeconds) }
    var order by remember(initialState) { mutableStateOf(initialState.orderIndex) }
    var error by remember(initialState) { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { onLoadCatalog() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    when (catalog) {
                        WorkoutPlanExerciseCatalogState.Idle,
                        WorkoutPlanExerciseCatalogState.Loading -> CircularProgressIndicator()
                        is WorkoutPlanExerciseCatalogState.Error -> {
                            Text(catalog.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onLoadCatalog) { Text("Reintentar") }
                        }
                        is WorkoutPlanExerciseCatalogState.Categories -> {
                            Text("Categoría")
                            catalog.categories.forEach { category ->
                                TextButton(onClick = { onLoadCategory(category.id) }) { Text(category.name) }
                            }
                        }
                        is WorkoutPlanExerciseCatalogState.Success -> {
                            Text("Ejercicio")
                            catalog.exercises.forEach { exercise ->
                                TextButton(onClick = {
                                    selectedExerciseId = exercise.id
                                    selectedExerciseName = exercise.name
                                }) {
                                    Text(
                                        if (selectedExerciseId == exercise.id) {
                                            "${exercise.name} (seleccionado)"
                                        } else {
                                            exercise.name
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    ExerciseFormFields(
                        sets = sets,
                        onSetsChange = { sets = it },
                        repetitions = repetitions,
                        onRepetitionsChange = { repetitions = it },
                        restSeconds = rest,
                        onRestSecondsChange = { rest = it },
                        orderIndex = order,
                        onOrderIndexChange = { order = it },
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val request = WorkoutPlanExerciseRequest(
                        exerciseId = selectedExerciseId,
                        sourceTemplateExerciseId = initialState.sourceTemplateExerciseId,
                        orderIndex = order.toIntOrNull() ?: 0,
                        sets = sets.toIntOrNull() ?: 0,
                        repetitions = repetitions.toIntOrNull() ?: 0,
                        restSeconds = rest.toIntOrNull() ?: -1,
                    )
                    val hasExerciseReference = request.exerciseId != null ||
                        request.sourceTemplateExerciseId != null
                    if (!hasExerciseReference || request.orderIndex < 1 || request.sets < 1 ||
                        request.repetitions < 1 || request.restSeconds < 0
                    ) {
                        error = "Los datos del ejercicio no son válidos."
                    } else {
                        onSubmit(request, selectedExerciseName)
                    }
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ExerciseFormFields(
    sets: String,
    onSetsChange: (String) -> Unit,
    repetitions: String,
    onRepetitionsChange: (String) -> Unit,
    restSeconds: String,
    onRestSecondsChange: (String) -> Unit,
    orderIndex: String,
    onOrderIndexChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        OutlinedTextField(sets, onSetsChange, label = { Text("Series") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            repetitions,
            onRepetitionsChange,
            label = { Text("Repeticiones") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                restSeconds,
                onRestSecondsChange,
                label = { Text("Descanso") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                orderIndex,
                onOrderIndexChange,
                label = { Text("Orden") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun WorkoutPlanDayChips(
    selectedDay: Int?,
    onSelectDay: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..7).forEach { day ->
            FilterChip(
                selected = selectedDay == day,
                onClick = { onSelectDay(day) },
                label = { Text(day.toString()) },
            )
        }
    }
}
