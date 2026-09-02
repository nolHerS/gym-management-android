package com.imanol.gymmanagement.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.auth.domain.GetCurrentUserUseCase
import com.imanol.gymmanagement.feature.auth.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
            } catch (exception: HttpException) {
                _uiState.value = if (exception.code() == 401 || exception.code() == 403) {
                    HomeUiState.Unauthorized
                } else {
                    HomeUiState.Error("No se pudo cargar el usuario. Inténtalo de nuevo.")
                }
            } catch (_: IOException) {
                _uiState.value = HomeUiState.Error(
                    "No se pudo conectar con el servidor. Inténtalo de nuevo.",
                )
            }
        }
    }
}
