package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
import com.imanol.gymmanagement.feature.workoutplan.domain.CreationMode
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDraftDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDraftExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest

@Composable
fun WorkoutPlansScreen(
    clientId: Long,
    viewModel: WorkoutPlanViewModel,
    onPlanSelected: (Long) -> Unit,
    onUnauthorized: () -> Unit,
    onCreate: () -> Unit = {},
) {
    val state by viewModel.plans.collectAsStateWithLifecycle()
    LaunchedEffect(clientId) { viewModel.load(clientId) }
    if (state is WorkoutPlansState.Unauthorized) LaunchedEffect(Unit) { onUnauthorized() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Planes de entrenamiento", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onCreate) { Text("Crear plan") }
        when (val plansState = state) {
            WorkoutPlansState.Loading -> CircularProgressIndicator()
            WorkoutPlansState.Empty -> Text("No hay planes para este cliente.")
            is WorkoutPlansState.Success -> LazyColumn {
                items(plansState.plans, key = { it.id }) { plan ->
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
                Text(plansState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.load(clientId) }) { Text("Reintentar") }
            }
            WorkoutPlansState.Unauthorized -> Unit
        }
    }
}

@Composable
fun WorkoutPlanFormScreen(
    clientId: Long,
    planId: Long?,
    viewModel: WorkoutPlanViewModel,
    onSaved: (Long) -> Unit,
) {
    val state by viewModel.createState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()

    var draftDaySelectorVisible by remember { mutableStateOf(false) }
    var scratchEditor by remember { mutableStateOf<ScratchExerciseEditorTarget?>(null) }
    var draftDeleteConfirmation by remember { mutableStateOf<DraftDeleteTarget?>(null) }

    LaunchedEffect(planId) {
        if (planId == null) viewModel.prepareCreate(clientId) else viewModel.prepareEdit(planId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                if (planId == null) "Crear plan" else "Editar plan",
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = state.startDate,
                onValueChange = viewModel::setStartDate,
                label = { Text("Inicio (AAAA-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.endDate,
                onValueChange = viewModel::setEndDate,
                label = { Text("Fin (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (planId == null) {
                Text("Modo de creación", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.creationMode == CreationMode.TEMPLATE,
                        onClick = { viewModel.setCreationMode(CreationMode.TEMPLATE) },
                        label = { Text("Plantilla") },
                    )
                    FilterChip(
                        selected = state.creationMode == CreationMode.FROM_SCRATCH,
                        onClick = viewModel::selectFromScratch,
                        label = { Text("Desde cero") },
                    )
                }
            }
        }

        if (planId == null && state.creationMode == CreationMode.TEMPLATE) {
            if (state.templates.isEmpty()) {
                item { Text("No hay plantillas activas disponibles.") }
            } else {
                items(state.templates, key = { "template-${it.id}" }) { template ->
                    TemplateOption(
                        template = template,
                        selected = state.template?.id == template.id,
                        onClick = { viewModel.selectTemplate(template) },
                    )
                }
            }
            state.templateDetail?.exercises?.forEach { templateExercise ->
                state.drafts[templateExercise.id]?.let { request ->
                    item(key = "exercise-${templateExercise.id}") {
                        Text(templateExercise.exercise.name, style = MaterialTheme.typography.titleSmall)
                        WorkoutPlanDayChips(
                            selectedDay = state.assignments[templateExercise.id],
                            onSelectDay = { day -> viewModel.assignExercise(templateExercise.id, day) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = request.sets.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { parsed ->
                                        viewModel.updateExercise(
                                            templateExercise.id,
                                            request.copy(sets = parsed),
                                        )
                                    }
                                },
                                label = { Text("Series") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = request.repetitions.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { parsed ->
                                        viewModel.updateExercise(
                                            templateExercise.id,
                                            request.copy(repetitions = parsed),
                                        )
                                    }
                                },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = request.restSeconds.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { parsed ->
                                        viewModel.updateExercise(
                                            templateExercise.id,
                                            request.copy(restSeconds = parsed),
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
        }

        if (planId == null && state.creationMode == CreationMode.FROM_SCRATCH) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { draftDaySelectorVisible = true }) { Text("Añadir día") }
                }
                if (state.scratchDraft.days.isEmpty()) {
                    Text("Añade días y ejercicios para crear el plan desde cero.")
                }
            }
            items(state.scratchDraft.days.sortedBy { it.dayOfWeek }, key = { it.localId }) { day ->
                ScratchDraftDayCard(
                    day = day,
                    onAddExercise = {
                        scratchEditor = ScratchExerciseEditorTarget(
                            dayOfWeek = day.dayOfWeek,
                            dayLocalId = day.localId,
                            localId = null,
                            initialState = WorkoutPlanExerciseEditorInitialState(),
                        )
                    },
                    onEditExercise = { draftExercise ->
                        scratchEditor = ScratchExerciseEditorTarget(
                            dayOfWeek = day.dayOfWeek,
                            dayLocalId = day.localId,
                            localId = draftExercise.localId,
                            initialState = WorkoutPlanExerciseEditorInitialState(
                                exerciseId = draftExercise.request.exerciseId,
                                exerciseName = draftExercise.exerciseName,
                                sourceTemplateExerciseId = draftExercise.request.sourceTemplateExerciseId,
                                sets = draftExercise.request.sets.toString(),
                                repetitions = draftExercise.request.repetitions.toString(),
                                restSeconds = draftExercise.request.restSeconds.toString(),
                                orderIndex = draftExercise.request.orderIndex.toString(),
                            ),
                        )
                    },
                    onDeleteExercise = { draftDeleteConfirmation = DraftDeleteTarget.Exercise(it) },
                    onDeleteDay = { draftDeleteConfirmation = DraftDeleteTarget.Day(day.localId) },
                )
            }
        }

        item {
            if (state.loading) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (planId == null && state.error != null && !state.saving) {
                TextButton(onClick = { viewModel.retryCreate(clientId, onSaved) }) {
                    Text("Reintentar creación")
                }
            }
            Button(
                enabled = !state.loading && !state.saving && !state.templateDetailLoading,
                onClick = {
                    if (planId == null) viewModel.create(clientId, onSaved)
                    else viewModel.update(onSaved)
                },
            ) { Text("Guardar") }
        }
    }

    draftDeleteConfirmation?.let { target ->
        AlertDialog(
            onDismissRequest = { draftDeleteConfirmation = null },
            title = { Text(if (target is DraftDeleteTarget.Day) "Eliminar día" else "Eliminar ejercicio") },
            text = {
                Text(
                    if (target is DraftDeleteTarget.Day) {
                        "Se eliminarán también los ejercicios del borrador."
                    } else {
                        "¿Quieres eliminar este ejercicio del borrador?"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (target) {
                        is DraftDeleteTarget.Day -> viewModel.deleteDraftDay(target.localId)
                        is DraftDeleteTarget.Exercise -> viewModel.deleteDraftExercise(target.localId)
                    }
                    draftDeleteConfirmation = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { draftDeleteConfirmation = null }) { Text("Cancelar") }
            },
        )
    }

    if (draftDaySelectorVisible) {
        val usedDays = state.scratchDraft.days.map { it.dayOfWeek }
        val availableDays = (1..7).filterNot { it in usedDays }
        WorkoutPlanDaySelectorDialog(
            title = "Seleccionar día",
            availableDays = availableDays,
            dayLabel = ::dayName,
            onDismiss = { draftDaySelectorVisible = false },
            onDaySelected = { day ->
                draftDaySelectorVisible = false
                scratchEditor = ScratchExerciseEditorTarget(
                    dayOfWeek = day,
                    dayLocalId = null,
                    localId = null,
                    initialState = WorkoutPlanExerciseEditorInitialState(),
                )
            },
        )
    }

    scratchEditor?.let { target ->
        WorkoutPlanExerciseEditorDialog(
            title = if (target.localId == null) "Añadir ejercicio" else "Editar ejercicio",
            catalog = catalog,
            isSubmitting = state.saving,
            initialState = target.initialState,
            onLoadCatalog = viewModel::loadExerciseCatalog,
            onLoadCategory = viewModel::loadExercises,
            onDismiss = { scratchEditor = null },
            onSubmit = { request, selectedName ->
                val updated = viewModel.upsertDraftExercise(
                    dayOfWeek = target.dayOfWeek,
                    dayLocalId = target.dayLocalId,
                    request = request,
                    exerciseName = selectedName ?: target.initialState.exerciseName,
                    localId = target.localId,
                )
                if (updated) scratchEditor = null
            },
        )
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
    val structureCatalog by structureViewModel.catalog.collectAsStateWithLifecycle()
    val mutation by viewModel.mutation.collectAsStateWithLifecycle()

    var confirmation by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<DetailExerciseEditorTarget?>(null) }
    var daySelector by remember { mutableStateOf(false) }

    LaunchedEffect(planId, state) {
        val seedPlan = (state as? WorkoutPlanDetailState.Success)?.plan?.takeIf { it.id == planId }
        structureViewModel.loadIfNeeded(planId, seedPlan)
    }
    LaunchedEffect(structureState) {
        if (structureState is WorkoutPlanStructureState.Success) {
            viewModel.setDetailPlan((structureState as WorkoutPlanStructureState.Success).plan)
        }
    }
    LaunchedEffect(state) {
        if (state is WorkoutPlanDetailState.Unauthorized) onUnauthorized()
    }

    val currentPlan = (structureState as? WorkoutPlanStructureState.Success)
        ?.plan
        ?: (state as? WorkoutPlanDetailState.Success)
            ?.plan
            ?.takeIf { it.id == planId }

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
                            deleteExercise && selectedExercise != null -> structureViewModel.deleteExercise(planId, selectedExercise)
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
                val plan = currentPlan ?: structure.plan
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
                            Text(operation.message, color = MaterialTheme.colorScheme.error)
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
                items(plan.days.sortedBy { it.dayOfWeek }, key = { it.id }) { day ->
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
                                OutlinedButton(onClick = {
                                    editor = DetailExerciseEditorTarget.Add(day.dayOfWeek)
                                }) { Text("Añadir ejercicio") }
                            }
                        } else {
                            day.exercises
                                .sortedBy { it.orderIndex }
                                .forEach { exercise ->
                                    WorkoutPlanExerciseCard(
                                        exercise = exercise,
                                        canManage = canManage && plan.status == "ACTIVE",
                                        onEdit = { editor = DetailExerciseEditorTarget.Edit(day.dayOfWeek, exercise) },
                                        onDelete = { confirmation = "delete-exercise:${exercise.id}" },
                                    )
                                }
                            if (canManage && plan.status == "ACTIVE") {
                                OutlinedButton(onClick = {
                                    editor = DetailExerciseEditorTarget.Add(day.dayOfWeek)
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

    if (daySelector && currentPlan != null) {
        val availableDays = (1..7).filterNot { day -> currentPlan.days.any { it.dayOfWeek == day } }
        WorkoutPlanDaySelectorDialog(
            title = "Seleccionar día",
            availableDays = availableDays,
            dayLabel = ::dayName,
            onDismiss = { daySelector = false },
            onDaySelected = { day ->
                daySelector = false
                editor = DetailExerciseEditorTarget.AddDay(day)
            },
        )
    }

    editor?.let { target ->
        WorkoutPlanExerciseEditorDialog(
            title = if (target is DetailExerciseEditorTarget.Edit) "Editar ejercicio" else "Añadir ejercicio",
            catalog = structureCatalog,
            isSubmitting = structureMutation is WorkoutPlanStructureMutationState.AddingExercise ||
                structureMutation is WorkoutPlanStructureMutationState.UpdatingExercise,
            initialState = target.initialState(),
            onLoadCatalog = structureViewModel::loadExerciseCatalog,
            onLoadCategory = structureViewModel::loadExercises,
            onDismiss = { editor = null },
            onSubmit = { request, _ ->
                when (target) {
                    is DetailExerciseEditorTarget.Add ->
                        structureViewModel.addExercise(planId, target.dayOfWeek, request)
                    is DetailExerciseEditorTarget.Edit ->
                        structureViewModel.updateExercise(planId, target.exercise.id, request)
                    is DetailExerciseEditorTarget.AddDay ->
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
private fun TemplateOption(
    template: WorkoutTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(if (selected) "${template.name} (seleccionada)" else template.name)
    }
}

@Composable
private fun ScratchDraftDayCard(
    day: WorkoutPlanDraftDay,
    onAddExercise: () -> Unit,
    onEditExercise: (WorkoutPlanDraftExercise) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onDeleteDay: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dayName(day.dayOfWeek), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onDeleteDay) { Text("Eliminar día") }
            }
            if (day.exercises.isEmpty()) {
                Text("Sin ejercicios")
            } else {
                day.exercises.sortedBy { it.request.orderIndex }.forEach { exercise ->
                    Card {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                exercise.exerciseName
                                    ?: exercise.request.exerciseId?.let { "Ejercicio #$it" }
                                    ?: "Ejercicio sin referencia",
                            )
                            Text("${exercise.request.sets} series × ${exercise.request.repetitions} repeticiones")
                            Text("Descanso: ${exercise.request.restSeconds} s")
                            Text("Orden: ${exercise.request.orderIndex}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onEditExercise(exercise) }) { Text("Editar") }
                                OutlinedButton(onClick = { onDeleteExercise(exercise.localId) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = onAddExercise) { Text("Añadir ejercicio") }
        }
    }
}

@Composable
private fun WorkoutPlanExerciseCard(
    exercise: WorkoutPlanExercise,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
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

private sealed interface DetailExerciseEditorTarget {
    val dayOfWeek: Int

    data class Add(override val dayOfWeek: Int) : DetailExerciseEditorTarget
    data class AddDay(override val dayOfWeek: Int) : DetailExerciseEditorTarget
    data class Edit(
        override val dayOfWeek: Int,
        val exercise: WorkoutPlanExercise,
    ) : DetailExerciseEditorTarget
}

private fun DetailExerciseEditorTarget.initialState(): WorkoutPlanExerciseEditorInitialState = when (this) {
    is DetailExerciseEditorTarget.Add,
    is DetailExerciseEditorTarget.AddDay -> WorkoutPlanExerciseEditorInitialState()
    is DetailExerciseEditorTarget.Edit -> WorkoutPlanExerciseEditorInitialState(
        exerciseId = exercise.exercise?.id,
        exerciseName = exercise.exercise?.name,
        sourceTemplateExerciseId = exercise.sourceTemplateExerciseId,
        sets = exercise.sets.toString(),
        repetitions = exercise.repetitions.toString(),
        restSeconds = exercise.restSeconds.toString(),
        orderIndex = exercise.orderIndex.toString(),
    )
}

private data class ScratchExerciseEditorTarget(
    val dayOfWeek: Int,
    val dayLocalId: String?,
    val localId: Long?,
    val initialState: WorkoutPlanExerciseEditorInitialState,
)

private sealed interface DraftDeleteTarget {
    data class Day(val localId: String) : DraftDeleteTarget
    data class Exercise(val localId: Long) : DraftDeleteTarget
}
