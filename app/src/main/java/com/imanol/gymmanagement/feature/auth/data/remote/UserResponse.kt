package com.imanol.gymmanagement.feature.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Long,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "",
)
