package com.imanol.gymmanagement.feature.client.domain

data class Client(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val active: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
