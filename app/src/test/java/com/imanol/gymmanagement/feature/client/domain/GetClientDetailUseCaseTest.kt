package com.imanol.gymmanagement.feature.client.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetClientDetailUseCaseTest {
    @Test
    fun returnsClientDetailFromRepository() = runBlocking {
        val client = Client(2L, "Ana", "López", "ana@test.com", "CLIENT", true, null, null)
        val useCase = GetClientDetailUseCase(
            object : ClientRepository {
                override suspend fun getTrainerClients(): List<Client> = emptyList()
                override suspend fun getClientDetail(clientId: Long): Client = client
            },
        )

        assertEquals(client, useCase(2L))
    }
}
