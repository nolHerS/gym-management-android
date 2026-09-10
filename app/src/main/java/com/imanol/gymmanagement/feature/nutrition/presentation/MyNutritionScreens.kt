package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymLoading

@Composable
fun MyNutritionScreen(
    viewModel: MyNutritionViewModel,
    onPlanSelected: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.plans.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    HandleUnauthorized((state as? MyNutritionUiState.Failure)?.problem, onUnauthorized)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mi nutrición", style = MaterialTheme.typography.headlineSmall)
        when (val current = state) {
            MyNutritionUiState.Loading -> GymLoading()
            MyNutritionUiState.Empty -> Text("No tienes planes nutricionales.")
            is MyNutritionUiState.Failure -> ProblemContent(current.problem, viewModel::load)
            is MyNutritionUiState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (current.activePlans.isNotEmpty()) {
                    item {
                        Text("Planes activos", style = MaterialTheme.typography.titleMedium)
                    }
                    items(current.activePlans, key = { "active-${it.id}" }) { active ->
                        PlanSummary(active.name, active.startDate, active.endDate, active.status) {
                            onPlanSelected(active.id)
                        }
                    }
                }
                item { Text("Todos los planes", style = MaterialTheme.typography.titleMedium) }
                items(current.plans, key = { it.id }) { plan ->
                    PlanSummary(plan.name, plan.startDate, plan.endDate, plan.status) {
                        onPlanSelected(plan.id)
                    }
                }
            }
        }
    }
}

@Composable
fun MyNutritionPlanScreen(
    planId: Long,
    viewModel: MyNutritionViewModel,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    LaunchedEffect(planId) { viewModel.loadDetail(planId) }
    HandleUnauthorized((state as? NutritionPlanDetailUiState.Failure)?.problem, onUnauthorized)

    when (val current = state) {
        NutritionPlanDetailUiState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { GymLoading() }
        is NutritionPlanDetailUiState.Failure -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { ProblemContent(current.problem, retry = { viewModel.loadDetail(planId) }) }
        is NutritionPlanDetailUiState.Success -> LazyColumn(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NutritionPlanHeader(current.plan) }
            items(current.plan.meals.sortedBy { it.orderIndex }, key = { it.id }) { meal ->
                NutritionMealCard(
                    meal.name,
                    meal.description,
                    meal.orderIndex,
                    meal.foods.sortedBy { it.orderIndex }.map {
                        "${it.orderIndex}. ${it.food.name}: ${it.quantity} ${it.unit}"
                    },
                )
            }
        }
    }
}

@Composable
private fun PlanSummary(
    name: String,
    startDate: String,
    endDate: String?,
    status: String,
    onClick: () -> Unit,
) {
    GymCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("$startDate · ${endDate ?: "Sin fin"}")
            Text(status)
        }
    }
}
