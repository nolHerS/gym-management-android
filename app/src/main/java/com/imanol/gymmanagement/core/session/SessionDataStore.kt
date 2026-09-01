package com.imanol.gymmanagement.core.session

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserSession(
    val token: String,
    val tokenType: String,
    val expiresAt: Long,
)

private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val token = stringPreferencesKey("token")
        val tokenType = stringPreferencesKey("token_type")
        val expiresAt = longPreferencesKey("expires_at")
    }

    val session: Flow<UserSession?> = context.sessionDataStore.data.map { preferences ->
        val token = preferences[Keys.token]
        val tokenType = preferences[Keys.tokenType]
        val expiresAt = preferences[Keys.expiresAt]

        if (token != null && tokenType != null && expiresAt != null) {
            UserSession(
                token = token,
                tokenType = tokenType,
                expiresAt = expiresAt,
            )
        } else {
            null
        }
    }

    suspend fun saveSession(
        token: String,
        tokenType: String,
        expiresIn: Long,
    ) {
        context.sessionDataStore.edit { preferences ->
            preferences[Keys.token] = token
            preferences[Keys.tokenType] = tokenType
            preferences[Keys.expiresAt] = System.currentTimeMillis() + expiresIn
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
}
