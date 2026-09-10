package com.imanol.gymmanagement.feature.nutrition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.imanol.gymmanagement.core.designsystem.component.GymLoading
import com.imanol.gymmanagement.core.designsystem.component.GymTextField
import com.imanol.gymmanagement.feature.nutrition.domain.Food

@Composable
fun FoodsScreen(
    viewModel: FoodViewModel,
    canManage: Boolean,
    onFoodSelected: (Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.foods.collectAsStateWithLifecycle()
    var pendingFood by remember { mutableStateOf<Food?>(null) }
    LaunchedEffect(Unit) { viewModel.loadFoods() }
    HandleUnauthorized((state as? FoodsUiState.Failure)?.problem, onUnauthorized)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alimentos", style = MaterialTheme.typography.headlineSmall)
            if (canManage) GymButton("Crear", onCreate)
        }
        when (val current = state) {
            FoodsUiState.Loading -> GymLoading()
            FoodsUiState.Empty -> Text("No hay alimentos disponibles.")
            is FoodsUiState.Failure -> ProblemContent(
                current.problem,
                retry = viewModel::loadFoods,
            )
            is FoodsUiState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(current.foods, key = { it.id }) { food ->
                    GymCard(
                        Modifier.fillMaxWidth(),
                        onClick = { onFoodSelected(food.id) },
                    ) {
                        Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) {
                            Text(food.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (food.active) "Activo" else "Inactivo")
                            FoodNutrition(food)
                            if (canManage) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GymButton("Editar", onClick = { onEdit(food.id) })
                                    GymButton(
                                        if (food.active) "Desactivar" else "Activar",
                                        onClick = { pendingFood = food },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingFood?.let { food ->
        AlertDialog(
            onDismissRequest = { pendingFood = null },
            title = { Text(if (food.active) "Desactivar alimento" else "Activar alimento") },
            text = { Text("¿Confirmas el cambio de estado?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingFood = null
                    viewModel.setActive(food)
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingFood = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
fun FoodDetailScreen(
    foodId: Long,
    viewModel: FoodViewModel,
    canManage: Boolean,
    onEdit: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(foodId) { viewModel.loadFood(foodId) }
    HandleUnauthorized((state as? FoodDetailUiState.Failure)?.problem, onUnauthorized)

    when (val current = state) {
        FoodDetailUiState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { GymLoading() }
        is FoodDetailUiState.Failure -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { ProblemContent(current.problem, retry = { viewModel.loadFood(foodId) }) }
        is FoodDetailUiState.Success -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(current.food.name, style = MaterialTheme.typography.headlineSmall)
            current.food.description?.let { Text(it) }
            Text(if (current.food.active) "Activo" else "Inactivo")
            FoodNutrition(current.food)
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymButton(
                        "Editar",
                        onClick = { onEdit(foodId) },
                        enabled = !current.actionInProgress,
                    )
                    GymButton(
                        if (current.food.active) "Desactivar" else "Activar",
                        onClick = { confirm = true },
                        enabled = !current.actionInProgress,
                    )
                }
            }
            if (confirm) AlertDialog(
                onDismissRequest = { confirm = false },
                title = { Text("Cambiar estado") },
                text = { Text("¿Confirmas esta acción?") },
                confirmButton = {
                    TextButton(onClick = {
                        confirm = false
                        viewModel.setActive(current.food)
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirm = false }) { Text("Cancelar") }
                },
            )
        }
    }
}

@Composable
fun FoodFormScreen(
    foodId: Long?,
    viewModel: FoodViewModel,
    onSaved: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val state by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(foodId) { viewModel.prepareForm(foodId) }
    LaunchedEffect(state.savedFoodId) { state.savedFoodId?.let(onSaved) }
    HandleUnauthorized(state.problem, onUnauthorized)

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GymLoading() }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                if (foodId == null) "Crear alimento" else "Editar alimento",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GymTextField(
                    state.name,
                    { value -> viewModel.updateForm { it.copy(name = value) } },
                    Modifier.fillMaxWidth(),
                    "Nombre",
                )
                GymTextField(
                    state.description,
                    { value -> viewModel.updateForm { it.copy(description = value) } },
                    Modifier.fillMaxWidth(),
                    "Descripción",
                    singleLine = false,
                )
                DecimalField("Calorías", state.calories) {
                    viewModel.updateForm { state -> state.copy(calories = it) }
                }
                DecimalField("Proteínas", state.protein) {
                    viewModel.updateForm { state -> state.copy(protein = it) }
                }
                DecimalField("Carbohidratos", state.carbohydrates) {
                    viewModel.updateForm { state -> state.copy(carbohydrates = it) }
                }
                DecimalField("Grasas", state.fats) {
                    viewModel.updateForm { state -> state.copy(fats = it) }
                }
                DecimalField("Tamaño de ración", state.servingSize) {
                    viewModel.updateForm { state -> state.copy(servingSize = it) }
                }
                GymTextField(
                    state.servingUnit,
                    { value -> viewModel.updateForm { it.copy(servingUnit = value) } },
                    Modifier.fillMaxWidth(),
                    "Unidad de ración",
                )
                state.problem?.let { ProblemContent(it) }
                GymButton(
                    "Guardar",
                    onClick = { viewModel.save(foodId) },
                    loading = state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DecimalField(label: String, value: String, onChange: (String) -> Unit) {
    GymTextField(
        value,
        onChange,
        Modifier.fillMaxWidth(),
        label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun FoodNutrition(food: Food) {
    val values = listOfNotNull(
        food.calories?.let { "Calorías: $it" },
        food.protein?.let { "Proteínas: $it" },
        food.carbohydrates?.let { "Carbohidratos: $it" },
        food.fats?.let { "Grasas: $it" },
        food.servingSize?.let { "Ración: $it" },
        food.servingUnit?.let { "Unidad: $it" },
    )
    if (values.isNotEmpty()) Text(values.joinToString(" · "))
}
