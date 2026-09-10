package com.imanol.gymmanagement.feature.client.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.client.domain.Client
import com.imanol.gymmanagement.feature.client.domain.GetClientDetailUseCase
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

sealed interface ClientDetailUiState {
    data object Loading : ClientDetailUiState
    data class Success(val client: Client) : ClientDetailUiState
    data class Error(val message: String) : ClientDetailUiState
    data object Unauthorized : ClientDetailUiState
}

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val getClientDetail: GetClientDetailUseCase,
) : ViewModel() {
    private var providedScope: CoroutineScope? = null
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow<ClientDetailUiState>(
        ClientDetailUiState.Loading,
    )
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    internal constructor(
        getClientDetail: GetClientDetailUseCase,
        scope: CoroutineScope,
    ) : this(getClientDetail) {
        providedScope = scope
    }

    fun loadClient(clientId: Long) {
        if (loadJob?.isActive == true) return

        _uiState.value = ClientDetailUiState.Loading
        loadJob = (providedScope ?: viewModelScope).launch {
            try {
                _uiState.value = ClientDetailUiState.Success(getClientDetail(clientId))
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401) {
                    ClientDetailUiState.Unauthorized
                } else {
                    ClientDetailUiState.Error(
                        if (exception.code() == 403) "Acceso denegado."
                        else "No se pudo cargar el cliente. Inténtalo de nuevo.",
                    )
                }
            } catch (_: IOException) {
                _uiState.value = ClientDetailUiState.Error(
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }
}
