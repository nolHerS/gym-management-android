package com.imanol.gymmanagement.feature.workoutexecution.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSetPosition
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionState
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionStatus
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutRestTimerStatus

@Composable
fun WorkoutTodayScreen(
    viewModel: WorkoutTodayViewModel,
    onUnauthorized: () -> Unit,
    onStart: (WorkoutExecutionSnapshot) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadToday()
    }
    LaunchedEffect(state) {
        if (state is WorkoutTodayUiState.Unauthorized) onUnauthorized()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Entrenamiento de hoy", style = MaterialTheme.typography.headlineSmall)
        when (val currentState = state) {
            WorkoutTodayUiState.Loading -> GymLoading()
            WorkoutTodayUiState.Empty -> Text("No hay entrenamiento programado para hoy.")
            is WorkoutTodayUiState.Error -> {
                GymErrorMessage(message = currentState.message)
                GymButton(text = "Reintentar", onClick = viewModel::retry)
            }
            WorkoutTodayUiState.Unauthorized -> Unit
            is WorkoutTodayUiState.Ambiguous -> Text(
                "Hay varios entrenamientos válidos para hoy. Debe resolverse la planificación antes de comenzar.",
                color = MaterialTheme.colorScheme.error,
            )
            is WorkoutTodayUiState.Success -> {
                Text(currentState.date)
                Text(dayName(currentState.dayOfWeek), style = MaterialTheme.typography.titleMedium)
                val day = currentState.plan.days.single { it.dayOfWeek == currentState.dayOfWeek }
                Text("${day.exercises.size} ejercicios")
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(day.exercises.sortedBy { it.orderIndex }, key = { it.id }) { exercise ->
                        GymCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    exercise.exercise?.name ?: "Ejercicio",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text("${exercise.sets} series × ${exercise.repetitions} repeticiones")
                                Text("Descanso: ${exercise.restSeconds} s")
                            }
                        }
                    }
                }
                GymButton(
                    text = "COMENZAR",
                    onClick = { viewModel.createSnapshot(onStart) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun WorkoutExecutionScreen(
    viewModel: WorkoutExecutionViewModel?,
    onFinished: (WorkoutExecutionState) -> Unit = {},
    onCancelled: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    if (viewModel == null) {
        WorkoutExecutionInvalidScreen(onBack)
        return
    }
    val executionState by viewModel.uiState.collectAsStateWithLifecycle()
    val timerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    val canLeaveWithConfirmation = executionState.status == WorkoutExecutionStatus.Running ||
        executionState.status == WorkoutExecutionStatus.Resting

    LaunchedEffect(executionState.status) {
        if (executionState.status == WorkoutExecutionStatus.Finished) {
            onFinished(executionState)
        }
    }
    BackHandler(enabled = canLeaveWithConfirmation) {
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("¿Abandonar entrenamiento?") },
            text = { Text("El progreso local se perderá.") },
            confirmButton = {
                GymButton(
                    text = "ABANDONAR",
                    onClick = {
                        showExitConfirmation = false
                        viewModel.cancel()
                    },
                )
            },
            dismissButton = {
                GymButton(
                    text = "SEGUIR ENTRENANDO",
                    onClick = { showExitConfirmation = false },
                )
            },
        )
    }

    val currentExercise = viewModel.snapshot.exercises.getOrNull(executionState.currentExerciseIndex)
    if (currentExercise == null) {
        WorkoutExecutionInvalidScreen(onBack)
        return
    }
    val isResting = executionState.status == WorkoutExecutionStatus.Resting
    val timerFinished = timerState.status == WorkoutRestTimerStatus.Finished
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Entrenamiento", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Ejercicio ${executionState.currentExerciseIndex + 1} de ${executionState.totalExercises}",
            style = MaterialTheme.typography.titleMedium,
        )
        GymCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(currentExercise.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${currentExercise.sets.size} series × " +
                        "${currentExercise.sets.firstOrNull()?.plannedRepetitions ?: 0} repeticiones",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Descanso: ${currentExercise.restSeconds} s")
            }
        }
        HorizontalDivider()
        Text(
            "Serie ${executionState.currentSetNumber} de ${currentExercise.sets.size}",
            style = MaterialTheme.typography.titleMedium,
        )
        SeriesProgress(
            exerciseIndex = executionState.currentExerciseIndex,
            sets = currentExercise.sets.size,
            state = executionState,
        )
        Text(
            "${executionState.completedSets} / ${executionState.totalSets} series · " +
                "${(executionState.progress * 100).toInt()}%",
        )
        if (isResting) {
            Text("DESCANSO", style = MaterialTheme.typography.headlineMedium)
            Text(
                formatRemainingMillis(timerState.remainingMillis),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                if (timerFinished) "Descanso terminado" else "Siguiente serie",
                style = MaterialTheme.typography.titleMedium,
            )
            GymButton(
                text = if (timerFinished) "CONTINUAR" else "SALTAR DESCANSO",
                onClick = viewModel::finishRest,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            when (executionState.status) {
                WorkoutExecutionStatus.Ready -> GymButton(
                    text = "INICIAR",
                    onClick = viewModel::start,
                    modifier = Modifier.fillMaxWidth(),
                )
                WorkoutExecutionStatus.Running -> GymButton(
                    text = "COMPLETAR SERIE",
                    onClick = viewModel::completeCurrentSet,
                    modifier = Modifier.fillMaxWidth(),
                )
                WorkoutExecutionStatus.Finished,
                WorkoutExecutionStatus.Cancelled -> Unit
                WorkoutExecutionStatus.Resting -> Unit
            }
        }
        if (executionState.status == WorkoutExecutionStatus.Running) {
            GymButton(
                text = "FINALIZAR",
                onClick = { viewModel.finish() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (executionState.status == WorkoutExecutionStatus.Running ||
            executionState.status == WorkoutExecutionStatus.Resting
        ) {
            GymButton(
                text = "ABANDONAR",
                onClick = { showExitConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (executionState.status == WorkoutExecutionStatus.Cancelled) {
            Text("Entrenamiento abandonado.", color = MaterialTheme.colorScheme.error)
            GymButton(text = "VOLVER", onClick = onCancelled, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SeriesProgress(
    exerciseIndex: Int,
    sets: Int,
    state: WorkoutExecutionState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(sets) { index ->
            val setNumber = index + 1
            val position = WorkoutExecutionSetPosition(exerciseIndex, setNumber)
            val (symbol, label) = when {
                position in state.completedSetPositions -> "✓" to "completada"
                setNumber == state.currentSetNumber -> "●" to "actual"
                else -> "○" to "pendiente"
            }
            Text(
                text = "$symbol $label",
                modifier = Modifier.padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun WorkoutExecutionInvalidScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("No se puede iniciar el entrenamiento", style = MaterialTheme.typography.headlineSmall)
        Text("El entrenamiento no contiene ejercicios válidos.")
        GymButton(text = "VOLVER", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun WorkoutFinishedScreen(
    state: WorkoutExecutionState,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Entrenamiento completado", style = MaterialTheme.typography.headlineSmall)
        Text("${state.totalExercises} ejercicios completados")
        Text("${state.completedSets} / ${state.totalSets} series")
        Text("Progreso: ${(state.progress * 100).toInt()}%")
        Text(
            "Este entrenamiento se ha completado localmente. Todavía no existe " +
                "sincronización con el servidor ni historial.",
        )
        GymButton(text = "VOLVER", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

private fun formatRemainingMillis(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> "Día"
}
