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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val exerciseCategoriesViewModel: ExerciseCategoriesViewModel by viewModels()
    private val exercisesViewModel: ExercisesViewModel by viewModels()
    private val exerciseDetailViewModel: ExerciseDetailViewModel by viewModels()

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
            )
        }
    }
}