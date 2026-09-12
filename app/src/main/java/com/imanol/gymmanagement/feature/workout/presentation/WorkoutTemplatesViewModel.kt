package com.imanol.gymmanagement.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.workout.domain.CreateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplateDetailUseCase
import com.imanol.gymmanagement.feature.workout.domain.GetWorkoutTemplatesUseCase
import com.imanol.gymmanagement.feature.workout.domain.UpdateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.ActivateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.DeactivateWorkoutTemplateUseCase
import com.imanol.gymmanagement.feature.workout.domain.WorkoutTemplate
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

sealed interface WorkoutTemplatesUiState {
    data object Loading : WorkoutTemplatesUiState
    data class Success(val templates: List<WorkoutTemplate>) : WorkoutTemplatesUiState
    data object Empty : WorkoutTemplatesUiState
    data class Error(val message: String) : WorkoutTemplatesUiState
    data object Unauthorized : WorkoutTemplatesUiState
}

data class WorkoutTemplateFormState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class WorkoutTemplatesViewModel @Inject constructor(
    private val getWorkoutTemplates: GetWorkoutTemplatesUseCase,
    private val getWorkoutTemplateDetail: GetWorkoutTemplateDetailUseCase,
    private val createWorkoutTemplate: CreateWorkoutTemplateUseCase,
    private val updateWorkoutTemplate: UpdateWorkoutTemplateUseCase,
    private val activateWorkoutTemplate: ActivateWorkoutTemplateUseCase,
    private val deactivateWorkoutTemplate: DeactivateWorkoutTemplateUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState =
        MutableStateFlow<WorkoutTemplatesUiState>(WorkoutTemplatesUiState.Loading)
    val uiState: StateFlow<WorkoutTemplatesUiState> = _uiState.asStateFlow()
    private val _formState = MutableStateFlow(WorkoutTemplateFormState())
    val formState: StateFlow<WorkoutTemplateFormState> = _formState.asStateFlow()

    internal constructor(
        getWorkoutTemplates: GetWorkoutTemplatesUseCase,
        getWorkoutTemplateDetail: GetWorkoutTemplateDetailUseCase,
        createWorkoutTemplate: CreateWorkoutTemplateUseCase,
        updateWorkoutTemplate: UpdateWorkoutTemplateUseCase,
        activateWorkoutTemplate: ActivateWorkoutTemplateUseCase,
        deactivateWorkoutTemplate: DeactivateWorkoutTemplateUseCase,
        scope: CoroutineScope,
    ) : this(
        getWorkoutTemplates,
        getWorkoutTemplateDetail,
        createWorkoutTemplate,
        updateWorkoutTemplate,
        activateWorkoutTemplate,
        deactivateWorkoutTemplate,
    ) {
        providedScope = scope
    }

    fun loadTemplates() {
        if (loadJob?.isActive == true) return

        _uiState.value = WorkoutTemplatesUiState.Loading
        loadJob = scope().launch {
            try {
                val templates = getWorkoutTemplates()
                _uiState.value = if (templates.isEmpty()) {
                    WorkoutTemplatesUiState.Empty
                } else {
                    WorkoutTemplatesUiState.Success(templates)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _uiState.value = if (exception is AppException.Unauthorized) {
                    WorkoutTemplatesUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    WorkoutTemplatesUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                } else {
                    WorkoutTemplatesUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudieron cargar las plantillas. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun prepareCreate() {
        _formState.value = WorkoutTemplateFormState()
    }

    fun loadTemplateForEdit(templateId: Long) {
        _formState.value = WorkoutTemplateFormState(isLoading = true)
        scope().launch {
            try {
                val template = getWorkoutTemplateDetail(templateId).template
                _formState.value = WorkoutTemplateFormState(
                    name = template.name,
                    description = template.description.orEmpty(),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _formState.value = WorkoutTemplateFormState(
                    errorMessage = if (exception is AppException.Unauthorized) {
                        "No autorizado."
                    } else if (exception is AppException.Network) {
                        "No se pudo conectar con el servidor. Inténtalo de nuevo."
                    } else {
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo cargar la plantilla. Inténtalo de nuevo."
                    },
                )
            }
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name, errorMessage = null)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description, errorMessage = null)
    }

    fun saveTemplate(templateId: Long?) {
        val state = _formState.value
        val name = state.name
        if (name.isBlank() || name.length > 150) {
            _formState.value = state.copy(
                errorMessage = "El nombre es obligatorio y no puede superar 150 caracteres.",
            )
            return
        }

        _formState.value = state.copy(isSaving = true, errorMessage = null, saved = false)
        scope().launch {
            try {
                if (templateId == null) {
                    createWorkoutTemplate(name, state.description.ifBlank { null })
                } else {
                    updateWorkoutTemplate(
                        templateId,
                        name,
                        state.description.ifBlank { null },
                    )
                }
                _formState.value = _formState.value.copy(isSaving = false, saved = true)
                loadTemplates()
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    errorMessage = if (exception is AppException.Unauthorized) {
                        "No autorizado."
                    } else if (exception is AppException.Network) {
                        "No se pudo conectar con el servidor. Inténtalo de nuevo."
                    } else {
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo guardar la plantilla. Inténtalo de nuevo."
                    },
                )
            }
        }
    }

    fun setActive(template: WorkoutTemplate) {
        scope().launch {
            try {
                if (template.active) {
                    deactivateWorkoutTemplate(template.id)
                } else {
                    activateWorkoutTemplate(template.id)
                }
                loadTemplates()
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _uiState.value = if (exception is AppException.Unauthorized) {
                    WorkoutTemplatesUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    WorkoutTemplatesUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                } else {
                    WorkoutTemplatesUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo actualizar el estado. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    private fun scope(): CoroutineScope = providedScope ?: viewModelScope
}
