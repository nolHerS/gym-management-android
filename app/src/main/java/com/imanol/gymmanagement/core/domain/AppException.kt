package com.imanol.gymmanagement.core.domain

sealed class AppException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Network(cause: Throwable) : AppException("No se pudo conectar con el servidor.", cause)
    class BadRequest(cause: Throwable) : AppException("La solicitud no es válida.", cause)
    class Unauthorized(cause: Throwable) : AppException("La sesión no es válida.", cause)
    class Forbidden(cause: Throwable) : AppException("Acceso denegado.", cause)
    class NotFound(cause: Throwable) : AppException("No se encontró el recurso.", cause)
    class Conflict(cause: Throwable) : AppException("La operación entra en conflicto.", cause)
    class Server(cause: Throwable) : AppException("Se ha producido un error en el servidor.", cause)
    class Serialization(cause: Throwable) : AppException("La respuesta no es válida.", cause)
    class InvalidResponse(message: String, cause: Throwable? = null) : AppException(message, cause)
    class Unexpected(cause: Throwable) : AppException("Ha ocurrido un error inesperado.", cause)
}
