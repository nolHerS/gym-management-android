package com.imanol.gymmanagement.feature.client.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTrainerClientsUseCaseTest {
    @Test
    fun returnsTrainerClientsFromRepository() = runBlocking {
        val clients = listOf(
            Client(2L, "Ana", "López", "ana@test.com", "CLIENT", true, null, null),
        )
        val useCase = GetTrainerClientsUseCase(
            object : ClientRepository {
                override suspend fun getTrainerClients(): List<Client> = clients
                override suspend fun getClientDetail(clientId: Long): Client =
                    error("Not used")
            },
        )

        assertEquals(clients, useCase())
    }
}
