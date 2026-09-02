package com.imanol.gymmanagement.feature.auth.domain

import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): User = repository.getCurrentUser()
}
