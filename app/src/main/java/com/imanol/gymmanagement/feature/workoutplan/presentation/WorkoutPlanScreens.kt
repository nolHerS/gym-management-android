package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise

@Composable
fun WorkoutPlansScreen(clientId: Long, viewModel: WorkoutPlanViewModel, onPlanSelected: (Long) -> Unit, onUnauthorized: () -> Unit, onCreate: () -> Unit = {}) {
    val state by viewModel.plans.collectAsStateWithLifecycle()
    LaunchedEffect(clientId) { viewModel.load(clientId) }
    if (state is WorkoutPlansState.Unauthorized) LaunchedEffect(Unit) { onUnauthorized() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Planes de entrenamiento", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onCreate) { Text("Crear plan") }
        when (val s = state) {
            WorkoutPlansState.Loading -> CircularProgressIndicator()
            WorkoutPlansState.Empty -> Text("No hay planes para este cliente.")
            is WorkoutPlansState.Success -> LazyColumn {
                items(s.plans, key = { it.id }) { plan ->
                    OutlinedButton(
                        onClick = { onPlanSelected(plan.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            buildString {
                                append("${plan.startDate} · ${plan.status}")
                                plan.endDate?.let { append(" · $it") }
                            },
                        )
                    }
                }
            }
            is WorkoutPlansState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.load(clientId) }) { Text("Reintentar") }
            }
            WorkoutPlansState.Unauthorized -> Unit
        }

    }
}

@Composable
fun WorkoutPlanFormScreen(clientId: Long, planId: Long?, viewModel: WorkoutPlanViewModel, onSaved: (Long) -> Unit) {
    val state by viewModel.createState.collectAsStateWithLifecycle()
    LaunchedEffect(planId) {
        if (planId == null) viewModel.prepareCreate() else viewModel.prepareEdit(planId)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(if (planId == null) "Crear plan" else "Editar plan", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                state.startDate,
                viewModel::setStartDate,
                label = { Text("Inicio (AAAA-MM-DD)") },
            )
            OutlinedTextField(
                state.endDate,
                viewModel::setEndDate,
                label = { Text("Fin (opcional)") },
            )
            if (planId == null) {
                Text("Plantilla", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::selectFromScratch) { Text("Crear desde cero") }
            }
        }
        if (planId == null) items(state.templates, key = { "template-${it.id}" }) { template ->
            OutlinedButton(
                onClick = { viewModel.selectTemplate(template) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.template?.id == template.id) "${template.name} (seleccionada)" else template.name)
            }
        }
        if (planId == null) state.templateDetail?.exercises?.forEach { templateExercise ->
            state.drafts[templateExercise.id]?.let { request ->
                item(key = "exercise-${templateExercise.id}") {
                    Text(templateExercise.exercise.name, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { day ->
                            FilterChip(
                                selected = state.assignments[templateExercise.id] == day,
                                onClick = { viewModel.assignExercise(templateExercise.id, day) },
                                label = { Text(day.toString()) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = request.sets.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    viewModel.updateExercise(
                                        templateExercise.id,
                                        request.copy(sets = it),
                                    )
                                }
                            },
                            label = { Text("Series") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = request.repetitions.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    viewModel.updateExercise(
                                        templateExercise.id,
                                        request.copy(repetitions = it),
                                    )
                                }
                            },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = request.restSeconds.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    viewModel.updateExercise(
                                        templateExercise.id,
                                        request.copy(restSeconds = it),
                                    )
                                }
                            },
                            label = { Text("Descanso") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            if (state.loading) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !state.loading && !state.saving,
                onClick = {
                    if (planId == null) viewModel.create(clientId, onSaved)
                    else viewModel.update(onSaved)
                },
            ) { Text("Guardar") }
        }
    }
}

