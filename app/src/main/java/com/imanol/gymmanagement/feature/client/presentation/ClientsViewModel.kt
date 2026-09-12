package com.imanol.gymmanagement.feature.client.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.client.domain.Client
import com.imanol.gymmanagement.feature.client.domain.GetTrainerClientsUseCase
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

sealed interface ClientsUiState {
    data object Loading : ClientsUiState
    data class Success(val clients: List<Client>) : ClientsUiState
    data object Empty : ClientsUiState
    data class Error(val message: String) : ClientsUiState
    data object Unauthorized : ClientsUiState
}

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val getTrainerClients: GetTrainerClientsUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<ClientsUiState>(ClientsUiState.Loading)
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    internal constructor(
        getTrainerClients: GetTrainerClientsUseCase,
        scope: CoroutineScope,
    ) : this(getTrainerClients) {
        providedScope = scope
    }

    fun loadClients() {
        if (loadJob?.isActive == true) return

        _uiState.value = ClientsUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                val clients = getTrainerClients()
                _uiState.value = if (clients.isEmpty()) {
                    ClientsUiState.Empty
                } else {
                    ClientsUiState.Success(clients)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _uiState.value = if (exception is AppException.Unauthorized) {
                    ClientsUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    ClientsUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                } else {
                    ClientsUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudieron cargar los clientes. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }
}
