package com.imanol.gymmanagement.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class UserSession(
    val token: String,
    val tokenType: String,
    val expiresAt: Long,
)

internal val Context.sessionDataStore by preferencesDataStore(name = "session")

internal object SessionDataStoreKeys {
    val encryptedSession = stringPreferencesKey("encrypted_session")
    val token = stringPreferencesKey("token")
    val tokenType = stringPreferencesKey("token_type")
    val expiresAt = longPreferencesKey("expires_at")
}

private const val SESSION_PAYLOAD_VERSION = 1

@Serializable
private data class SessionPayload(
    val version: Int,
    val token: String,
    val tokenType: String,
    val expiresAt: Long,
)

class InvalidSessionException(message: String) : IllegalArgumentException(message)

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: SessionCipher,
) {
    val session: Flow<UserSession?> = context.sessionDataStore.data
        .map { preferences -> readSession(preferences) }
        .catch { emit(null) }

    suspend fun saveSession(
        token: String,
        tokenType: String,
        expiresIn: Long,
    ) {
        if (expiresIn <= 0L) {
            throw InvalidSessionException("Session expiration is invalid")
        }
        val expiresAt = System.currentTimeMillis() + expiresIn
        validateSessionValues(token, tokenType, expiresAt)

        val payload = Json.encodeToString(
            SessionPayload(
                version = SESSION_PAYLOAD_VERSION,
                token = token,
                tokenType = tokenType,
                expiresAt = expiresAt,
            ),
        )
        val encryptedSession = cipher.encrypt(payload)

        context.sessionDataStore.edit { preferences ->
            preferences[SessionDataStoreKeys.encryptedSession] = encryptedSession
            removeLegacyKeys(preferences)
        }
    }

    suspend fun getValidSession(): UserSession? {
        val storedSession = session.first() ?: return null
        if (storedSession.expiresAt > System.currentTimeMillis()) {
            return storedSession
        }

        clearSession()
        return null
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private suspend fun readSession(
        preferences: androidx.datastore.preferences.core.Preferences,
    ): UserSession? {
        val encryptedSession = preferences[SessionDataStoreKeys.encryptedSession]
        if (encryptedSession != null) {
            return try {
                decodePayload(cipher.decrypt(encryptedSession))
            } catch (_: Exception) {
                clearSession()
                null
            }
        }

        val legacySession = try {
            readLegacySession(preferences)
        } catch (_: InvalidSessionException) {
            clearSession()
            return null
        } ?: return null
        return try {
            val encryptedPayload = cipher.encrypt(
                Json.encodeToString(
                    SessionPayload(
                        version = SESSION_PAYLOAD_VERSION,
                        token = legacySession.token,
                        tokenType = legacySession.tokenType,
                        expiresAt = legacySession.expiresAt,
                    ),
                ),
            )
            context.sessionDataStore.edit { currentPreferences ->
                currentPreferences[SessionDataStoreKeys.encryptedSession] = encryptedPayload
                removeLegacyKeys(currentPreferences)
            }
            legacySession
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    private fun readLegacySession(
        preferences: androidx.datastore.preferences.core.Preferences,
    ): UserSession? {
        val token = preferences[SessionDataStoreKeys.token]
        val tokenType = preferences[SessionDataStoreKeys.tokenType]
        val expiresAt = preferences[SessionDataStoreKeys.expiresAt]

        if (token == null && tokenType == null && expiresAt == null) {
            return null
        }

        validateSessionValues(token, tokenType, expiresAt)
        return UserSession(
            token = requireNotNull(token),
            tokenType = requireNotNull(tokenType),
            expiresAt = requireNotNull(expiresAt),
        )
    }

    private fun decodePayload(payload: String): UserSession {
        val decoded = Json.decodeFromString<SessionPayload>(payload)
        require(decoded.version == SESSION_PAYLOAD_VERSION) { "Unsupported session payload" }
        validateSessionValues(decoded.token, decoded.tokenType, decoded.expiresAt)
        return UserSession(decoded.token, decoded.tokenType, decoded.expiresAt)
    }

    private fun validateSessionValues(
        token: String?,
        tokenType: String?,
        expiresAt: Long?,
    ) {
        if (token.isNullOrBlank()) throw InvalidSessionException("Session token is blank")
        if (tokenType.isNullOrBlank()) throw InvalidSessionException("Session token type is blank")
        if (expiresAt == null || expiresAt <= 0L) {
            throw InvalidSessionException("Session expiration is invalid")
        }
    }

    private fun removeLegacyKeys(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
    ) {
        preferences.remove(SessionDataStoreKeys.token)
        preferences.remove(SessionDataStoreKeys.tokenType)
        preferences.remove(SessionDataStoreKeys.expiresAt)
    }
}
