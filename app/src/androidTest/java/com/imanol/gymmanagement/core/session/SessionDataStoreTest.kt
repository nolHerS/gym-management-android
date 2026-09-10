package com.imanol.gymmanagement.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class SessionDataStoreTest {
    private lateinit var context: Context
    private lateinit var dataStore: SessionDataStore
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        dataStore = SessionDataStore(context, AndroidSessionCipher())
        sessionManager = SessionManager(dataStore)
        dataStore.clearSession()
    }

    @After
    fun tearDown() = runBlocking {
        dataStore.clearSession()
        deleteSessionKey()
    }

    @Test
    fun savesAndRestoresSession() = runBlocking {
        dataStore.saveSession("token-1", "Bearer", 60_000)

        val session = dataStore.getValidSession()

        assertEquals("token-1", session?.token)
        assertEquals("Bearer", session?.tokenType)
        assertTrue(session!!.expiresAt > System.currentTimeMillis())
        assertEncryptedStorage()
    }

    @Test
    fun consecutiveSessionsReplacePreviousSession() = runBlocking {
        dataStore.saveSession("token-1", "Bearer", 60_000)
        dataStore.saveSession("token-2", "Bearer", 60_000)

        assertEquals("token-2", dataStore.getValidSession()?.token)
    }

    @Test
    fun clearSessionRemovesEncryptedAndLegacyData() = runBlocking {
        dataStore.saveSession("token-1", "Bearer", 60_000)
        dataStore.clearSession()

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun invalidSessionValuesAreRejected() = runBlocking {
        assertInvalidSave("", "Bearer", 60_000)
        assertInvalidSave("token", "", 60_000)
        assertInvalidSave("token", "Bearer", 0)
        assertInvalidSave("token", "Bearer", -1)
    }

    @Test
    fun corruptCiphertextIsCleared() = runBlocking {
        writeEncryptedValue("{invalid")

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun corruptIvIsCleared() = runBlocking {
        dataStore.saveSession("token", "Bearer", 60_000)
        val envelope = encryptedValue()
        val corrupted = buildJsonObject {
            put("version", envelope["version"]!!)
            put("iv", "not-base64")
            put("ciphertext", envelope["ciphertext"]!!)
        }.toString()
        writeEncryptedValue(corrupted)

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun corruptPayloadIsCleared() = runBlocking {
        writeEncryptedValue(AndroidSessionCipher().encrypt("not-a-session-payload"))

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun unknownFormatVersionIsCleared() = runBlocking {
        writeEncryptedValue(
            """{"version":999,"iv":"AA==","ciphertext":"AA=="}""",
        )

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun missingKeystoreKeyIsCleared() = runBlocking {
        dataStore.saveSession("token", "Bearer", 60_000)
        deleteSessionKey()

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun migratesLegacyPlaintextSessionAndRemovesLegacyKeys() = runBlocking {
        val expiresAt = System.currentTimeMillis() + 60_000
        context.sessionDataStore.edit { preferences ->
            preferences[SessionDataStoreKeys.token] = "legacy-token"
            preferences[SessionDataStoreKeys.tokenType] = "Bearer"
            preferences[SessionDataStoreKeys.expiresAt] = expiresAt
        }

        val session = dataStore.getValidSession()
        val preferences = context.sessionDataStore.data.first()

        assertEquals("legacy-token", session?.token)
        assertNotNull(preferences[SessionDataStoreKeys.encryptedSession])
        assertFalse(preferences.contains(SessionDataStoreKeys.token))
        assertFalse(preferences.contains(SessionDataStoreKeys.tokenType))
        assertFalse(preferences.contains(SessionDataStoreKeys.expiresAt))
        assertFalse(
            preferences[SessionDataStoreKeys.encryptedSession]
                ?.contains("legacy-token") == true,
        )
    }

    @Test
    fun failedLegacyMigrationClearsDataWithoutCrashing() = runBlocking {
        context.sessionDataStore.edit { preferences ->
            preferences[SessionDataStoreKeys.token] = ""
            preferences[SessionDataStoreKeys.tokenType] = "Bearer"
            preferences[SessionDataStoreKeys.expiresAt] = System.currentTimeMillis() + 60_000
        }

        assertNull(dataStore.getValidSession())
        assertTrue(context.sessionDataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun sessionManagerRefreshesValidAndExpiredSessions() = runBlocking {
        sessionManager.saveSession("manager-token", "Bearer", 60_000)
        assertNotNull(sessionManager.refresh())
        assertEquals(SessionStatus.Authenticated, sessionManager.status.value)

        dataStore.clearSession()
        sessionManager.saveSession("short-lived", "Bearer", 1)
        delay(10)
        assertNull(sessionManager.refresh())
        assertEquals(SessionStatus.Unauthenticated, sessionManager.status.value)
    }

    @Test
    fun sessionManagerLogoutAndInvalidateClearSession() = runBlocking {
        sessionManager.saveSession("manager-token", "Bearer", 60_000)
        sessionManager.invalidate()
        assertNull(dataStore.getValidSession())

        sessionManager.saveSession("manager-token-2", "Bearer", 60_000)
        sessionManager.logout()
        assertNull(dataStore.getValidSession())
        assertEquals(SessionStatus.Unauthenticated, sessionManager.status.value)
    }

    private suspend fun assertInvalidSave(token: String, tokenType: String, expiresIn: Long) {
        try {
            dataStore.saveSession(token, tokenType, expiresIn)
            throw AssertionError("Expected InvalidSessionException")
        } catch (_: InvalidSessionException) {
            assertNull(dataStore.getValidSession())
        }
    }

    private suspend fun writeEncryptedValue(value: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[SessionDataStoreKeys.encryptedSession] = value
        }
    }

    private suspend fun encryptedValue() =
        Json.parseToJsonElement(
            context.sessionDataStore.data.first()[SessionDataStoreKeys.encryptedSession]!!,
        ).jsonObject

    private suspend fun assertEncryptedStorage() {
        val preferences = context.sessionDataStore.data.first()
        val encrypted = preferences[SessionDataStoreKeys.encryptedSession]
        assertNotNull(encrypted)
        assertFalse(encrypted!!.contains("token-1"))
        assertFalse(preferences.contains(SessionDataStoreKeys.token))
        assertFalse(preferences.contains(SessionDataStoreKeys.tokenType))
        assertFalse(preferences.contains(SessionDataStoreKeys.expiresAt))
    }

    private fun deleteSessionKey() {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(SESSION_KEY_ALIAS)) {
                deleteEntry(SESSION_KEY_ALIAS)
            }
        }
    }
}
