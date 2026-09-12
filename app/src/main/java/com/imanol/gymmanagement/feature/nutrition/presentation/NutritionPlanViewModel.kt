package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.nutrition.domain.CompleteNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.CreateNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.DeactivateNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.Food
import com.imanol.gymmanagement.feature.nutrition.domain.GetClientNutritionPlansUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodsUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.MealFoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.MealInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlan
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanInput
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanStatus
import com.imanol.gymmanagement.feature.nutrition.domain.UpdateNutritionPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NutritionPlansUiState {
    data object Loading : NutritionPlansUiState
    data object Empty : NutritionPlansUiState
    data class Success(val plans: List<NutritionPlan>) : NutritionPlansUiState
    data class Failure(val problem: NutritionProblem) : NutritionPlansUiState
}

sealed interface NutritionPlanDetailUiState {
    data object Loading : NutritionPlanDetailUiState
    data class Success(val plan: NutritionPlan, val actionInProgress: Boolean = false) :
        NutritionPlanDetailUiState
    data class Failure(val problem: NutritionProblem) : NutritionPlanDetailUiState
}

data class MealFoodDraft(
    val localId: Long,
    val foodId: Long,
    val quantity: String = "1",
    val unit: String = "g",
    val orderIndex: String = "1",
)

data class MealDraft(
    val localId: Long,
    val name: String = "",
    val description: String = "",
    val orderIndex: String = "1",
    val foods: List<MealFoodDraft> = emptyList(),
)

data class NutritionPlanFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = NutritionPlanStatus.ACTIVE,
    val meals: List<MealDraft> = emptyList(),
    val foods: List<Food> = emptyList(),
    val problem: NutritionProblem? = null,
    val savedPlanId: Long? = null,
)

