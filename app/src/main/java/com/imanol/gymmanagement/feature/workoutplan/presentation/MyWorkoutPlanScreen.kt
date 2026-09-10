package com.imanol.gymmanagement.feature.workoutplan.presentation

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
private val dayNames = mapOf(
    1 to "LUNES",
    2 to "MARTES",
    3 to "MIÉRCOLES",
    4 to "JUEVES",
    5 to "VIERNES",
    6 to "SÁBADO",
    7 to "DOMINGO",
)

@Composable
fun MyWorkoutPlanScreen(
    viewModel: MyWorkoutPlanViewModel,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentWeek()
    }
    LaunchedEffect(uiState) {
        if (uiState is MyWorkoutPlanUiState.Unauthorized) onUnauthorized()
    }

    val weekStart = uiState.weekStart
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mi entrenamiento", style = MaterialTheme.typography.headlineSmall)
        WeekNavigation(
            weekStart = weekStart,
            onPrevious = viewModel::previousWeek,
            onNext = viewModel::nextWeek,
            onCurrent = viewModel::thisWeek,
        )
        when (val state = uiState) {
            is MyWorkoutPlanUiState.Loading -> GymLoading()
            is MyWorkoutPlanUiState.Empty -> Text("No hay entrenamiento esta semana.")
            is MyWorkoutPlanUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(text = "Reintentar", onClick = viewModel::retry)
            }
            is MyWorkoutPlanUiState.Unauthorized -> Unit
            is MyWorkoutPlanUiState.Success -> {
                val days = state.plans.contentByDay()
                if (days.isEmpty()) {
                    Text("No hay entrenamiento esta semana.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        days.forEach { (day, exercises) ->
                            item(key = "day-$day") {
                                Text(dayNames.getValue(day), style = MaterialTheme.typography.titleMedium)
                            }
                            items(exercises, key = { it.id }) { exercise ->
                                ExercisePlanCard(exercise)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekNavigation(
    weekStart: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Semana del ${formatWeekStart(weekStart)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GymButton(text = "Anterior", onClick = onPrevious, modifier = Modifier.weight(1f))
            GymButton(text = "Esta semana", onClick = onCurrent, modifier = Modifier.weight(1f))
            GymButton(text = "Siguiente", onClick = onNext, modifier = Modifier.weight(1f))
        }
    }
}

private fun formatWeekStart(value: String): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).let { parser ->
        val position = ParsePosition(0)
        val date = requireNotNull(parser.parse(value, position)) { "Invalid ISO date: $value" }
        require(position.index == value.length) { "Invalid ISO date: $value" }
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

@Composable
private fun ExercisePlanCard(exercise: WorkoutPlanExercise) {
    GymCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = exercise.exercise?.name ?: "Ejercicio",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = "${exercise.sets} series × ${exercise.repetitions} repeticiones")
            Text(text = "Descanso: ${exercise.restSeconds} s")
            Text(text = "Orden: ${exercise.orderIndex}")
        }
    }
}

private fun List<WorkoutPlan>.contentByDay(): Map<Int, List<WorkoutPlanExercise>> =
    flatMap { it.days }
        .flatMap { day -> day.exercises.map { exercise -> day.dayOfWeek to exercise } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, exercises) -> exercises.sortedBy { it.orderIndex } }
        .toSortedMap()
