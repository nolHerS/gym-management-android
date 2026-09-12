package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.exercise.domain.Exercise
import com.imanol.gymmanagement.feature.exercise.domain.ExerciseCategory
import com.imanol.gymmanagement.feature.exercise.domain.GetExerciseCategoriesUseCase
import com.imanol.gymmanagement.feature.exercise.domain.GetExercisesByCategoryUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.AddWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.AddWorkoutPlanExerciseUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeleteWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeleteWorkoutPlanExerciseUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDay
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExercise
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanExerciseRequest
import com.imanol.gymmanagement.feature.workoutplan.domain.UpdateWorkoutPlanExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkoutPlanStructureState {
    data object Idle : WorkoutPlanStructureState
    data object Loading : WorkoutPlanStructureState
    data class Success(val plan: WorkoutPlan) : WorkoutPlanStructureState
    data class Error(val message: String) : WorkoutPlanStructureState
}

sealed interface WorkoutPlanStructureMutationState {
    data object Idle : WorkoutPlanStructureMutationState
    data object AddingDay : WorkoutPlanStructureMutationState
    data object DeletingDay : WorkoutPlanStructureMutationState
    data object AddingExercise : WorkoutPlanStructureMutationState
    data object UpdatingExercise : WorkoutPlanStructureMutationState
    data object DeletingExercise : WorkoutPlanStructureMutationState
    data class Success(val message: String) : WorkoutPlanStructureMutationState
    data class Error(val message: String) : WorkoutPlanStructureMutationState
}

sealed interface WorkoutPlanExerciseCatalogState {
    data object Idle : WorkoutPlanExerciseCatalogState
    data object Loading : WorkoutPlanExerciseCatalogState
    data class Categories(val categories: List<ExerciseCategory>) : WorkoutPlanExerciseCatalogState
    data class Success(val exercises: List<Exercise>) : WorkoutPlanExerciseCatalogState
    data class Error(val message: String) : WorkoutPlanExerciseCatalogState
}

