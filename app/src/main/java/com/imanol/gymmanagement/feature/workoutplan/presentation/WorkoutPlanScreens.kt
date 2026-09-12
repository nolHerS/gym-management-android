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
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest

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
    val structureMutation by structureViewModel.mutation.collectAsStateWithLifecycle()
    val mutation by viewModel.mutation.collectAsStateWithLifecycle()
    var confirmation by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<ExerciseEditorTarget?>(null) }
    var daySelector by remember { mutableStateOf(false) }
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
        val deleteDay = confirmation?.startsWith("delete-day:") == true
        val deleteExercise = confirmation?.startsWith("delete-exercise:") == true
        val selectedDay = confirmation?.substringAfter("delete-day:")?.toIntOrNull()
        val selectedExercise = confirmation?.substringAfter("delete-exercise:")?.toLongOrNull()
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = {
                Text(
                    when {
                        complete -> "Completar plan"
                        deleteDay -> "Eliminar día"
                        deleteExercise -> "Eliminar ejercicio"
                        else -> "Desactivar plan"
                    },
                )
            },
            text = {
                Text(
                    when {
                        complete -> "¿Quieres marcar este plan como completado?"
                        deleteDay -> "¿Quieres eliminar el día? Se eliminarán también los ejercicios asociados."
                        deleteExercise -> "¿Quieres eliminar este ejercicio?"
                        else -> "¿Quieres desactivar este plan?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        when {
                            complete -> viewModel.complete(planId)
                            deleteDay && selectedDay != null -> structureViewModel.deleteDay(planId, selectedDay)
                            deleteExercise && selectedExercise != null ->
                                structureViewModel.deleteExercise(planId, selectedExercise)
                            else -> viewModel.deactivate(planId)
                        }
                    },
                ) {
                    Text(
                        when {
                            complete -> "Completar"
                            deleteDay || deleteExercise -> "Eliminar"
                            else -> "Desactivar"
                        },
                    )
                }
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
                        mutation is WorkoutPlanMutationState.Deactivating ||
                        structureMutation is WorkoutPlanStructureMutationState.AddingDay ||
                        structureMutation is WorkoutPlanStructureMutationState.DeletingDay ||
                        structureMutation is WorkoutPlanStructureMutationState.AddingExercise ||
                        structureMutation is WorkoutPlanStructureMutationState.UpdatingExercise ||
                        structureMutation is WorkoutPlanStructureMutationState.DeletingExercise
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
                        OutlinedButton(enabled = !mutating, onClick = { daySelector = true }) {
                            Text("Añadir día")
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
                    when (val operation = structureMutation) {
                        is WorkoutPlanStructureMutationState.Success -> Text(operation.message)
                        is WorkoutPlanStructureMutationState.Error -> {
                            Text(
                                operation.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                            if (operation.message == "El plan ha cambiado. Recarga el plan antes de continuar.") {
                                TextButton(onClick = structureViewModel::retry) { Text("Recargar") }
                            }
                        }
                        WorkoutPlanStructureMutationState.AddingDay,
                        WorkoutPlanStructureMutationState.DeletingDay,
                        WorkoutPlanStructureMutationState.AddingExercise,
                        WorkoutPlanStructureMutationState.UpdatingExercise,
                        WorkoutPlanStructureMutationState.DeletingExercise -> CircularProgressIndicator()
                        WorkoutPlanStructureMutationState.Idle -> Unit
                    }
                }
                items(
                    plan.days.sortedBy { it.dayOfWeek },
                    key = { it.id },
                ) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(dayName(day.dayOfWeek), style = MaterialTheme.typography.titleLarge)
                        if (canManage && plan.status == "ACTIVE") {
                            OutlinedButton(
                                enabled = structureMutation !is WorkoutPlanStructureMutationState.AddingDay &&
                                    structureMutation !is WorkoutPlanStructureMutationState.DeletingDay,
                                onClick = { confirmation = "delete-day:${day.dayOfWeek}" },
                            ) {
                                Text("Eliminar día")
                            }
                        }
                        if (day.exercises.isEmpty()) {
                            Text("Sin ejercicios")
                            if (canManage && plan.status == "ACTIVE") {
                                Text("Este día no tiene ejercicios.")
                                OutlinedButton(onClick = {
                                    editor = ExerciseEditorTarget.Add(day.dayOfWeek)
                                }) { Text("Añadir ejercicio") }
                            }
                        } else {
                            day.exercises
                                .sortedBy { it.orderIndex }
                                .forEach { exercise ->
                                    WorkoutPlanExerciseCard(
                                        exercise = exercise,
                                        canManage = canManage && plan.status == "ACTIVE",
                                        onEdit = { editor = ExerciseEditorTarget.Edit(day.dayOfWeek, exercise) },
                                        onDelete = { confirmation = "delete-exercise:${exercise.id}" },
                                    )
                                }
                            if (canManage && plan.status == "ACTIVE") {
                                OutlinedButton(onClick = {
                                    editor = ExerciseEditorTarget.Add(day.dayOfWeek)
                                }) { Text("Añadir ejercicio") }
                            }
                        }
                    }
                }
                if (plan.days.isEmpty()) {
                    item {
                        Text(
                            if (canManage && plan.status == "ACTIVE") {
                                "Este plan no tiene días."
                            } else {
                                "Este plan todavía no tiene días."
                            },
                        )
                        if (canManage && plan.status == "ACTIVE") {
                            Button(onClick = { daySelector = true }) { Text("Añadir día") }
                        }
                    }
                }
            }
        }
    }
    if (daySelector) {
        AlertDialog(
            onDismissRequest = { daySelector = false },
            title = { Text("Seleccionar día") },
            text = {
                Column {
                    (1..7).forEach { day ->
                        TextButton(onClick = {
                            daySelector = false
                            editor = ExerciseEditorTarget.AddDay(day)
                        }) { Text(dayName(day)) }
                    }
                }
            },
            confirmButton = {},
        )
    }
    editor?.let { target ->
        WorkoutPlanExerciseEditorDialog(
            target = target,
            catalog = structureViewModel.catalog.collectAsStateWithLifecycle().value,
            mutation = structureMutation,
            onLoadCatalog = structureViewModel::loadExerciseCatalog,
            onLoadCategory = structureViewModel::loadExercises,
            onDismiss = { editor = null },
            onSubmit = { request ->
                when (target) {
                    is ExerciseEditorTarget.Add ->
                        structureViewModel.addExercise(planId, target.dayOfWeek, request)
                    is ExerciseEditorTarget.Edit ->
                        structureViewModel.updateExercise(planId, target.exercise.id, request)
                    is ExerciseEditorTarget.AddDay ->
                        structureViewModel.addDay(planId, WorkoutPlanDayRequest(target.dayOfWeek, listOf(request)))
                }
                editor = null
            },
        )
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
private fun WorkoutPlanExerciseCard(
    exercise: WorkoutPlanExercise,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(exercise.exercise?.name ?: "Ejercicio no disponible")
            exercise.exercise?.categoryName
                ?.takeIf { it.isNotBlank() }
                ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("${exercise.sets} series × ${exercise.repetitions} repeticiones")
            Text("Descanso: ${exercise.restSeconds} s")
            Text("Orden: ${exercise.orderIndex}")
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Editar") }
                    OutlinedButton(onClick = onDelete) { Text("Eliminar") }
                }
            }
        }
    }
}

