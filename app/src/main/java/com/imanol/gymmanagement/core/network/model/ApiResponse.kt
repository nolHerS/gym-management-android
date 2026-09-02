package com.imanol.gymmanagement.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val timestamp: String,
    val status: Int,
    val error: String? = null,
    val message: String,
    val data: T? = null,
)
