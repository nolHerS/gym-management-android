package com.imanol.gymmanagement.feature.client.data.remote

import com.imanol.gymmanagement.feature.auth.data.remote.UserResponse
import com.imanol.gymmanagement.feature.client.domain.Client

fun UserResponse.toClient(): Client =
    Client(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = role,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
