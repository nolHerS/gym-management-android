package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
fun CreateWorkoutPlanScreen(clientId: Long, viewModel: WorkoutPlanViewModel, onCreated: (Long) -> Unit) {
    val state by viewModel.createState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.prepareCreate() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Crear plan", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(state.startDate, viewModel::setStartDate, label = { Text("Inicio (AAAA-MM-DD)") })
        OutlinedTextField(state.endDate, viewModel::setEndDate, label = { Text("Fin (opcional)") })
        Text("Plantilla", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = viewModel::selectFromScratch) { Text("Crear desde cero") }
        state.templates.forEach { template ->
            OutlinedButton(
                onClick = { viewModel.selectTemplate(template) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.template?.id == template.id) "${template.name} (seleccionada)" else template.name)
            }
        }
        state.templateDetail?.exercises?.forEach { templateExercise ->
            val request = state.drafts[templateExercise.id] ?: return@forEach
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
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !state.saving, onClick = { viewModel.create(clientId, onCreated) }) { Text("Guardar") }
    }
}

@Composable
fun WorkoutPlanDetailScreen(planId: Long, viewModel: WorkoutPlanViewModel, onUnauthorized: () -> Unit) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    LaunchedEffect(planId) { viewModel.loadDetail(planId) }
    LaunchedEffect(state) {
        if (state is WorkoutPlanDetailState.Unauthorized) onUnauthorized()
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalle del plan", style = MaterialTheme.typography.headlineSmall)
        when (val s = state) {
            WorkoutPlanDetailState.Loading -> CircularProgressIndicator()
            is WorkoutPlanDetailState.Error -> Text(s.message)
            WorkoutPlanDetailState.Unauthorized -> Unit
            is WorkoutPlanDetailState.Success -> { Text("Estado: ${s.plan.status}"); Text("Desde: ${s.plan.startDate}"); s.plan.days.forEach { day -> Text("Día ${day.dayOfWeek}: ${day.exercises.size} ejercicios") } }
        }
    }
}
