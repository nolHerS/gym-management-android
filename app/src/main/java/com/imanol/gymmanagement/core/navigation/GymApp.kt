package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.home.presentation.HomeViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseCategoriesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExercisesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseDetailViewModel
import com.imanol.gymmanagement.feature.client.presentation.ClientsViewModel
import com.imanol.gymmanagement.feature.client.presentation.ClientDetailViewModel
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplatesViewModel
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplateDetailViewModel

@Composable
fun GymApp(
    loginViewModel: LoginViewModel,
    homeViewModel: HomeViewModel,
    exerciseCategoriesViewModel: ExerciseCategoriesViewModel,
    exercisesViewModel: ExercisesViewModel,
    exerciseDetailViewModel: ExerciseDetailViewModel,
    clientsViewModel: ClientsViewModel,
    clientDetailViewModel: ClientDetailViewModel,
    workoutTemplatesViewModel: WorkoutTemplatesViewModel,
    workoutTemplateDetailViewModel: WorkoutTemplateDetailViewModel,
) {
    GymTheme {
        GymNavHost(
            loginViewModel,
            homeViewModel,
            exerciseCategoriesViewModel,
            exercisesViewModel,
            exerciseDetailViewModel,
            clientsViewModel,
            clientDetailViewModel,
            workoutTemplatesViewModel,
            workoutTemplateDetailViewModel,
        )
    }
}
