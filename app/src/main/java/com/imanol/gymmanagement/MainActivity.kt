package com.imanol.gymmanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.imanol.gymmanagement.core.navigation.GymApp
import androidx.activity.viewModels
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
import com.imanol.gymmanagement.feature.nutrition.presentation.FoodViewModel
import com.imanol.gymmanagement.feature.nutrition.presentation.MyNutritionViewModel
import com.imanol.gymmanagement.feature.nutrition.presentation.NutritionPlanViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val exerciseCategoriesViewModel: ExerciseCategoriesViewModel by viewModels()
    private val exercisesViewModel: ExercisesViewModel by viewModels()
    private val exerciseDetailViewModel: ExerciseDetailViewModel by viewModels()
    private val clientsViewModel: ClientsViewModel by viewModels()
    private val clientDetailViewModel: ClientDetailViewModel by viewModels()
    private val workoutTemplatesViewModel: WorkoutTemplatesViewModel by viewModels()
    private val workoutTemplateDetailViewModel: WorkoutTemplateDetailViewModel by viewModels()
    private val workoutPlanViewModel: WorkoutPlanViewModel by viewModels()
    private val workoutPlanStructureViewModel: WorkoutPlanStructureViewModel by viewModels()
    private val myWorkoutPlanViewModel: MyWorkoutPlanViewModel by viewModels()
    private val nutritionPlanViewModel: NutritionPlanViewModel by viewModels()
    private val foodViewModel: FoodViewModel by viewModels()
    private val myNutritionViewModel: MyNutritionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymApp(
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
                nutritionPlanViewModel,
                foodViewModel,
                myNutritionViewModel,
            )
        }
    }
}