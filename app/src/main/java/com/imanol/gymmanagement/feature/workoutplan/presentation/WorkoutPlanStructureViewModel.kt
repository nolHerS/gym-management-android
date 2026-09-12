package com.imanol.gymmanagement.feature.workoutplan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException
import com.imanol.gymmanagement.feature.workoutplan.domain.GetWorkoutPlanDetailUseCase
import com.imanol.gymmanagement.feature.workoutplan.domain.WorkoutPlan
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

@HiltViewModel
class WorkoutPlanStructureViewModel @Inject constructor(
    private val getPlan: GetWorkoutPlanDetailUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkoutPlanStructureState>(
        WorkoutPlanStructureState.Idle,
    )
    val state = _state.asStateFlow()

    private var loadedPlanId: Long? = null
    private var scope: CoroutineScope = viewModelScope

    internal constructor(
        getPlan: GetWorkoutPlanDetailUseCase,
        scope: CoroutineScope,
    ) : this(getPlan) {
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
