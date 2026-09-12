package com.imanol.gymmanagement.core.network

import com.imanol.gymmanagement.core.domain.AppException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AppExceptionMapperTest {
    @Test
    fun mapsHttpStatusCodes() {
        assertEquals(AppException.BadRequest::class, httpException(400).toAppException()::class)
        assertEquals(AppException.Unauthorized::class, httpException(401).toAppException()::class)
        assertEquals(AppException.Forbidden::class, httpException(403).toAppException()::class)
        assertEquals(AppException.NotFound::class, httpException(404).toAppException()::class)
        assertEquals(AppException.Conflict::class, httpException(409).toAppException()::class)
        assertEquals(AppException.Server::class, httpException(500).toAppException()::class)
        assertEquals(AppException.Server::class, httpException(503).toAppException()::class)
    }

    @Test
    fun mapsNetworkAndTimeoutToNetwork() {
        assertEquals(AppException.Network::class, IOException().toAppException()::class)
        assertEquals(AppException.Network::class, SocketTimeoutException().toAppException()::class)
    }

    @Test
    fun mapsSerializationAndInvalidResponse() {
        assertEquals(
            AppException.Serialization::class,
            SerializationException("invalid json").toAppException()::class,
        )
        assertEquals(
            AppException.InvalidResponse::class,
            IllegalStateException("missing data").toAppException()::class,
        )
    }

    @Test
    fun mapsUnexpectedException() {
        assertEquals(
            AppException.Unexpected::class,
            IllegalArgumentException("unexpected").toAppException()::class,
        )
    }

    @Test
    fun preservesExistingAppException() {
        val exception = AppException.Network(IOException())

        assertSame(exception, exception.toAppException())
    }

    @Test
    fun propagatesCancellation() {
        val cancellation = CancellationException("cancelled")

        try {
            cancellation.toAppException()
            fail("CancellationException must be rethrown")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private fun httpException(code: Int) =
        HttpException(Response.error<Unit>(code, code.toString().toResponseBody()))
}
