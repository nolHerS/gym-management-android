package com.imanol.gymmanagement.feature.client.domain

interface ClientRepository {
    suspend fun getTrainerClients(): List<Client>

    suspend fun getClientDetail(clientId: Long): Client
}
