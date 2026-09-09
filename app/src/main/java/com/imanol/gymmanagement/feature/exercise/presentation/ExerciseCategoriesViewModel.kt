package com.imanol.gymmanagement.feature.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface ExerciseCategoriesUiState {
    data object Loading : ExerciseCategoriesUiState
    data class Success(val categories: List<ExerciseCategory>) : ExerciseCategoriesUiState
    data class Error(val message: String) : ExerciseCategoriesUiState
    data object Unauthorized : ExerciseCategoriesUiState
}

@HiltViewModel
class ExerciseCategoriesViewModel @Inject constructor(
    private val getExerciseCategories: GetExerciseCategoriesUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private val _uiState =
        MutableStateFlow<ExerciseCategoriesUiState>(ExerciseCategoriesUiState.Loading)
    val uiState: StateFlow<ExerciseCategoriesUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    internal constructor(
        getExerciseCategories: GetExerciseCategoriesUseCase,
        scope: CoroutineScope,
    ) : this(getExerciseCategories) {
        providedScope = scope
    }

    fun loadCategories() {
        if (loadJob?.isActive == true) return

        _uiState.value = ExerciseCategoriesUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                _uiState.value =
                    ExerciseCategoriesUiState.Success(getExerciseCategories())
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401 || exception.code() == 403) {
                    ExerciseCategoriesUiState.Unauthorized
                } else {
                    ExerciseCategoriesUiState.Error(
                        "No se pudieron cargar las categorías. Inténtalo de nuevo.",
                    )
                }
            } catch (_: IOException) {
                _uiState.value = ExerciseCategoriesUiState.Error(
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }
}