@HiltViewModel
class WorkoutPlanStructureViewModel @Inject constructor(
    private val getPlan: GetWorkoutPlanDetailUseCase,
    private val addDayUseCase: AddWorkoutPlanDayUseCase,
    private val deleteDayUseCase: DeleteWorkoutPlanDayUseCase,
    private val addExerciseUseCase: AddWorkoutPlanExerciseUseCase,
    private val updateExerciseUseCase: UpdateWorkoutPlanExerciseUseCase,
    private val deleteExerciseUseCase: DeleteWorkoutPlanExerciseUseCase,
    private val getExerciseCategories: GetExerciseCategoriesUseCase,
    private val getExercisesByCategory: GetExercisesByCategoryUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkoutPlanStructureState>(WorkoutPlanStructureState.Idle)
    val state = _state.asStateFlow()
    private val _mutation = MutableStateFlow<WorkoutPlanStructureMutationState>(
        WorkoutPlanStructureMutationState.Idle,
    )
    val mutation = _mutation.asStateFlow()
    private val _catalog = MutableStateFlow<WorkoutPlanExerciseCatalogState>(
        WorkoutPlanExerciseCatalogState.Idle,
    )
    val catalog = _catalog.asStateFlow()
    private var loadedPlanId: Long? = null
    private var scope: CoroutineScope = viewModelScope

    internal constructor(
        getPlan: GetWorkoutPlanDetailUseCase,
        addDayUseCase: AddWorkoutPlanDayUseCase,
        deleteDayUseCase: DeleteWorkoutPlanDayUseCase,
        addExerciseUseCase: AddWorkoutPlanExerciseUseCase,
        updateExerciseUseCase: UpdateWorkoutPlanExerciseUseCase,
        deleteExerciseUseCase: DeleteWorkoutPlanExerciseUseCase,
        getExerciseCategories: GetExerciseCategoriesUseCase,
        getExercisesByCategory: GetExercisesByCategoryUseCase,
        scope: CoroutineScope,
    ) : this(
        getPlan,
        addDayUseCase,
        deleteDayUseCase,
        addExerciseUseCase,
        updateExerciseUseCase,
        deleteExerciseUseCase,
        getExerciseCategories,
        getExercisesByCategory,
    ) {
        this.scope = scope
    }

    fun load(planId: Long) {
        loadedPlanId = planId
        scope.launch {
            _state.value = WorkoutPlanStructureState.Loading
            try {
                _state.value = WorkoutPlanStructureState.Success(getPlan(planId))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.value = WorkoutPlanStructureState.Error(exception.toAppException().messageForStructure())
            }
        }
    }

    fun loadIfNeeded(planId: Long, seedPlan: WorkoutPlan? = null) {
        val current = (_state.value as? WorkoutPlanStructureState.Success)?.plan
        if (current?.id == planId) return
        if (seedPlan?.id == planId) {
            loadedPlanId = planId
            _state.value = WorkoutPlanStructureState.Success(seedPlan)
            return
        }
        load(planId)
    }

    fun setPlan(plan: WorkoutPlan) {
        loadedPlanId = plan.id
        _state.value = WorkoutPlanStructureState.Success(plan)
    }

    fun retry() {
        loadedPlanId?.let(::load)
    }

    fun addDay(planId: Long, request: WorkoutPlanDayRequest) {
        if (isMutating()) return
        val current = currentPlan(planId) ?: return
        if (current.status != "ACTIVE" ||
            request.dayOfWeek !in 1..7 ||
            request.exercises.isEmpty() ||
            current.days.any { it.dayOfWeek == request.dayOfWeek }
        ) {
            _mutation.value = WorkoutPlanStructureMutationState.Error(
                "El día no puede añadirse con los datos actuales.",
            )
            return
        }
        _mutation.value = WorkoutPlanStructureMutationState.AddingDay
        scope.launch {
            try {
                val day = addDayUseCase(planId, request)
                _state.value = WorkoutPlanStructureState.Success(
                    current.copy(days = (current.days + day).sortedBy { it.dayOfWeek }),
                )
                _mutation.value = WorkoutPlanStructureMutationState.Success("Día añadido correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _mutation.value = WorkoutPlanStructureMutationState.Error(
                    exception.toAppException().messageForStructureMutation(),
                )
            }
        }
    }

    fun deleteDay(planId: Long, dayOfWeek: Int) {
        if (isMutating()) return
        val current = currentPlan(planId) ?: return
        if (current.status != "ACTIVE" || current.days.none { it.dayOfWeek == dayOfWeek }) {
            _mutation.value = WorkoutPlanStructureMutationState.Error(
                "El día no puede eliminarse con los datos actuales.",
            )
            return
        }
        _mutation.value = WorkoutPlanStructureMutationState.DeletingDay
        scope.launch {
            try {
                deleteDayUseCase(planId, dayOfWeek)
                _state.value = WorkoutPlanStructureState.Success(
                    current.copy(days = current.days.filterNot { it.dayOfWeek == dayOfWeek }),
                )
                _mutation.value = WorkoutPlanStructureMutationState.Success("Día eliminado correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _mutation.value = WorkoutPlanStructureMutationState.Error(
                    exception.toAppException().messageForStructureMutation(),
                )
            }
        }
    }

    fun loadExerciseCatalog() {
        if (_catalog.value is WorkoutPlanExerciseCatalogState.Loading) return
        _catalog.value = WorkoutPlanExerciseCatalogState.Loading
        scope.launch {
            try {
                _catalog.value = WorkoutPlanExerciseCatalogState.Categories(
                    getExerciseCategories().filter { it.active },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _catalog.value = WorkoutPlanExerciseCatalogState.Error(
                    exception.toAppException().messageForCatalog(),
                )
            }
        }
    }

    fun loadExercises(categoryId: Long) {
        _catalog.value = WorkoutPlanExerciseCatalogState.Loading
        scope.launch {
            try {
                _catalog.value = WorkoutPlanExerciseCatalogState.Success(
                    getExercisesByCategory(categoryId).filter { it.active },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _catalog.value = WorkoutPlanExerciseCatalogState.Error(
                    exception.toAppException().messageForCatalog(),
                )
            }
        }
    }

    fun addExercise(planId: Long, dayOfWeek: Int, request: WorkoutPlanExerciseRequest) {
        if (isMutating()) return
        val current = currentPlan(planId)
        val day = current?.days?.find { it.dayOfWeek == dayOfWeek }
        validateExercise(current, day, dayOfWeek, request)?.let {
            _mutation.value = WorkoutPlanStructureMutationState.Error(it)
            return
        }
        _mutation.value = WorkoutPlanStructureMutationState.AddingExercise
        scope.launch {
            try {
                val added = addExerciseUseCase(planId, dayOfWeek, request)
                updateDay(dayOfWeek) { it + added }
                _mutation.value = WorkoutPlanStructureMutationState.Success("Ejercicio añadido correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _mutation.value = WorkoutPlanStructureMutationState.Error(
                    exception.toAppException().messageForExerciseMutation(),
                )
            }
        }
    }

    fun updateExercise(planId: Long, exerciseId: Long, request: WorkoutPlanExerciseRequest) {
        if (isMutating()) return
        val current = currentPlan(planId)
        val existing = current?.days?.flatMap { it.exercises }?.find { it.id == exerciseId }
        val day = current?.days?.find { it.exercises.any { exercise -> exercise.id == exerciseId } }
        val validation = validateExercise(current, day, day?.dayOfWeek ?: 0, request, exerciseId)
        if (existing == null || day == null || validation != null) {
            _mutation.value = WorkoutPlanStructureMutationState.Error(
                validation ?: "El ejercicio no existe.",
            )
            return
        }
        val existingDayOfWeek = day.dayOfWeek
        _mutation.value = WorkoutPlanStructureMutationState.UpdatingExercise
        scope.launch {
            try {
                val updated = updateExerciseUseCase(exerciseId, request)
                updateDay(existingDayOfWeek) { exercises ->
                    exercises.map { if (it.id == exerciseId) updated else it }
                }
                _mutation.value = WorkoutPlanStructureMutationState.Success("Ejercicio actualizado correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _mutation.value = WorkoutPlanStructureMutationState.Error(
                    exception.toAppException().messageForExerciseMutation(),
                )
            }
        }
    }

    fun deleteExercise(planId: Long, exerciseId: Long) {
        if (isMutating()) return
        val current = currentPlan(planId)
        val exists = current?.days?.any { it.exercises.any { exercise -> exercise.id == exerciseId } } == true
        if (current == null || current.status != "ACTIVE" || !exists) {
            _mutation.value = WorkoutPlanStructureMutationState.Error("El ejercicio no existe.")
            return
        }
        _mutation.value = WorkoutPlanStructureMutationState.DeletingExercise
        scope.launch {
            try {
                deleteExerciseUseCase(exerciseId)
                _state.value = WorkoutPlanStructureState.Success(
                    current.copy(
                        days = current.days.map { day ->
                            day.copy(exercises = day.exercises.filterNot { it.id == exerciseId })
                        },
                    ),
                )
                _mutation.value = WorkoutPlanStructureMutationState.Success("Ejercicio eliminado correctamente.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _mutation.value = WorkoutPlanStructureMutationState.Error(
                    exception.toAppException().messageForExerciseMutation(),
                )
            }
        }
    }

    private fun validateExercise(
        plan: WorkoutPlan?,
        day: WorkoutPlanDay?,
        dayOfWeek: Int,
        request: WorkoutPlanExerciseRequest,
        editingId: Long? = null,
    ): String? = when {
        plan == null || plan.status != "ACTIVE" -> "El ejercicio no puede modificarse con el estado actual del plan."
        dayOfWeek !in 1..7 || day == null -> "El día no existe."
        request.exerciseId == null && request.sourceTemplateExerciseId == null -> "Selecciona un ejercicio."
        request.orderIndex < 1 || request.sets < 1 ||
            request.repetitions < 1 || request.restSeconds < 0 ->
            "Los datos del ejercicio no son válidos."
        day.exercises.any {
            it.id != editingId && request.exerciseId != null && it.exercise?.id == request.exerciseId
        } -> "El ejercicio ya existe en este día."
        day.exercises.any { it.id != editingId && it.orderIndex == request.orderIndex } ->
            "El orden ya existe en este día."
        else -> null
    }

    private fun updateDay(
        dayOfWeek: Int,
        transform: (List<WorkoutPlanExercise>) -> List<WorkoutPlanExercise>,
    ) {
        val current = (_state.value as? WorkoutPlanStructureState.Success)?.plan ?: return
        _state.value = WorkoutPlanStructureState.Success(
            current.copy(
                days = current.days.map { day ->
                    if (day.dayOfWeek == dayOfWeek) {
                        day.copy(exercises = transform(day.exercises).sortedBy { it.orderIndex })
                    } else {
                        day
                    }
                },
            ),
        )
    }

    private fun currentPlan(planId: Long): WorkoutPlan? =
        (_state.value as? WorkoutPlanStructureState.Success)?.plan?.takeIf { it.id == planId }

    private fun isMutating(): Boolean =
        _mutation.value is WorkoutPlanStructureMutationState.AddingDay ||
            _mutation.value is WorkoutPlanStructureMutationState.DeletingDay ||
            _mutation.value is WorkoutPlanStructureMutationState.AddingExercise ||
            _mutation.value is WorkoutPlanStructureMutationState.UpdatingExercise ||
            _mutation.value is WorkoutPlanStructureMutationState.DeletingExercise
}

private fun AppException.messageForStructure(): String = when (this) {
    is AppException.BadRequest -> "Los datos del plan no son válidos."
    is AppException.Forbidden -> "No tienes permisos para consultar este plan."
    is AppException.NotFound -> "El plan no existe."
    is AppException.Conflict -> "El plan ha cambiado. Recarga el plan antes de continuar."
    is AppException.Server -> "El servidor no está disponible. Inténtalo de nuevo."
    is AppException.Network -> "No se ha podido conectar con el servidor."
    is AppException.Serialization, is AppException.InvalidResponse ->
        "La respuesta del servidor no es válida."
    else -> "Ha ocurrido un error inesperado."
}

private fun AppException.messageForStructureMutation(): String = when (this) {
    is AppException.BadRequest -> "El día no puede añadirse o eliminarse con los datos actuales."
    is AppException.Forbidden -> "No tienes permisos para modificar este plan."
    is AppException.NotFound -> "El plan o el día no existe."
    is AppException.Conflict -> "El plan ha cambiado. Recarga el plan antes de continuar."
    is AppException.Server -> "El servidor no está disponible. Inténtalo de nuevo."
    is AppException.Network -> "No se ha podido conectar con el servidor."
    is AppException.Serialization, is AppException.InvalidResponse ->
        "La respuesta del servidor no es válida."
    else -> "Ha ocurrido un error inesperado."
}

private fun AppException.messageForExerciseMutation(): String = when (this) {
    is AppException.BadRequest -> "Los datos del ejercicio no son válidos."
    is AppException.Forbidden -> "No tienes permisos para modificar este plan."
    is AppException.NotFound -> "El plan, día o ejercicio no existe."
    is AppException.Conflict -> "El plan ha cambiado. Recarga el plan antes de continuar."
    is AppException.Server -> "El servidor no está disponible. Inténtalo de nuevo."
    is AppException.Network -> "No se ha podido conectar con el servidor."
    is AppException.Serialization, is AppException.InvalidResponse ->
        "La respuesta del servidor no es válida."
    else -> "Ha ocurrido un error inesperado."
}

private fun AppException.messageForCatalog(): String = when (this) {
    is AppException.Network -> "No se ha podido conectar con el servidor."
    is AppException.Serialization, is AppException.InvalidResponse ->
        "La respuesta del servidor no es válida."
    is AppException.Forbidden -> "No tienes permisos para consultar los ejercicios."
    else -> "No se pudieron cargar los ejercicios."
}
