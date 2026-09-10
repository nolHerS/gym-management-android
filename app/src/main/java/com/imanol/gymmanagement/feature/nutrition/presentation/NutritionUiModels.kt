package com.imanol.gymmanagement.feature.nutrition.presentation

import java.io.IOException
import retrofit2.HttpException

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

internal fun Throwable.toNutritionProblem(defaultMessage: String): NutritionProblem = when (this) {
    is IllegalArgumentException -> NutritionProblem(
        NutritionFailure.VALIDATION,
        message ?: "Revisa los datos introducidos.",
    )
    is HttpException -> when (code()) {
        400 -> NutritionProblem(NutritionFailure.VALIDATION, "Revisa los datos introducidos.")
        401 -> NutritionProblem(NutritionFailure.UNAUTHORIZED, "La sesión ya no es válida.")
        403 -> NutritionProblem(NutritionFailure.FORBIDDEN, "Acceso denegado.")
        404 -> NutritionProblem(NutritionFailure.NOT_FOUND, "No se encontró el recurso.")
        409 -> NutritionProblem(NutritionFailure.CONFLICT, "La operación entra en conflicto con el estado actual.")
        else -> NutritionProblem(NutritionFailure.UNKNOWN, defaultMessage)
    }
    is IOException -> NutritionProblem(
        NutritionFailure.NETWORK,
        "No se pudo conectar con el servidor.",
    )
    else -> NutritionProblem(NutritionFailure.UNKNOWN, defaultMessage)
}