@HiltViewModel
class NutritionPlanViewModel @Inject constructor(
    private val getClientPlans: GetClientNutritionPlansUseCase,
    private val getPlan: GetNutritionPlanUseCase,
    private val createPlan: CreateNutritionPlanUseCase,
    private val updatePlan: UpdateNutritionPlanUseCase,
    private val deactivatePlan: DeactivateNutritionPlanUseCase,
    private val completePlan: CompleteNutritionPlanUseCase,
    private val getFoods: GetFoodsUseCase,
) : ViewModel() {
    private var scope: CoroutineScope = viewModelScope
    private val _plans = MutableStateFlow<NutritionPlansUiState>(NutritionPlansUiState.Loading)
    val plans = _plans.asStateFlow()
    private val _detail =
        MutableStateFlow<NutritionPlanDetailUiState>(NutritionPlanDetailUiState.Loading)
    val detail = _detail.asStateFlow()
    private val _form = MutableStateFlow(NutritionPlanFormState())
    val form = _form.asStateFlow()
    private var selectedStatus: String? = null
    private var loadedClientId: Long? = null
    private var nextLocalId = 1L

    internal constructor(
        getClientPlans: GetClientNutritionPlansUseCase,
        getPlan: GetNutritionPlanUseCase,
        createPlan: CreateNutritionPlanUseCase,
        updatePlan: UpdateNutritionPlanUseCase,
        deactivatePlan: DeactivateNutritionPlanUseCase,
        completePlan: CompleteNutritionPlanUseCase,
        getFoods: GetFoodsUseCase,
        scope: CoroutineScope,
    ) : this(
        getClientPlans,
        getPlan,
        createPlan,
        updatePlan,
        deactivatePlan,
        completePlan,
        getFoods,
    ) {
        this.scope = scope
    }

    fun loadPlans(clientId: Long, status: String? = selectedStatus) {
        selectedStatus = status
        loadedClientId = clientId
        _plans.value = NutritionPlansUiState.Loading
        scope.launch {
            try {
                val result = getClientPlans(clientId, status)
                _plans.value = if (result.isEmpty()) {
                    NutritionPlansUiState.Empty
                } else {
                    NutritionPlansUiState.Success(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Exception) {
                _plans.value = NutritionPlansUiState.Failure(
                    throwable.toNutritionProblem("No se pudieron cargar los planes."),
                )
            }
        }
    }

    fun loadDetail(planId: Long) {
        _detail.value = NutritionPlanDetailUiState.Loading
        scope.launch {
            try {
                _detail.value = NutritionPlanDetailUiState.Success(getPlan(planId))
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Exception) {
                _detail.value = NutritionPlanDetailUiState.Failure(
                    throwable.toNutritionProblem("No se pudo cargar el plan."),
                )
            }
        }
    }

    fun prepareForm(planId: Long?) {
        _form.value = NutritionPlanFormState(loading = true)
        scope.launch {
            try {
                val foods = getFoods().filter { it.active }
                _form.value = if (planId == null) {
                    NutritionPlanFormState(foods = foods)
                } else {
                    val plan = getPlan(planId)
                    NutritionPlanFormState(
                        name = plan.name,
                        description = plan.description.orEmpty(),
                        startDate = plan.startDate,
                        endDate = plan.endDate.orEmpty(),
                        status = plan.status,
                        meals = plan.meals.sortedBy { it.orderIndex }.map { meal ->
                            MealDraft(
                                localId = localId(),
                                name = meal.name,
                                description = meal.description.orEmpty(),
                                orderIndex = meal.orderIndex.toString(),
                                foods = meal.foods.sortedBy { it.orderIndex }.map {
                                    MealFoodDraft(
                                        localId(),
                                        it.food.id,
                                        it.quantity,
                                        it.unit,
                                        it.orderIndex.toString(),
                                    )
                                },
                            )
                        },
                        foods = foods,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Exception) {
                _form.value = NutritionPlanFormState(
                    problem = throwable.toNutritionProblem("No se pudo preparar el formulario."),
                )
            }
        }
    }

    fun updatePlanInfo(
        name: String = _form.value.name,
        description: String = _form.value.description,
        startDate: String = _form.value.startDate,
        endDate: String = _form.value.endDate,
        status: String = _form.value.status,
    ) {
        _form.update {
            it.copy(
                name = name,
                description = description,
                startDate = startDate,
                endDate = endDate,
                status = status,
                problem = null,
            )
        }
    }

    fun addMeal() {
        _form.update {
            it.copy(
                meals = it.meals + MealDraft(
                    localId = localId(),
                    orderIndex = (it.meals.size + 1).toString(),
                ),
                problem = null,
            )
        }
    }

    fun removeMeal(localId: Long) {
        _form.update { it.copy(meals = it.meals.filterNot { meal -> meal.localId == localId }) }
    }

    fun updateMeal(localId: Long, transform: (MealDraft) -> MealDraft) {
        _form.update { state ->
            state.copy(
                meals = state.meals.map { if (it.localId == localId) transform(it) else it },
                problem = null,
            )
        }
    }

    fun addFood(mealId: Long, foodId: Long) {
        updateMeal(mealId) { meal ->
            if (meal.foods.any { it.foodId == foodId }) {
                meal
            } else {
                meal.copy(
                    foods = meal.foods + MealFoodDraft(
                        localId = localId(),
                        foodId = foodId,
                        orderIndex = (meal.foods.size + 1).toString(),
                    ),
                )
            }
        }
    }

    fun updateFood(mealId: Long, draft: MealFoodDraft) {
        updateMeal(mealId) { meal ->
            meal.copy(foods = meal.foods.map { if (it.localId == draft.localId) draft else it })
        }
    }

    fun removeFood(mealId: Long, foodLocalId: Long) {
        updateMeal(mealId) { meal ->
            meal.copy(foods = meal.foods.filterNot { it.localId == foodLocalId })
        }
    }

    fun save(clientId: Long, planId: Long?) {
        val state = _form.value
        val input = state.toInput(planId != null)
        input.validationError(requireStatus = planId != null)?.let { error ->
            _form.update {
                it.copy(problem = NutritionProblem(NutritionFailure.VALIDATION, error))
            }
            return
        }
        _form.update { it.copy(saving = true, problem = null) }
        scope.launch {
            try {
                val saved = if (planId == null) {
                    createPlan(clientId, input)
                } else {
                    updatePlan(planId, input)
                }
                _form.update { it.copy(saving = false, savedPlanId = saved.id) }
                loadedClientId?.let { loadPlans(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Exception) {
                _form.update {
                    it.copy(
                        saving = false,
                        problem = throwable.toNutritionProblem("No se pudo guardar el plan."),
                    )
                }
            }
        }
    }

    fun deactivate(planId: Long) = runAction(planId) { deactivatePlan(it) }

    fun complete(planId: Long) = runAction(planId) { completePlan(it) }

    private fun runAction(planId: Long, action: suspend (Long) -> Unit) {
        val current = _detail.value
        if (current is NutritionPlanDetailUiState.Success) {
            _detail.value = current.copy(actionInProgress = true)
        }
        scope.launch {
            try {
                action(planId)
                loadDetail(planId)
                loadedClientId?.let { loadPlans(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Exception) {
                _detail.value = NutritionPlanDetailUiState.Failure(
                    throwable.toNutritionProblem("No se pudo actualizar el plan."),
                )
            }
        }
    }

    private fun localId() = nextLocalId++
}

private fun NutritionPlanFormState.toInput(editing: Boolean) = NutritionPlanInput(
    name = name.trim(),
    description = description.trim().ifBlank { null },
    startDate = startDate.trim(),
    endDate = endDate.trim().ifBlank { null },
    status = status.takeIf { editing },
    meals = meals.map { meal ->
        MealInput(
            name = meal.name.trim(),
            description = meal.description.trim().ifBlank { null },
            orderIndex = meal.orderIndex.toIntOrNull() ?: 0,
            foods = meal.foods.map {
                MealFoodInput(
                    foodId = it.foodId,
                    quantity = it.quantity.trim(),
                    unit = it.unit.trim(),
                    orderIndex = it.orderIndex.toIntOrNull() ?: 0,
                )
            },
        )
    },
)
