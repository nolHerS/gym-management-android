package com.imanol.gymmanagement.core.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.imanol.gymmanagement.core.session.SessionDataStore
import com.imanol.gymmanagement.core.session.AndroidSessionCipher
import com.imanol.gymmanagement.core.session.SessionManager
import com.imanol.gymmanagement.feature.auth.data.remote.AuthApi
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(AndroidJUnit4::class)
class JwtAuthInterceptorTest {
    private lateinit var sessionDataStore: SessionDataStore
    private lateinit var sessionManager: SessionManager
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionDataStore = SessionDataStore(context, AndroidSessionCipher())
        sessionManager = SessionManager(sessionDataStore)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        runBlocking { sessionDataStore.clearSession() }
        server.close()
    }

    @Test
    fun addsJwtToProtectedRetrofitRequest() = runBlocking {
        sessionDataStore.saveSession(
            token = "test-jwt",
            tokenType = "Bearer",
            expiresIn = 60_000,
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"timestamp":"2026-09-02T22:54:39","status":200,"message":"User retrieved successfully","data":{"id":42}}""",
                )
                .build(),
        )

        val authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        JwtAuthInterceptor(
                            object : Lazy<SessionManager> {
                                override fun get(): SessionManager = sessionManager
                            },
                        ),
                    )
                    .build(),
            )
            .addConverterFactory(
                Json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(AuthApi::class.java)

        authApi.getCurrentUser()

        assertEquals("Bearer${" "}test-jwt", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun unauthorizedResponseInvalidatesSession() = runBlocking {
        sessionManager.saveSession("test-jwt", "Bearer", 60_000)
        server.enqueue(MockResponse.Builder().code(401).build())

        OkHttpClient.Builder()
            .addInterceptor(
                JwtAuthInterceptor(
                    object : Lazy<SessionManager> {
                        override fun get(): SessionManager = sessionManager
                    },
                ),
            )
            .build()
            .newCall(Request.Builder().url(server.url("/api/protected")).build())
            .execute()

        assertEquals(null, sessionManager.currentSession())
    }

    @Test
    fun forbiddenResponseKeepsSession() = runBlocking {
        sessionManager.saveSession("test-jwt", "Bearer", 60_000)
        server.enqueue(MockResponse.Builder().code(403).build())

        OkHttpClient.Builder()
            .addInterceptor(
                JwtAuthInterceptor(
                    object : Lazy<SessionManager> {
                        override fun get(): SessionManager = sessionManager
                    },
                ),
            )
            .build()
            .newCall(Request.Builder().url(server.url("/api/protected")).build())
            .execute()

        assertEquals("test-jwt", sessionManager.currentSession()?.token)
    }

    @Test
    fun loginUnauthorizedResponseDoesNotInvalidateSession() = runBlocking {
        sessionManager.saveSession("existing-jwt", "Bearer", 60_000)
        server.enqueue(MockResponse.Builder().code(401).build())

        OkHttpClient.Builder()
            .addInterceptor(
                JwtAuthInterceptor(
                    object : Lazy<SessionManager> {
                        override fun get(): SessionManager = sessionManager
                    },
                ),
            )
            .build()
            .newCall(Request.Builder().url(server.url("/api/auth/login")).build())
            .execute()

        assertEquals("existing-jwt", sessionManager.currentSession()?.token)
    }

    @Test
    fun concurrentUnauthorizedResponsesLeaveOneUnauthenticatedSession() = runBlocking {
        sessionManager.saveSession("concurrent-jwt", "Bearer", 60_000)
        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(401).build())
        val client = OkHttpClient.Builder()
            .addInterceptor(
                JwtAuthInterceptor(
                    object : Lazy<SessionManager> {
                        override fun get(): SessionManager = sessionManager
                    },
                ),
            )
            .build()

        listOf("/api/first", "/api/second").map { path ->
            async {
                client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()
            }
        }.awaitAll()

        assertEquals(null, sessionManager.currentSession())
    }
}
