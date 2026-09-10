package com.imanol.gymmanagement.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SessionStatus {
    Checking,
    Authenticated,
    Unauthenticated,
}

@Singleton
class SessionManager @Inject constructor(
    private val sessionDataStore: SessionDataStore,
) {
    private val invalidationMutex = Mutex()
    private val _status = MutableStateFlow(SessionStatus.Checking)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    suspend fun refresh(): UserSession? {
        val session = sessionDataStore.getValidSession()
        _status.value = if (session == null) {
            SessionStatus.Unauthenticated
        } else {
            SessionStatus.Authenticated
        }
        return session
    }

    suspend fun currentSession(): UserSession? =
        sessionDataStore.getValidSession().also { session ->
            _status.value = if (session == null) {
                SessionStatus.Unauthenticated
            } else {
                SessionStatus.Authenticated
            }
        }

    suspend fun saveSession(token: String, tokenType: String, expiresIn: Long) {
        sessionDataStore.saveSession(token, tokenType, expiresIn)
        _status.value = SessionStatus.Authenticated
    }

    suspend fun logout() {
        invalidationMutex.withLock {
            sessionDataStore.clearSession()
            _status.value = SessionStatus.Unauthenticated
        }
    }

    suspend fun invalidate() {
        invalidationMutex.withLock {
            if (_status.value == SessionStatus.Unauthenticated) return
            sessionDataStore.clearSession()
            _status.value = SessionStatus.Unauthenticated
        }
    }

    suspend fun invalidateIfCurrent(token: String) {
        invalidationMutex.withLock {
            val current = sessionDataStore.getValidSession()
            if (current?.token == token) {
                sessionDataStore.clearSession()
                _status.value = SessionStatus.Unauthenticated
            }
        }
    }
}
