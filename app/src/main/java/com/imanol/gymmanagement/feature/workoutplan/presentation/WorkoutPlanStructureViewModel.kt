package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.AddWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.DeleteWorkoutPlanDayUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlanDayRequest
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
    data class Success(val message: String) : WorkoutPlanStructureMutationState
    data class Error(val message: String) : WorkoutPlanStructureMutationState
}

@HiltViewModel
class WorkoutPlanStructureViewModel @Inject constructor(
    private val getPlan: GetWorkoutPlanDetailUseCase,
    private val addDayUseCase: AddWorkoutPlanDayUseCase,
    private val deleteDayUseCase: DeleteWorkoutPlanDayUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkoutPlanStructureState>(
        WorkoutPlanStructureState.Idle,
    )
    val state = _state.asStateFlow()
    private val _mutation = MutableStateFlow<WorkoutPlanStructureMutationState>(
        WorkoutPlanStructureMutationState.Idle,
    )
    val mutation = _mutation.asStateFlow()

    private var loadedPlanId: Long? = null
    private var scope: CoroutineScope = viewModelScope

    internal constructor(
        getPlan: GetWorkoutPlanDetailUseCase,
        addDayUseCase: AddWorkoutPlanDayUseCase,
        deleteDayUseCase: DeleteWorkoutPlanDayUseCase,
        scope: CoroutineScope,
    ) : this(getPlan, addDayUseCase, deleteDayUseCase) {
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
                _state.value = WorkoutPlanStructureState.Error(
                    exception.toAppException().messageForStructure(),
                )
            }
        }
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

    private fun currentPlan(planId: Long): WorkoutPlan? =
        (_state.value as? WorkoutPlanStructureState.Success)
            ?.plan
            ?.takeIf { it.id == planId }

    private fun isMutating(): Boolean =
        _mutation.value is WorkoutPlanStructureMutationState.AddingDay ||
            _mutation.value is WorkoutPlanStructureMutationState.DeletingDay
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
