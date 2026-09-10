package com.imanol.gymmanagement.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.core.session.InvalidSessionException
import com.imanol.gymmanagement.core.session.SessionManager
import com.imanol.gymmanagement.feature.auth.data.remote.AuthApi
import com.imanol.gymmanagement.feature.auth.data.remote.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginSuccess(
    val tokenType: String,
    val expiresIn: Long,
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: LoginSuccess? = null,
)

enum class SessionState {
    Checking,
    Authenticated,
    Unauthenticated,
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    val sessionState: StateFlow<SessionState> =
        sessionManager.status.map { status ->
            when (status) {
                com.imanol.gymmanagement.core.session.SessionStatus.Checking -> SessionState.Checking
                com.imanol.gymmanagement.core.session.SessionStatus.Authenticated -> SessionState.Authenticated
                com.imanol.gymmanagement.core.session.SessionStatus.Unauthenticated -> SessionState.Unauthenticated
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            SessionState.Checking,
        )

    init {
        viewModelScope.launch {
            sessionManager.refresh()
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, success = null)
            }

            try {
                val response = authApi.login(
                    LoginRequest(
                        email = currentState.email,
                        password = currentState.password,
                    ),
                )
                sessionManager.saveSession(
                    token = response.token,
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        success = LoginSuccess(
                            tokenType = response.tokenType,
                            expiresIn = response.expiresIn,
                        ),
                    )
                }
            } catch (_: HttpException) {
                showGenericError()
            } catch (_: IOException) {
                showGenericError()
            } catch (_: InvalidSessionException) {
                showGenericError()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            _uiState.value = LoginUiState()
        }
    }

    private fun showGenericError() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = "No se pudo iniciar sesión. Inténtalo de nuevo.",
            )
        }
    }
}
