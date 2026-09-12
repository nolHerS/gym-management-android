package com.imanol.gymmanagement.core.network

import com.imanol.gymmanagement.core.domain.AppException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

object AppExceptionMapper {
    fun map(throwable: Throwable): AppException = throwable.toAppException()
}

fun Throwable.toAppException(): AppException {
    if (this is CancellationException) throw this
    return when (this) {
        is AppException -> this
        is HttpException -> when (code()) {
            400 -> AppException.BadRequest(this)
            401 -> AppException.Unauthorized(this)
            403 -> AppException.Forbidden(this)
            404 -> AppException.NotFound(this)
            409 -> AppException.Conflict(this)
            in 500..599 -> AppException.Server(this)
            else -> AppException.Unexpected(this)
        }
        is SerializationException -> AppException.Serialization(this)
        is IllegalStateException -> AppException.InvalidResponse(message ?: "Respuesta inválida.", this)
        is IOException -> AppException.Network(this)
        else -> AppException.Unexpected(this)
    }
}

suspend fun <T> networkCall(block: suspend () -> T): T = try {
    block()
} catch (throwable: Throwable) {
    throw throwable.toAppException()
}
