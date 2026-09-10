package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workoutplan.domain.GetMyWorkoutWeekUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface MyWorkoutPlanUiState {
    val weekStart: String

    data class Loading(override val weekStart: String) : MyWorkoutPlanUiState
    data class Success(
        override val weekStart: String,
        val plans: List<WorkoutPlan>,
    ) : MyWorkoutPlanUiState
    data class Empty(override val weekStart: String) : MyWorkoutPlanUiState
    data class Error(override val weekStart: String, val message: String) : MyWorkoutPlanUiState
    data class Unauthorized(override val weekStart: String) : MyWorkoutPlanUiState
}

fun String.mondayOfWeek(): String {
    val calendar = parseIsoDate(this)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    return formatIsoDate(calendar)
}

@HiltViewModel
class MyWorkoutPlanViewModel @Inject constructor(
    private val getMyWorkoutWeek: GetMyWorkoutWeekUseCase,
) : ViewModel() {
    private val initialWeek = formatIsoDate(Calendar.getInstance()).mondayOfWeek()
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

    fun loadWeek(weekStart: String) {
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
        loadWeek(shiftWeek(_uiState.value.weekStart, -1))
    }

    fun nextWeek() {
        loadWeek(shiftWeek(_uiState.value.weekStart, 1))
    }

    fun thisWeek() {
        loadCurrentWeek()
    }
}

private fun shiftWeek(value: String, weeks: Int): String =
    parseIsoDate(value).apply { add(Calendar.WEEK_OF_YEAR, weeks) }.let(::formatIsoDate).mondayOfWeek()

private fun parseIsoDate(value: String): Calendar =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.let { format ->
        ParsePosition(0).let { position ->
            requireNotNull(format.parse(value, position)) { "Invalid ISO date: $value" }
            require(position.index == value.length) { "Invalid ISO date: $value" }
            Calendar.getInstance().apply { time = format.parse(value)!! }
        }
    }

private fun formatIsoDate(calendar: Calendar): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time)
