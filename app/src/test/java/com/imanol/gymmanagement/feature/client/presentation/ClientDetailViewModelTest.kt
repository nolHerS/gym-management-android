package com.imanol.gymmanagement.feature.client.presentation

import com.imanol.gymmanagement.feature.client.domain.Client
import com.imanol.gymmanagement.feature.client.domain.ClientRepository
import com.imanol.gymmanagement.feature.client.domain.GetClientDetailUseCase
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClientDetailViewModelTest {
    private val client = Client(2L, "Ana", "López", "ana@test.com", "CLIENT", true, null, null)

    @Test
    fun loadingBecomesSuccess() {
        val viewModel = viewModelFor(null)

        viewModel.loadClient(2L)

        assertEquals(ClientDetailUiState.Success(client), viewModel.uiState.value)
    }

    @Test
    fun networkFailureProducesErrorState() {
        val viewModel = viewModelFor(IOException())

        viewModel.loadClient(2L)

        assertTrue(viewModel.uiState.value is ClientDetailUiState.Error)
    }

    @Test
    fun unauthorizedResponseProducesUnauthorizedState() {
        val viewModel = viewModelFor(
            HttpException(Response.error<Unit>(401, "Unauthorized".toResponseBody())),
        )

        viewModel.loadClient(2L)

        assertEquals(ClientDetailUiState.Unauthorized, viewModel.uiState.value)
    }

    @Test
    fun notFoundResponseProducesErrorState() {
        val viewModel = viewModelFor(
            HttpException(Response.error<Unit>(404, "Not found".toResponseBody())),
        )

        viewModel.loadClient(2L)

        assertTrue(viewModel.uiState.value is ClientDetailUiState.Error)
    }

    private fun viewModelFor(exception: Exception?): ClientDetailViewModel =
        ClientDetailViewModel(
            GetClientDetailUseCase(
                object : ClientRepository {
                    override suspend fun getTrainerClients(): List<Client> = emptyList()
                    override suspend fun getClientDetail(clientId: Long): Client {
                        if (exception != null) throw exception
                        return client
                    }
                },
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
}
