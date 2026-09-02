package com.imanol.gymmanagement.core.network

import com.imanol.gymmanagement.core.session.SessionDataStore
import dagger.Lazy
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class JwtAuthInterceptor @Inject constructor(
    private val sessionDataStore: Lazy<SessionDataStore>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.endsWith("/api/auth/login")) {
            return chain.proceed(request)
        }

        val session = runBlocking {
            sessionDataStore.get().getValidSession()
        } ?: return chain.proceed(request)
        val token = session.token.takeIf { it.isNotBlank() }
            ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "${session.tokenType} $token")
                .build(),
        )
    }
}
