package com.imanol.gymmanagement.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface ExercisesUiState {
    data object Loading : ExercisesUiState
    data class Success(val exercises: List<Exercise>) : ExercisesUiState
    data object Empty : ExercisesUiState
    data class Error(val message: String) : ExercisesUiState
    data object Unauthorized : ExercisesUiState
}

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val getExercisesByCategory: GetExercisesByCategoryUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<ExercisesUiState>(ExercisesUiState.Loading)
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    internal constructor(
        getExercisesByCategory: GetExercisesByCategoryUseCase,
        scope: CoroutineScope,
    ) : this(getExercisesByCategory) {
        providedScope = scope
    }

    fun loadExercises(categoryId: Long) {
        if (loadJob?.isActive == true) return

        _uiState.value = ExercisesUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                val exercises = getExercisesByCategory(categoryId)
                _uiState.value = if (exercises.isEmpty()) {
                    ExercisesUiState.Empty
                } else {
                    ExercisesUiState.Success(exercises)
                }
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401) {
                    ExercisesUiState.Unauthorized
                } else {
                    ExercisesUiState.Error(
                        if (exception.code() == 403) "Acceso denegado."
                        else "No se pudieron cargar los ejercicios. Inténtalo de nuevo.",
                    )
                }
            } catch (_: IOException) {
                _uiState.value = ExercisesUiState.Error(
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }
}
