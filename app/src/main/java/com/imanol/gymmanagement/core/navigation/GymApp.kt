package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.home.presentation.HomeViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseCategoriesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExercisesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseDetailViewModel

@Composable
fun GymApp(
    loginViewModel: LoginViewModel,
    homeViewModel: HomeViewModel,
    exerciseCategoriesViewModel: ExerciseCategoriesViewModel,
    exercisesViewModel: ExercisesViewModel,
    exerciseDetailViewModel: ExerciseDetailViewModel,
) {
    GymTheme {
        GymNavHost(
            loginViewModel,
            homeViewModel,
            exerciseCategoriesViewModel,
            exercisesViewModel,
            exerciseDetailViewModel,
        )
    }
}
