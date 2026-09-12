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
import com.imanol.gymmanagement.feature.workoutplan.presentation.WorkoutPlanViewModel
import com.imanol.gymmanagement.feature.workoutplan.presentation.WorkoutPlanStructureViewModel
import com.imanol.gymmanagement.feature.workoutplan.presentation.MyWorkoutPlanViewModel
import com.imanol.gymmanagement.feature.workoutexecution.presentation.WorkoutTodayViewModel
import com.imanol.gymmanagement.feature.nutrition.presentation.FoodViewModel
import com.imanol.gymmanagement.feature.nutrition.presentation.MyNutritionViewModel
import com.imanol.gymmanagement.feature.nutrition.presentation.NutritionPlanViewModel
import com.imanol.gymmanagement.feature.workoutsession.domain.AddWorkoutSessionSetUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.CancelWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.FinishWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.UpdateWorkoutSessionExerciseUseCase

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
    workoutPlanViewModel: WorkoutPlanViewModel,
    workoutPlanStructureViewModel: WorkoutPlanStructureViewModel,
    myWorkoutPlanViewModel: MyWorkoutPlanViewModel,
    workoutTodayViewModel: WorkoutTodayViewModel,
    nutritionPlanViewModel: NutritionPlanViewModel,
    foodViewModel: FoodViewModel,
    myNutritionViewModel: MyNutritionViewModel,
    addWorkoutSessionSet: AddWorkoutSessionSetUseCase,
    updateWorkoutSessionExercise: UpdateWorkoutSessionExerciseUseCase,
    finishWorkoutSession: FinishWorkoutSessionUseCase,
    cancelWorkoutSession: CancelWorkoutSessionUseCase,
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
            workoutPlanViewModel,
            workoutPlanStructureViewModel,
            myWorkoutPlanViewModel,
            workoutTodayViewModel,
            nutritionPlanViewModel,
            foodViewModel,
            myNutritionViewModel,
            addWorkoutSessionSet,
            updateWorkoutSessionExercise,
            finishWorkoutSession,
            cancelWorkoutSession,
        )
    }
}
