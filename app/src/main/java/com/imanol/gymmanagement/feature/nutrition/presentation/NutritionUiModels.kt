package com.imanol.gymmanagement.feature.nutrition.presentation

import com.imanol.gymmanagement.core.domain.AppException
import com.imanol.gymmanagement.core.network.toAppException

enum class NutritionFailure {
    UNAUTHORIZED,
    NOT_FOUND,
    VALIDATION,
    CONFLICT,
    NETWORK,
    UNKNOWN,
    FORBIDDEN,
}

data class NutritionProblem(
    val failure: NutritionFailure,
    val message: String,
)

internal fun Throwable.toNutritionProblem(defaultMessage: String): NutritionProblem {
    val appException = if (this is AppException) this else toAppException()
    return when (appException) {
    is AppException.BadRequest -> NutritionProblem(NutritionFailure.VALIDATION, "Revisa los datos introducidos.")
    is AppException.Unauthorized -> NutritionProblem(NutritionFailure.UNAUTHORIZED, "La sesión ya no es válida.")
    is AppException.Forbidden -> NutritionProblem(NutritionFailure.FORBIDDEN, "Acceso denegado.")
    is AppException.NotFound -> NutritionProblem(NutritionFailure.NOT_FOUND, "No se encontró el recurso.")
    is AppException.Conflict -> NutritionProblem(
        NutritionFailure.CONFLICT,
        "La operación entra en conflicto con el estado actual.",
    )
    is AppException.Network -> NutritionProblem(
        NutritionFailure.NETWORK,
        "No se pudo conectar con el servidor.",
    )
    is AppException.Serialization, is AppException.InvalidResponse, is AppException.Server,
    is AppException.Unexpected ->
        NutritionProblem(NutritionFailure.UNKNOWN, defaultMessage)
    }
}
