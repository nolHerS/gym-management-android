package com.imanol.gymmanagement.feature.client.domain

import javax.inject.Inject

class GetTrainerClientsUseCase @Inject constructor(
    private val repository: ClientRepository,
) {
    suspend operator fun invoke(): List<Client> = repository.getTrainerClients()
}