@Composable
fun WorkoutPlanDetailScreen(
    planId: Long,
    viewModel: WorkoutPlanViewModel,
    structureViewModel: WorkoutPlanStructureViewModel,
    canManage: Boolean,
    onEdit: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    val structureState by structureViewModel.state.collectAsStateWithLifecycle()
    val mutation by viewModel.mutation.collectAsStateWithLifecycle()
    var confirmation by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(planId) { structureViewModel.load(planId) }
    LaunchedEffect(structureState) {
        if (structureState is WorkoutPlanStructureState.Success) {
            viewModel.setDetailPlan((structureState as WorkoutPlanStructureState.Success).plan)
        }
    }
    LaunchedEffect(state) {
        if (state is WorkoutPlanDetailState.Unauthorized) onUnauthorized()
    }
    if (confirmation != null) {
        val complete = confirmation == "complete"
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (complete) "Completar plan" else "Desactivar plan") },
            text = {
                Text(if (complete) "¿Quieres marcar este plan como completado?" else "¿Quieres desactivar este plan?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        if (complete) viewModel.complete(planId) else viewModel.deactivate(planId)
                    },
                ) { Text(if (complete) "Completar" else "Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) { Text("Cancelar") }
            },
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Detalle del plan", style = MaterialTheme.typography.headlineSmall) }
        when (val structure = structureState) {
            WorkoutPlanStructureState.Idle,
            WorkoutPlanStructureState.Loading -> item { CircularProgressIndicator() }
            is WorkoutPlanStructureState.Error -> item {
                Text(structure.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = structureViewModel::retry) { Text("Reintentar") }
            }
            is WorkoutPlanStructureState.Success -> {
                val plan = (state as? WorkoutPlanDetailState.Success)
                    ?.takeIf { it.plan.id == planId }
                    ?.plan
                    ?: structure.plan
                item {
                    Text("Estado: ${plan.status}")
                    Text("Desde: ${plan.startDate}")
                    plan.endDate?.let { Text("Hasta: $it") }
                }
                item {
                    if (canManage && plan.status == "ACTIVE") {
                    val mutating = mutation is WorkoutPlanMutationState.Completing ||
                        mutation is WorkoutPlanMutationState.Deactivating
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !mutating, onClick = { onEdit(plan.clientId) }) {
                            Text("Editar")
                        }
                        Button(enabled = !mutating, onClick = { confirmation = "complete" }) {
                            Text("Completar")
                        }
                        OutlinedButton(enabled = !mutating, onClick = { confirmation = "deactivate" }) {
                            Text("Desactivar")
                        }
                    }
                }
                }
                item {
                    when (val operation = mutation) {
                        is WorkoutPlanMutationState.Success -> Text(operation.message)
                        is WorkoutPlanMutationState.Error -> Text(
                            operation.message,
                            color = MaterialTheme.colorScheme.error,
                        )
                        WorkoutPlanMutationState.Completing,
                        WorkoutPlanMutationState.Deactivating -> CircularProgressIndicator()
                        WorkoutPlanMutationState.Idle -> Unit
                    }
                }
                items(
                    plan.days.sortedBy { it.dayOfWeek },
                    key = { it.id },
                ) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(dayName(day.dayOfWeek), style = MaterialTheme.typography.titleLarge)
                        if (day.exercises.isEmpty()) {
                            Text("Sin ejercicios")
                        } else {
                            day.exercises
                                .sortedBy { it.orderIndex }
                                .forEach { exercise ->
                                    WorkoutPlanExerciseCard(exercise)
                                }
                        }
                    }
                }
            }
        }
    }
}

fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> "Día $dayOfWeek"
}

@Composable
private fun WorkoutPlanExerciseCard(exercise: WorkoutPlanExercise) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(exercise.exercise?.name ?: "Ejercicio no disponible")
            exercise.exercise?.categoryName
                ?.takeIf { it.isNotBlank() }
                ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("${exercise.sets} series × ${exercise.repetitions} repeticiones")
            Text("Descanso: ${exercise.restSeconds} s")
            Text("Orden: ${exercise.orderIndex}")
        }
    }
}
