package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyActiveNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyNutritionPlanUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.GetMyNutritionPlansUseCase
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyNutritionUiState {
    data object Loading : MyNutritionUiState
    data object Empty : MyNutritionUiState
    data class Success(
        val plans: List<NutritionPlan>,
        val activePlans: List<NutritionPlan>,
    ) : MyNutritionUiState
    data class Failure(val problem: NutritionProblem) : MyNutritionUiState
}

@HiltViewModel
class MyNutritionViewModel @Inject constructor(
    private val getPlans: GetMyNutritionPlansUseCase,
    private val getActive: GetMyActiveNutritionPlanUseCase,
    private val getPlan: GetMyNutritionPlanUseCase,
) : ViewModel() {
    private var scope: CoroutineScope = viewModelScope
    private val _plans = MutableStateFlow<MyNutritionUiState>(MyNutritionUiState.Loading)
    val plans = _plans.asStateFlow()
    private val _detail =
        MutableStateFlow<NutritionPlanDetailUiState>(NutritionPlanDetailUiState.Loading)
    val detail = _detail.asStateFlow()

    internal constructor(
        getPlans: GetMyNutritionPlansUseCase,
        getActive: GetMyActiveNutritionPlanUseCase,
        getPlan: GetMyNutritionPlanUseCase,
        scope: CoroutineScope,
    ) : this(getPlans, getActive, getPlan) {
        this.scope = scope
    }

    fun load() {
        _plans.value = MyNutritionUiState.Loading
        scope.launch {
            try {
                val plans = async { getPlans() }
                val active = async { getActive() }
                val all = plans.await()
                val activePlans = active.await()
                _plans.value = if (all.isEmpty() && activePlans.isEmpty()) {
                    MyNutritionUiState.Empty
                } else {
                    MyNutritionUiState.Success(all, activePlans)
                }
            } catch (throwable: Throwable) {
                _plans.value = MyNutritionUiState.Failure(
                    throwable.toNutritionProblem("No se pudo cargar tu nutrición."),
                )
            }
        }
    }

    fun loadDetail(id: Long) {
        _detail.value = NutritionPlanDetailUiState.Loading
        scope.launch {
            try {
                _detail.value = NutritionPlanDetailUiState.Success(getPlan(id))
            } catch (throwable: Throwable) {
                _detail.value = NutritionPlanDetailUiState.Failure(
                    throwable.toNutritionProblem("No se pudo cargar el plan."),
                )
            }
        }
    }
}
