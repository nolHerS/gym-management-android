package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading
import com.imanol.gymmanagement.core.designsystem.component.GymTextField
import com.imanol.gymmanagement.feature.nutrition.domain.Food
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlan
import com.imanol.gymmanagement.feature.nutrition.domain.NutritionPlanStatus

@Composable
fun NutritionPlansScreen(
    clientId: Long,
    viewModel: NutritionPlanViewModel,
    canManage: Boolean,
    onPlanSelected: (Long) -> Unit,
    onCreate: () -> Unit,
    onUnauthorized: () -> Unit,
    onAccessDenied: () -> Unit,
) {
    val state by viewModel.plans.collectAsStateWithLifecycle()
    var filter by remember(clientId) { mutableStateOf<String?>(null) }
    if (!canManage) {
        LaunchedEffect(Unit) { onAccessDenied() }
        return
    }
    LaunchedEffect(clientId, filter) { viewModel.loadPlans(clientId, filter) }
    HandleUnauthorized((state as? NutritionPlansUiState.Failure)?.problem, onUnauthorized)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Planes nutricionales", style = MaterialTheme.typography.headlineSmall)
            GymButton("Crear", onCreate)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                null to "Todos",
                NutritionPlanStatus.ACTIVE to "Activos",
                NutritionPlanStatus.COMPLETED to "Completados",
                NutritionPlanStatus.INACTIVE to "Inactivos",
            ).forEach { (value, label) ->
                Button(onClick = { filter = value }, enabled = filter != value) { Text(label) }
            }
        }
        when (val current = state) {
            NutritionPlansUiState.Loading -> GymLoading()
            NutritionPlansUiState.Empty -> Text("No hay planes para este filtro.")
            is NutritionPlansUiState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(current.plans, key = { it.id }) { plan ->
                    GymCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlanSelected(plan.id) },
                    ) {
                        Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleMedium)
                            plan.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            Text("${plan.startDate} · ${plan.endDate ?: "Sin fin"}")
                            Text(plan.status)
                            Text("${plan.meals.size} comidas")
                        }
                    }
                }
            }
            is NutritionPlansUiState.Failure -> ProblemContent(
                current.problem,
                retry = { viewModel.loadPlans(clientId, filter) },
            )
        }
    }
}

