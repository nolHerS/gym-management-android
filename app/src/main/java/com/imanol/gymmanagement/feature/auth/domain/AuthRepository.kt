package com.imanol.gymmanagement.feature.auth.domain

interface AuthRepository {
    suspend fun getCurrentUser(): User
}
