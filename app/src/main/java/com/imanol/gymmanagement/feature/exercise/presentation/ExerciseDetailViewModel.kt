package com.imanol.gymmanagement.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseDetailUseCase
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

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState
    data class Success(val exercise: Exercise) : ExerciseDetailUiState
    data class Error(val message: String) : ExerciseDetailUiState
    data object Unauthorized : ExerciseDetailUiState
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val getExerciseDetail: GetExerciseDetailUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<ExerciseDetailUiState>(
        ExerciseDetailUiState.Loading,
    )
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    internal constructor(
        getExerciseDetail: GetExerciseDetailUseCase,
        scope: CoroutineScope,
    ) : this(getExerciseDetail) {
        providedScope = scope
    }

    fun loadExercise(exerciseId: Long) {
        if (loadJob?.isActive == true) return

        _uiState.value = ExerciseDetailUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                _uiState.value = ExerciseDetailUiState.Success(getExerciseDetail(exerciseId))
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401 || exception.code() == 403) {
                    ExerciseDetailUiState.Unauthorized
                } else {
                    ExerciseDetailUiState.Error(
                        "No se pudo cargar el ejercicio. Inténtalo de nuevo.",
                    )
                }
            } catch (_: IOException) {
                _uiState.value = ExerciseDetailUiState.Error(
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }
}
