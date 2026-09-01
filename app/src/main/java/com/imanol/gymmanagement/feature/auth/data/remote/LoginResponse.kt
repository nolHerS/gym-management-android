package com.imanol.gymmanagement.feature.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
)
