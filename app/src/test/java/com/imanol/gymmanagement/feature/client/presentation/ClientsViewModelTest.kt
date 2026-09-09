package com.imanol.gymmanagement.feature.client.presentation

import com.imanol.gymmanagement.feature.client.domain.Client
import com.imanol.gymmanagement.feature.client.domain.ClientRepository
import com.imanol.gymmanagement.feature.client.domain.GetTrainerClientsUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClientsViewModelTest {
    private val client = Client(2L, "Ana", "López", "ana@test.com", "CLIENT", true, null, null)

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor { listOf(client) }

        viewModel.loadClients()

        assertEquals(ClientsUiState.Success(listOf(client)), viewModel.uiState.value)
    }

    @Test
    fun emptyResponseProducesEmptyState() {
        val viewModel = viewModelFor { emptyList() }

        viewModel.loadClients()

        assertEquals(ClientsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor { throw IOException() }

        viewModel.loadClients()

        assertTrue(viewModel.uiState.value is ClientsUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor {
            throw HttpException(Response.error<Unit>(403, "Forbidden".toResponseBody()))
        }

        viewModel.loadClients()

        assertEquals(ClientsUiState.Unauthorized, viewModel.uiState.value)
    }

    private fun viewModelFor(
        result: suspend () -> List<Client>,
    ): ClientsViewModel =
        ClientsViewModel(
            GetTrainerClientsUseCase(
                object : ClientRepository {
                    override suspend fun getTrainerClients(): List<Client> = result()
                    override suspend fun getClientDetail(clientId: Long): Client =
                        error("Not used")
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
}
