package com.imanol.gymmanagement.feature.client.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TrainerClientResponse(
    val id: Long,
    val trainerId: Long,
    val clientId: Long,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