@Composable
fun NutritionPlanDetailScreen(
    planId: Long,
    viewModel: NutritionPlanViewModel,
    canManage: Boolean,
    onEdit: (Long, Long) -> Unit,
    onUnauthorized: () -> Unit,
    onAccessDenied: () -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    var confirmation by remember { mutableStateOf<String?>(null) }
    if (!canManage) {
        LaunchedEffect(Unit) { onAccessDenied() }
        return
    }
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
        ) {
            ProblemContent(current.problem, retry = { viewModel.loadDetail(planId) })
        }
        is NutritionPlanDetailUiState.Success -> LazyColumn(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NutritionPlanHeader(current.plan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymButton(
                        "Editar",
                        onClick = { onEdit(current.plan.clientId, current.plan.id) },
                        enabled = !current.actionInProgress,
                    )
                    if (current.plan.status == NutritionPlanStatus.ACTIVE) {
                        GymButton(
                            "Completar",
                            onClick = { confirmation = NutritionPlanStatus.COMPLETED },
                            enabled = !current.actionInProgress,
                        )
                        GymButton(
                            "Desactivar",
                            onClick = { confirmation = NutritionPlanStatus.INACTIVE },
                            enabled = !current.actionInProgress,
                        )
                    }
                }
            }
            items(current.plan.meals.sortedBy { it.orderIndex }, key = { it.id }) {
                NutritionMealCard(it.name, it.description, it.orderIndex, it.foods.map { food ->
                    "${food.orderIndex}. ${food.food.name}: ${food.quantity} ${food.unit}" +
                        foodNutritionSuffix(food.food)
                })
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (action == NutritionPlanStatus.COMPLETED) "Completar plan" else "Desactivar plan") },
            text = { Text("¿Confirmas esta acción?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    if (action == NutritionPlanStatus.COMPLETED) {
                        viewModel.complete(planId)
                    } else {
                        viewModel.deactivate(planId)
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
fun NutritionPlanFormScreen(
    clientId: Long,
    planId: Long?,
    viewModel: NutritionPlanViewModel,
    canManage: Boolean,
    onSaved: (Long) -> Unit,
    onUnauthorized: () -> Unit,
    onAccessDenied: () -> Unit,
) {
    val state by viewModel.form.collectAsStateWithLifecycle()
    if (!canManage) {
        LaunchedEffect(Unit) { onAccessDenied() }
        return
    }
    LaunchedEffect(clientId, planId) { viewModel.prepareForm(planId) }
    LaunchedEffect(state.savedPlanId) { state.savedPlanId?.let(onSaved) }
    HandleUnauthorized(state.problem, onUnauthorized)

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GymLoading() }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (planId == null) "Crear plan nutricional" else "Editar plan nutricional",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GymTextField(
                    state.name,
                    { viewModel.updatePlanInfo(name = it) },
                    Modifier.fillMaxWidth(),
                    "Nombre",
                )
                GymTextField(
                    state.description,
                    { viewModel.updatePlanInfo(description = it) },
                    Modifier.fillMaxWidth(),
                    "Descripción",
                    singleLine = false,
                )
                GymTextField(
                    state.startDate,
                    { viewModel.updatePlanInfo(startDate = it) },
                    Modifier.fillMaxWidth(),
                    "Inicio (AAAA-MM-DD)",
                )
                GymTextField(
                    state.endDate,
                    { viewModel.updatePlanInfo(endDate = it) },
                    Modifier.fillMaxWidth(),
                    "Fin opcional (AAAA-MM-DD)",
                )
                if (planId != null) StatusSelector(state.status) {
                    viewModel.updatePlanInfo(status = it)
                }
            }
        }
        items(state.meals, key = { it.localId }) { meal ->
            MealEditor(
                meal = meal,
                foods = state.foods,
                onUpdate = { viewModel.updateMeal(meal.localId, it) },
                onRemove = { viewModel.removeMeal(meal.localId) },
                onAddFood = { viewModel.addFood(meal.localId, it) },
                onUpdateFood = { viewModel.updateFood(meal.localId, it) },
                onRemoveFood = { viewModel.removeFood(meal.localId, it) },
            )
        }
        item {
            GymButton("Añadir comida", viewModel::addMeal)
            state.problem?.let { ProblemContent(it) }
            GymButton(
                "Guardar",
                onClick = { viewModel.save(clientId, planId) },
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MealEditor(
    meal: MealDraft,
    foods: List<Food>,
    onUpdate: ((MealDraft) -> MealDraft) -> Unit,
    onRemove: () -> Unit,
    onAddFood: (Long) -> Unit,
    onUpdateFood: (MealFoodDraft) -> Unit,
    onRemoveFood: (Long) -> Unit,
) {
    GymCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GymTextField(
                meal.name,
                { value -> onUpdate { it.copy(name = value) } },
                Modifier.fillMaxWidth(),
                "Comida",
            )
            GymTextField(
                meal.description,
                { value -> onUpdate { it.copy(description = value) } },
                Modifier.fillMaxWidth(),
                "Descripción",
                singleLine = false,
            )
            GymTextField(
                meal.orderIndex,
                { value -> onUpdate { it.copy(orderIndex = value) } },
                Modifier.fillMaxWidth(),
                "Orden",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            meal.foods.forEach { draft ->
                val food = foods.firstOrNull { it.id == draft.foodId }
                GymCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), Arrangement.spacedBy(6.dp)) {
                        Text(food?.name ?: "Alimento #${draft.foodId}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GymTextField(
                                draft.quantity,
                                { onUpdateFood(draft.copy(quantity = it)) },
                                Modifier.weight(1f),
                                "Cantidad",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            GymTextField(
                                draft.unit,
                                { onUpdateFood(draft.copy(unit = it)) },
                                Modifier.weight(1f),
                                "Unidad",
                            )
                            GymTextField(
                                draft.orderIndex,
                                { onUpdateFood(draft.copy(orderIndex = it)) },
                                Modifier.weight(1f),
                                "Orden",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        TextButton(onClick = { onRemoveFood(draft.localId) }) {
                            Text("Quitar alimento")
                        }
                    }
                }
            }
            FoodPicker(foods.filter { food -> meal.foods.none { it.foodId == food.id } }, onAddFood)
            TextButton(onClick = onRemove) { Text("Eliminar comida") }
        }
    }
}

@Composable
private fun FoodPicker(foods: List<Food>, onSelected: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }, enabled = foods.isNotEmpty()) {
            Text(if (foods.isEmpty()) "Sin alimentos disponibles" else "Añadir alimento")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            foods.forEach { food ->
                DropdownMenuItem(
                    text = { Text(food.name) },
                    onClick = {
                        expanded = false
                        onSelected(food.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun StatusSelector(status: String, onSelected: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NutritionPlanStatus.values.forEach { value ->
            Button(onClick = { onSelected(value) }, enabled = status != value) { Text(value) }
        }
    }
}

@Composable
internal fun NutritionPlanHeader(plan: NutritionPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(plan.name, style = MaterialTheme.typography.headlineSmall)
        plan.description?.let { Text(it) }
        Text("${plan.startDate} · ${plan.endDate ?: "Sin fin"}")
        Text("Estado: ${plan.status}")
    }
}

@Composable
internal fun NutritionMealCard(
    name: String,
    description: String?,
    order: Int,
    foodLines: List<String>,
) {
    GymCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
            Text("$order. $name", style = MaterialTheme.typography.titleMedium)
            description?.let { Text(it) }
            foodLines.forEach { Text(it) }
        }
    }
}

private fun foodNutritionSuffix(food: Food): String {
    val nutrition = listOfNotNull(
        food.calories?.let { "kcal $it" },
        food.protein?.let { "P $it" },
        food.carbohydrates?.let { "C $it" },
        food.fats?.let { "G $it" },
    )
    return nutrition.takeIf { it.isNotEmpty() }?.joinToString(prefix = " (", postfix = ")") ?: ""
}

@Composable
internal fun ProblemContent(problem: NutritionProblem, retry: (() -> Unit)? = null) {
    GymErrorMessage(problem.message)
    retry?.let { GymButton("Reintentar", it) }
}

@Composable
internal fun HandleUnauthorized(problem: NutritionProblem?, onUnauthorized: () -> Unit) {
    LaunchedEffect(problem?.failure) {
        if (problem?.failure == NutritionFailure.UNAUTHORIZED) onUnauthorized()
    }
}
