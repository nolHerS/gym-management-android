package com.imanol.gymmanagement.feature.client.data

import com.imanol.gymmanagement.feature.auth.data.remote.UserResponse
import com.imanol.gymmanagement.feature.client.data.remote.toClient
import com.imanol.gymmanagement.feature.client.domain.Client
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientMapperTest {
    @Test
    fun mapsUserResponseToClient() {
        val response = UserResponse(
            id = 2L,
            firstName = "Ana",
            lastName = "López",
            email = "ana@test.com",
            role = "CLIENT",
            active = true,
            createdAt = "2026-09-01T10:00:00",
            updatedAt = "2026-09-02T10:00:00",
        )

        assertEquals(
            Client(
                id = 2L,
                firstName = "Ana",
                lastName = "López",
                email = "ana@test.com",
                role = "CLIENT",
                active = true,
                createdAt = "2026-09-01T10:00:00",
                updatedAt = "2026-09-02T10:00:00",
            ),
            response.toClient(),
        )
    }
}