private sealed interface ExerciseEditorTarget {
    val dayOfWeek: Int
    data class Add(override val dayOfWeek: Int) : ExerciseEditorTarget
    data class AddDay(override val dayOfWeek: Int) : ExerciseEditorTarget
    data class Edit(override val dayOfWeek: Int, val exercise: WorkoutPlanExercise) : ExerciseEditorTarget
}

@Composable
private fun WorkoutPlanExerciseEditorDialog(
    target: ExerciseEditorTarget,
    catalog: WorkoutPlanExerciseCatalogState,
    mutation: WorkoutPlanStructureMutationState,
    onLoadCatalog: () -> Unit,
    onLoadCategory: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (WorkoutPlanExerciseRequest) -> Unit,
) {
    var selectedExerciseId by remember(target) {
        mutableStateOf((target as? ExerciseEditorTarget.Edit)?.exercise?.exercise?.id)
    }
    var sets by remember(target) { mutableStateOf((target as? ExerciseEditorTarget.Edit)?.exercise?.sets?.toString() ?: "") }
    var repetitions by remember(target) { mutableStateOf((target as? ExerciseEditorTarget.Edit)?.exercise?.repetitions?.toString() ?: "") }
    var rest by remember(target) { mutableStateOf((target as? ExerciseEditorTarget.Edit)?.exercise?.restSeconds?.toString() ?: "") }
    var order by remember(target) { mutableStateOf((target as? ExerciseEditorTarget.Edit)?.exercise?.orderIndex?.toString() ?: "") }
    var error by remember(target) { mutableStateOf<String?>(null) }
    LaunchedEffect(target) { onLoadCatalog() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (target is ExerciseEditorTarget.Edit) "Editar ejercicio" else "Añadir ejercicio") },
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
                                TextButton(onClick = {
                                    onLoadCategory(category.id)
                                }) { Text(category.name) }
                            }
                        }
                        is WorkoutPlanExerciseCatalogState.Success -> {
                            Text("Ejercicio")
                            catalog.exercises.forEach { exercise ->
                                TextButton(onClick = { selectedExerciseId = exercise.id }) {
                                    Text(if (selectedExerciseId == exercise.id) "${exercise.name} (seleccionado)" else exercise.name)
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(sets, { sets = it }, label = { Text("Series") })
                    OutlinedTextField(repetitions, { repetitions = it }, label = { Text("Repeticiones") })
                    OutlinedTextField(rest, { rest = it }, label = { Text("Descanso") })
                    OutlinedTextField(order, { order = it }, label = { Text("Orden") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = mutation !is WorkoutPlanStructureMutationState.AddingExercise &&
                    mutation !is WorkoutPlanStructureMutationState.UpdatingExercise,
                onClick = {
                    val request = WorkoutPlanExerciseRequest(
                        exerciseId = selectedExerciseId,
                        sourceTemplateExerciseId = (target as? ExerciseEditorTarget.Edit)?.exercise?.sourceTemplateExerciseId,
                        orderIndex = order.toIntOrNull() ?: 0,
                        sets = sets.toIntOrNull() ?: 0,
                        repetitions = repetitions.toIntOrNull() ?: 0,
                        restSeconds = rest.toIntOrNull() ?: -1,
                    )
                    val hasExerciseReference = selectedExerciseId != null ||
                        request.sourceTemplateExerciseId != null
                    if (!hasExerciseReference || request.orderIndex < 1 || request.sets < 1 ||
                        request.repetitions < 1 || request.restSeconds < 0
                    ) {
                        error = "Los datos del ejercicio no son válidos."
                    } else {
                        onSubmit(request)
                    }
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
