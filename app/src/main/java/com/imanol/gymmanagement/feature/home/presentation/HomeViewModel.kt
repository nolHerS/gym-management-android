package com.imanol.gymmanagement.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.auth.domain.GetCurrentUserUseCase
import com.imanol.gymmanagement.feature.auth.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val user: User) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data object Unauthorized : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadUser() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Success(getCurrentUser())
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                val exception = throwable.toAppException()
                _uiState.value = if (exception is AppException.Unauthorized) {
                    HomeUiState.Unauthorized
                } else if (exception is AppException.Network) {
                    HomeUiState.Error(
                        "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                    )
                } else {
                    HomeUiState.Error(
                        if (exception is AppException.Forbidden) "Acceso denegado."
                        else "No se pudo cargar el usuario. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }
}
