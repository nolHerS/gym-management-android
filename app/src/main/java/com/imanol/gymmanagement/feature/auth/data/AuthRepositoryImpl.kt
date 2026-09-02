package com.imanol.gymmanagement.feature.auth.data

import com.imanol.gymmanagement.feature.auth.data.remote.AuthApi
import com.imanol.gymmanagement.feature.auth.domain.AuthRepository
import com.imanol.gymmanagement.feature.auth.domain.User
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
) : AuthRepository {
    override suspend fun getCurrentUser(): User =
        authApi.getCurrentUser().data?.let { response ->
            User(
                id = response.id,
                name = "${response.firstName} ${response.lastName}".trim(),
                email = response.email,
                role = response.role,
            )
        } ?: error("Current user response did not contain data")
}
