package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.nutrition.domain.Food
import com.imanol.gymmanagement.feature.nutrition.domain.FoodInput
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetFoodsUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.SaveFoodUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.SetFoodActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FoodsUiState {
    data object Loading : FoodsUiState
    data object Empty : FoodsUiState
    data class Success(val foods: List<Food>) : FoodsUiState
    data class Failure(val problem: NutritionProblem) : FoodsUiState
}

sealed interface FoodDetailUiState {
    data object Loading : FoodDetailUiState
    data class Success(val food: Food, val actionInProgress: Boolean = false) : FoodDetailUiState
    data class Failure(val problem: NutritionProblem) : FoodDetailUiState
}

data class FoodFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val description: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbohydrates: String = "",
    val fats: String = "",
    val servingSize: String = "",
    val servingUnit: String = "",
    val problem: NutritionProblem? = null,
    val savedFoodId: Long? = null,
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val getFoods: GetFoodsUseCase,
    private val getFood: GetFoodUseCase,
    private val saveFood: SaveFoodUseCase,
    private val setFoodActive: SetFoodActiveUseCase,
) : ViewModel() {
    private var scope: CoroutineScope = viewModelScope
    private val _foods = MutableStateFlow<FoodsUiState>(FoodsUiState.Loading)
    val foods = _foods.asStateFlow()
    private val _detail = MutableStateFlow<FoodDetailUiState>(FoodDetailUiState.Loading)
    val detail = _detail.asStateFlow()
    private val _form = MutableStateFlow(FoodFormState())
    val form = _form.asStateFlow()

    internal constructor(
        getFoods: GetFoodsUseCase,
        getFood: GetFoodUseCase,
        saveFood: SaveFoodUseCase,
        setFoodActive: SetFoodActiveUseCase,
        scope: CoroutineScope,
    ) : this(getFoods, getFood, saveFood, setFoodActive) {
        this.scope = scope
    }

    fun loadFoods() {
        _foods.value = FoodsUiState.Loading
        scope.launch {
            try {
                val result = getFoods()
                _foods.value = if (result.isEmpty()) FoodsUiState.Empty else FoodsUiState.Success(result)
            } catch (throwable: Throwable) {
                _foods.value = FoodsUiState.Failure(
                    throwable.toNutritionProblem("No se pudieron cargar los alimentos."),
                )
            }
        }
    }

    fun loadFood(id: Long) {
        _detail.value = FoodDetailUiState.Loading
        scope.launch {
            try {
                _detail.value = FoodDetailUiState.Success(getFood(id))
            } catch (throwable: Throwable) {
                _detail.value = FoodDetailUiState.Failure(
                    throwable.toNutritionProblem("No se pudo cargar el alimento."),
                )
            }
        }
    }

    fun prepareForm(id: Long?) {
        if (id == null) {
            _form.value = FoodFormState()
            return
        }
        _form.value = FoodFormState(loading = true)
        scope.launch {
            try {
                val food = getFood(id)
                _form.value = FoodFormState(
                    name = food.name,
                    description = food.description.orEmpty(),
                    calories = food.calories.orEmpty(),
                    protein = food.protein.orEmpty(),
                    carbohydrates = food.carbohydrates.orEmpty(),
                    fats = food.fats.orEmpty(),
                    servingSize = food.servingSize.orEmpty(),
                    servingUnit = food.servingUnit.orEmpty(),
                )
            } catch (throwable: Throwable) {
                _form.value = FoodFormState(
                    problem = throwable.toNutritionProblem("No se pudo cargar el alimento."),
                )
            }
        }
    }

    fun updateForm(transform: (FoodFormState) -> FoodFormState) {
        _form.update { transform(it).copy(problem = null) }
    }

    fun save(id: Long?) {
        val state = _form.value
        val input = FoodInput(
            state.name.trim(),
            state.description.trim().ifBlank { null },
            state.calories.trim().ifBlank { null },
            state.protein.trim().ifBlank { null },
            state.carbohydrates.trim().ifBlank { null },
            state.fats.trim().ifBlank { null },
            state.servingSize.trim().ifBlank { null },
            state.servingUnit.trim().ifBlank { null },
        )
        input.validationError()?.let { error ->
            _form.update {
                it.copy(problem = NutritionProblem(NutritionFailure.VALIDATION, error))
            }
            return
        }
        _form.update { it.copy(saving = true, problem = null) }
        scope.launch {
            try {
                val saved = saveFood(id, input)
                _form.update { it.copy(saving = false, savedFoodId = saved.id) }
                loadFoods()
            } catch (throwable: Throwable) {
                _form.update {
                    it.copy(
                        saving = false,
                        problem = throwable.toNutritionProblem("No se pudo guardar el alimento."),
                    )
                }
            }
        }
    }

    fun setActive(food: Food) {
        val current = _detail.value
        if (current is FoodDetailUiState.Success && current.food.id == food.id) {
            _detail.value = current.copy(actionInProgress = true)
        }
        scope.launch {
            try {
                setFoodActive(food.id, !food.active)
                loadFoods()
                if (current is FoodDetailUiState.Success) loadFood(food.id)
            } catch (throwable: Throwable) {
                val problem = throwable.toNutritionProblem("No se pudo actualizar el alimento.")
                if (current is FoodDetailUiState.Success) {
                    _detail.value = FoodDetailUiState.Failure(problem)
                } else {
                    _foods.value = FoodsUiState.Failure(problem)
                }
            }
        }
    }
}
