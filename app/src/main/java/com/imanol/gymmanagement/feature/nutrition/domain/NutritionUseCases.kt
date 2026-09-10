package com.imanol.gymmanagement.feature.nutrition.domain

import javax.inject.Inject

class GetClientNutritionPlansUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(clientId: Long, status: String? = null) =
        repository.getClientPlans(clientId, status)
}

class GetNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long) = repository.getPlan(id)
}

class CreateNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(clientId: Long, input: NutritionPlanInput): NutritionPlan {
        input.validationError(requireStatus = false)?.let { throw IllegalArgumentException(it) }
        return repository.createPlan(clientId, input)
    }
}

class UpdateNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long, input: NutritionPlanInput): NutritionPlan {
        input.validationError(requireStatus = true)?.let { throw IllegalArgumentException(it) }
        return repository.updatePlan(id, input)
    }
}

class DeactivateNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long) = repository.deactivatePlan(id)
}

class CompleteNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long) = repository.completePlan(id)
}

class GetMyNutritionPlansUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke() = repository.getMyPlans()
}

class GetMyActiveNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke() = repository.getMyActivePlans()
}

class GetMyNutritionPlanUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long) = repository.getMyPlan(id)
}

class GetFoodsUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke() = repository.getFoods()
}

class GetFoodUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long) = repository.getFood(id)
}

class SaveFoodUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long?, input: FoodInput): Food {
        input.validationError()?.let { throw IllegalArgumentException(it) }
        return if (id == null) repository.createFood(input) else repository.updateFood(id, input)
    }
}

class SetFoodActiveUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(id: Long, active: Boolean) {
        if (active) repository.activateFood(id) else repository.deactivateFood(id)
    }
}
