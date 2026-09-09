package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.imanol.gymmanagement.feature.auth.presentation.LoginScreen
import com.imanol.gymmanagement.feature.auth.presentation.SessionState
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.auth.presentation.SplashScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseCategoriesScreen
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseCategoriesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExercisesScreen
import com.imanol.gymmanagement.feature.exercise.presentation.ExercisesViewModel
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseDetailScreen
import com.imanol.gymmanagement.feature.exercise.presentation.ExerciseDetailViewModel
import com.imanol.gymmanagement.feature.client.presentation.ClientsScreen
import com.imanol.gymmanagement.feature.client.presentation.ClientsViewModel
import com.imanol.gymmanagement.feature.client.presentation.ClientDetailScreen
import com.imanol.gymmanagement.feature.client.presentation.ClientDetailViewModel
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplatesScreen
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplatesViewModel
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplateDetailScreen
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplateDetailViewModel
import com.imanol.gymmanagement.feature.workout.presentation.WorkoutTemplateFormScreen

@Composable
fun GymNavHost(
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
    val navController = rememberNavController()
    val sessionState by loginViewModel.sessionState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AuthGraph,
    ) {
        navigation<AuthGraph>(startDestination = Splash) {
            composable<Splash> {
                LaunchedEffect(sessionState) {
                    when (sessionState) {
                        SessionState.Authenticated -> navController.navigate(Home) {
                            popUpTo(AuthGraph) { inclusive = true }
                        }
                        SessionState.Unauthenticated -> navController.navigate(Login) {
                            popUpTo(Splash) { inclusive = true }
                        }
                        SessionState.Checking -> Unit
                    }
                }
                SplashScreen(
                    onContinue = {
                        if (sessionState == SessionState.Unauthenticated) {
                            navController.navigate(Login) {
                                popUpTo(Splash) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable<Login> {
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(Home) {
                            popUpTo(AuthGraph) { inclusive = true }
                        }
                    },
                )
            }
        }

        navigation<MainGraph>(startDestination = Home) {
            composable<Home> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCategories = { navController.navigate(ExerciseCategories) },
                    onNavigateToClients = { navController.navigate(Clients) },
                    onNavigateToWorkoutTemplates = {
                        navController.navigate(WorkoutTemplates)
                    },
                    onLogout = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(Home) { inclusive = true }
                        }
                    },
                )
            }
            composable<ExerciseCategories> {
                ExerciseCategoriesScreen(
                    viewModel = exerciseCategoriesViewModel,
                    onCategorySelected = { categoryId ->
                        navController.navigate(Exercises(categoryId))
                    },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<Exercises> { backStackEntry ->
                val route = backStackEntry.toRoute<Exercises>()
                ExercisesScreen(
                    categoryId = route.categoryId,
                    viewModel = exercisesViewModel,
                    onExerciseSelected = { exerciseId ->
                        navController.navigate(ExerciseDetail(exerciseId))
                    },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<ExerciseDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ExerciseDetail>()
                ExerciseDetailScreen(
                    exerciseId = route.exerciseId,
                    viewModel = exerciseDetailViewModel,
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<Clients> {
                ClientsScreen(
                    viewModel = clientsViewModel,
                    onClientSelected = { clientId ->
                        navController.navigate(ClientDetail(clientId))
                    },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<ClientDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ClientDetail>()
                ClientDetailScreen(
                    clientId = route.clientId,
                    viewModel = clientDetailViewModel,
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<WorkoutTemplates> {
                WorkoutTemplatesScreen(
                    viewModel = workoutTemplatesViewModel,
                    onTemplateSelected = { templateId ->
                        navController.navigate(WorkoutTemplateDetail(templateId))
                    },
                    onCreateTemplate = {
                        navController.navigate(WorkoutTemplateForm())
                    },
                    onEditTemplate = { templateId ->
                        navController.navigate(WorkoutTemplateForm(templateId))
                    },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<WorkoutTemplateDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<WorkoutTemplateDetail>()
                WorkoutTemplateDetailScreen(
                    templateId = route.templateId,
                    viewModel = workoutTemplateDetailViewModel,
                    onEditTemplate = { templateId ->
                        navController.navigate(WorkoutTemplateForm(templateId))
                    },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
            composable<WorkoutTemplateForm> { backStackEntry ->
                val route = backStackEntry.toRoute<WorkoutTemplateForm>()
                WorkoutTemplateFormScreen(
                    templateId = route.templateId,
                    viewModel = workoutTemplatesViewModel,
                    onSaved = { navController.popBackStack() },
                    onUnauthorized = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(MainGraph) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
