package com.imanol.gymmanagement.feature.exercise.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.imanol.gymmanagement.core.designsystem.component.GymButton
import com.imanol.gymmanagement.core.designsystem.component.GymCard
import com.imanol.gymmanagement.core.designsystem.component.GymErrorMessage
import com.imanol.gymmanagement.core.designsystem.component.GymLoading

@Composable
fun ExerciseCategoriesScreen(
    viewModel: ExerciseCategoriesViewModel,
    onCategorySelected: (Long) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }
    LaunchedEffect(uiState) {
        if (uiState is ExerciseCategoriesUiState.Unauthorized) {
            onUnauthorized()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Categorías de ejercicios",
            style = MaterialTheme.typography.headlineSmall,
        )
        when (val state = uiState) {
            ExerciseCategoriesUiState.Loading -> {
                GymLoading(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is ExerciseCategoriesUiState.Success -> {
                if (state.categories.isEmpty()) {
                    Text(text = "No hay categorías disponibles.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.categories, key = { it.id }) { category ->
                            ExerciseCategoryItem(
                                name = category.name,
                                active = category.active,
                                onClick = { onCategorySelected(category.id) },
                            )
                        }
                    }
                }
            }

            is ExerciseCategoriesUiState.Error -> {
                GymErrorMessage(message = state.message)
                GymButton(
                    text = "Reintentar",
                    onClick = viewModel::loadCategories,
                )
            }

            ExerciseCategoriesUiState.Unauthorized -> Unit
        }
    }
}

@Composable
private fun ExerciseCategoryItem(
    name: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    GymCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (active) "Activa" else "Inactiva",
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
