package com.imanol.gymmanagement.feature.client.domain

import javax.inject.Inject

class GetClientDetailUseCase @Inject constructor(
    private val repository: ClientRepository,
) {
    suspend operator fun invoke(clientId: Long): Client =
        repository.getClientDetail(clientId)
}
