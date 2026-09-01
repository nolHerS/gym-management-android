package com.imanol.gymmanagement.core.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.imanol.gymmanagement.core.session.SessionDataStore
import com.imanol.gymmanagement.feature.auth.data.remote.AuthApi
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class JwtAuthInterceptorTest {
    private lateinit var sessionDataStore: SessionDataStore
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionDataStore = SessionDataStore(context)
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
                .body("""{"id":"42"}""")
                .build(),
        )

        val authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        JwtAuthInterceptor(
                            object : Lazy<SessionDataStore> {
                                override fun get(): SessionDataStore = sessionDataStore
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

        authApi.getUser("42")

        assertEquals("Bearer test-jwt", server.takeRequest().headers["Authorization"])
    }
}
