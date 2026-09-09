package com.imanol.gymmanagement.feature.client.data

import com.imanol.gymmanagement.feature.auth.domain.AuthRepository
import com.imanol.gymmanagement.feature.client.data.remote.ClientApi
import com.imanol.gymmanagement.feature.client.data.remote.toClient
import com.imanol.gymmanagement.feature.client.domain.Client
import com.imanol.gymmanagement.feature.client.domain.ClientRepository
import javax.inject.Inject

class ClientRepositoryImpl @Inject constructor(
    private val clientApi: ClientApi,
    private val authRepository: AuthRepository,
) : ClientRepository {
    override suspend fun getTrainerClients(): List<Client> {
        val trainerId = authRepository.getCurrentUser().id
        val relationships = clientApi.getTrainerClients(trainerId).data
            ?: error("Trainer clients response did not contain data")

        return relationships.map { relationship ->
            clientApi.getClientById(relationship.clientId).data?.toClient()
                ?: error("Client response did not contain data")
        }
    }

    override suspend fun getClientDetail(clientId: Long): Client =
        clientApi.getClientById(clientId).data?.toClient()
            ?: error("Client response did not contain data")
}
