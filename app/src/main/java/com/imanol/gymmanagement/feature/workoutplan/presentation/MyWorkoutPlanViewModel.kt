package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface MyWorkoutPlanUiState {
    val weekStart: LocalDate

    data class Loading(override val weekStart: LocalDate) : MyWorkoutPlanUiState
    data class Success(
        override val weekStart: LocalDate,
        val plans: List<WorkoutPlan>,
    ) : MyWorkoutPlanUiState
    data class Empty(override val weekStart: LocalDate) : MyWorkoutPlanUiState
    data class Error(override val weekStart: LocalDate, val message: String) : MyWorkoutPlanUiState
    data class Unauthorized(override val weekStart: LocalDate) : MyWorkoutPlanUiState
}

fun LocalDate.mondayOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@HiltViewModel
class MyWorkoutPlanViewModel @Inject constructor(
    private val getMyWorkoutWeek: GetMyWorkoutWeekUseCase,
) : ViewModel() {
    private val initialWeek = LocalDate.now().mondayOfWeek()
    private val _uiState = MutableStateFlow<MyWorkoutPlanUiState>(
        MyWorkoutPlanUiState.Loading(initialWeek),
    )
    val uiState: StateFlow<MyWorkoutPlanUiState> = _uiState.asStateFlow()

    private var providedScope: CoroutineScope? = null

    internal constructor(
        getMyWorkoutWeek: GetMyWorkoutWeekUseCase,
        scope: CoroutineScope,
    ) : this(getMyWorkoutWeek) {
        providedScope = scope
    }

    fun loadCurrentWeek() {
        loadWeek(initialWeek)
    }

    fun loadWeek(weekStart: LocalDate) {
        val monday = weekStart.mondayOfWeek()
        _uiState.value = MyWorkoutPlanUiState.Loading(monday)
        (providedScope ?: viewModelScope).launch {
            try {
                val plans = getMyWorkoutWeek(monday.toString())
                _uiState.value = if (plans.isEmpty()) {
                    MyWorkoutPlanUiState.Empty(monday)
                } else {
                    MyWorkoutPlanUiState.Success(monday, plans)
                }
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401 || exception.code() == 403) {
                    MyWorkoutPlanUiState.Unauthorized(monday)
                } else {
                    MyWorkoutPlanUiState.Error(
                        monday,
                        "No se pudo cargar tu planificación. Inténtalo de nuevo.",
                    )
                }
            } catch (_: IOException) {
                _uiState.value = MyWorkoutPlanUiState.Error(
                    monday,
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }

    fun retry() {
        loadWeek(_uiState.value.weekStart)
    }

    fun previousWeek() {
        loadWeek(_uiState.value.weekStart.minusWeeks(1))
    }

    fun nextWeek() {
        loadWeek(_uiState.value.weekStart.plusWeeks(1))
    }

    fun thisWeek() {
        loadCurrentWeek()
    }
}
