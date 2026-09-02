package com.imanol.gymmanagement.feature.auth.domain

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
)
