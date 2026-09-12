package com.imanol.gymmanagement.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workout.domain.AddExerciseToWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.DeleteWorkoutTemplateExerciseUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.UpdateWorkoutTemplateExerciseUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplateDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException

sealed interface WorkoutTemplateDetailUiState {
    data object Loading : WorkoutTemplateDetailUiState
    data class Success(val detail: WorkoutTemplateDetail) : WorkoutTemplateDetailUiState
    data object Empty : WorkoutTemplateDetailUiState
    data class Error(val message: String) : WorkoutTemplateDetailUiState
    data object Unauthorized : WorkoutTemplateDetailUiState
}

sealed interface ExerciseCatalogUiState {
    data object Idle : ExerciseCatalogUiState
    data object Loading : ExerciseCatalogUiState
    data class Success(val exercises: List<Exercise>) : ExerciseCatalogUiState
    data class Error(val message: String) : ExerciseCatalogUiState
}

@HiltViewModel
class WorkoutTemplateDetailViewModel @Inject constructor(
    private val getWorkoutTemplateDetail: GetWorkoutTemplateDetailUseCase,
    private val addExercise: AddExerciseToWorkoutTemplateUseCase,
    private val updateExercise: UpdateWorkoutTemplateExerciseUseCase,
    private val deleteExercise: DeleteWorkoutTemplateExerciseUseCase,
    private val getExerciseCategories: GetExerciseCategoriesUseCase,
    private val getExercisesByCategory: GetExercisesByCategoryUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<WorkoutTemplateDetailUiState>(
        WorkoutTemplateDetailUiState.Loading,
    )
    val uiState: StateFlow<WorkoutTemplateDetailUiState> = _uiState.asStateFlow()
    private val _catalogState = MutableStateFlow<ExerciseCatalogUiState>(
        ExerciseCatalogUiState.Idle,
    )
    val catalogState: StateFlow<ExerciseCatalogUiState> = _catalogState.asStateFlow()
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    internal constructor(
        getWorkoutTemplateDetail: GetWorkoutTemplateDetailUseCase,
        addExercise: AddExerciseToWorkoutTemplateUseCase,
        updateExercise: UpdateWorkoutTemplateExerciseUseCase,
        deleteExercise: DeleteWorkoutTemplateExerciseUseCase,
        getExerciseCategories: GetExerciseCategoriesUseCase,
        getExercisesByCategory: GetExercisesByCategoryUseCase,
        scope: CoroutineScope,
    ) : this(
        getWorkoutTemplateDetail,
        addExercise,
        updateExercise,
        deleteExercise,
        getExerciseCategories,
        getExercisesByCategory,
    ) {
        providedScope = scope
    }

    fun loadDetail(templateId: Long) {
        if (loadJob?.isActive == true) return

        _uiState.value = WorkoutTemplateDetailUiState.Loading
        loadJob = scope().launch {
            try {
                val detail = getWorkoutTemplateDetail(templateId)
                _uiState.value = if (detail.exercises.isEmpty()) {
                    WorkoutTemplateDetailUiState.Success(detail)
                } else {
                    WorkoutTemplateDetailUiState.Success(detail)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _uiState.value = if (exception is AppException.Unauthorized) {
                    WorkoutTemplateDetailUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    WorkoutTemplateDetailUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                } else {
                    WorkoutTemplateDetailUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo cargar la plantilla. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun loadExerciseCatalog() {
        _catalogState.value = ExerciseCatalogUiState.Loading
        scope().launch {
            try {
                val exercises = getExerciseCategories()
                    .flatMap { getExercisesByCategory(it.id) }
                    .distinctBy { it.id }
                _catalogState.value = ExerciseCatalogUiState.Success(exercises)
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _catalogState.value = ExerciseCatalogUiState.Error(
                    if (exception is AppException.Network) {
                        "No se pudo conectar con el servidor."
                    } else {
                        "No se pudo cargar el catálogo de ejercicios."
                    },
                )
            }
        }
    }

    fun addExercise(
        templateId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ) {
        runAction {
            addExercise(
                templateId,
                exerciseId,
                orderIndex,
                sets,
                repetitions,
                restSeconds,
            )
        }
    }

    fun updateExercise(
        templateId: Long,
        templateExerciseId: Long,
        exerciseId: Long,
        orderIndex: Int,
        sets: Int,
        repetitions: Int,
        restSeconds: Int,
    ) {
        runAction {
            updateExercise(
                templateExerciseId,
                exerciseId,
                orderIndex,
                sets,
                repetitions,
                restSeconds,
            )
        }
    }

    fun deleteExercise(templateId: Long, templateExerciseId: Long) {
        runAction {
            deleteExercise(templateExerciseId)
        }
    }

    private fun runAction(action: suspend () -> Unit) {
        _actionError.value = null
        scope().launch {
            try {
                action()
                val templateId = (_uiState.value as? WorkoutTemplateDetailUiState.Success)
                    ?.detail?.template?.id
                if (templateId != null) loadDetail(templateId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                if (exception is AppException.Unauthorized) {
                    _uiState.value = WorkoutTemplateDetailUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    _actionError.value = "No se pudo conectar con el servidor. Inténtalo de nuevo."
                } else {
                    _actionError.value = if (exception is AppException.Forbidden) {
                        "Acceso denegado."
                    } else {
                        "No se pudo actualizar la plantilla. Inténtalo de nuevo."
                    }
                }
            }
        }
    }

    private fun scope(): CoroutineScope = providedScope ?: viewModelScope
}
