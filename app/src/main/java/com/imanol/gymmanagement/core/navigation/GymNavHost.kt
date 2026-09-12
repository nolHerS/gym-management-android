package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imanol.gymmanagement.feature.auth.presentation.LoginScreen
import com.imanol.gymmanagement.feature.auth.presentation.SessionState
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.auth.presentation.SplashScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeViewModel
import com.imanol.gymmanagement.feature.home.presentation.HomeUiState
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
import com.imanol.gymmanagement.feature.workoutplan.presentation.*
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionSnapshot
import com.imanol.gymmanagement.feature.workoutexecution.domain.WorkoutExecutionState
import com.imanol.gymmanagement.feature.workoutexecution.presentation.*
import com.imanol.gymmanagement.feature.workoutsession.domain.AddWorkoutSessionSetUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.CancelWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.FinishWorkoutSessionUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.UpdateWorkoutSessionExerciseUseCase
import com.imanol.gymmanagement.feature.workoutsession.domain.WorkoutSession
import com.imanol.gymmanagement.feature.nutrition.presentation.*

private fun isAuthenticationRoute(route: String?): Boolean =
    route != null && setOf(
        AuthGraph::class.qualifiedName,
        Splash::class.qualifiedName,
        Login::class.qualifiedName,
    ).filterNotNull().any { route.startsWith(it) }

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
    val navController = rememberNavController()
    val sessionState by loginViewModel.sessionState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var pendingWorkoutSnapshot by remember { mutableStateOf<WorkoutExecutionSnapshot?>(null) }
    var pendingWorkoutSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var finishedWorkoutState by remember { mutableStateOf<WorkoutExecutionState?>(null) }

    LaunchedEffect(sessionState, backStackEntry) {
        if (
            sessionState == SessionState.Unauthenticated &&
            backStackEntry != null &&
            !isAuthenticationRoute(backStackEntry?.destination?.route)
        ) {
            navController.navigate(Login) {
                popUpTo(MainGraph) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(homeState, backStackEntry) {
        val role = (homeState as? HomeUiState.Success)?.user?.role
        val destinationRoute = backStackEntry?.destination?.route
        if (destinationRoute != null && role != null && !RoleGuard.canAccess(role, destinationRoute)) {
            navController.navigate(Home) {
                popUpTo(MainGraph)
                launchSingleTop = true
            }
        }
    }

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
                    onNavigateToMyWorkoutPlan = {
                        navController.navigate(MyWorkoutPlan)
                    },
                    onNavigateToFoods = { navController.navigate(Foods) },
                    onNavigateToMyNutrition = { navController.navigate(MyNutrition) },
                    onLogout = {
                        loginViewModel.logout {
                            navController.navigate(Login) {
                                popUpTo(Home) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable<MyWorkoutPlan> {
                MyWorkoutPlanScreen(
                    viewModel = myWorkoutPlanViewModel,
                    onUnauthorized = {},
                    onNavigateToWorkoutToday = { navController.navigate(WorkoutToday) },
                )
            }
            composable<WorkoutToday> {
                WorkoutTodayScreen(
                    viewModel = workoutTodayViewModel,
                    onUnauthorized = {},
                    onStart = { snapshot, session ->
                        pendingWorkoutSnapshot = snapshot
                        pendingWorkoutSession = session
                        navController.navigate(WorkoutExecution)
                    },
                )
            }
            composable<WorkoutExecution> {
                val snapshot = pendingWorkoutSnapshot
                val session = pendingWorkoutSession
                val executionViewModel = snapshot?.let { currentSnapshot ->
                    session?.let { currentSession ->
                        viewModel<WorkoutExecutionViewModel>(
                            key = "workout-execution-${currentSession.id}",
                            factory = WorkoutExecutionViewModelFactory(
                                currentSnapshot,
                                currentSession,
                                addWorkoutSessionSet,
                                updateWorkoutSessionExercise,
                                finishWorkoutSession,
                                cancelWorkoutSession,
                            ),
                        )
                    }
                }
                WorkoutExecutionScreen(
                    viewModel = executionViewModel,
                    onFinished = { state ->
                        finishedWorkoutState = state
                        navController.navigate(WorkoutFinished) {
                            popUpTo(WorkoutExecution) { inclusive = true }
                        }
                    },
                    onCancelled = {
                        navController.popBackStack(WorkoutToday, false)
                    },
                    onBack = {
                        navController.popBackStack(WorkoutToday, false)
                    },
                )
            }
            composable<WorkoutFinished> {
                val state = finishedWorkoutState
                if (state == null) {
                    navController.popBackStack(WorkoutToday, false)
                } else {
                    WorkoutFinishedScreen(
                        state = state,
                        onDone = { navController.popBackStack(WorkoutToday, false) },
                    )
                }
            }
            composable<ExerciseCategories> {
                ExerciseCategoriesScreen(
                    viewModel = exerciseCategoriesViewModel,
                    onCategorySelected = { categoryId ->
                        navController.navigate(Exercises(categoryId))
                    },
                    onUnauthorized = {},
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
                    onUnauthorized = {},
                )
            }
            composable<ExerciseDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ExerciseDetail>()
                ExerciseDetailScreen(
                    exerciseId = route.exerciseId,
                    viewModel = exerciseDetailViewModel,
                    onUnauthorized = {},
                )
            }
            composable<Clients> {
                ClientsScreen(
                    viewModel = clientsViewModel,
                    onClientSelected = { clientId ->
                        navController.navigate(ClientDetail(clientId))
                    },
                    onUnauthorized = {},
                )
            }
            composable<ClientDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<ClientDetail>()
                ClientDetailScreen(
                    clientId = route.clientId,
                    viewModel = clientDetailViewModel,
                    onWorkoutPlans = { navController.navigate(WorkoutPlans(it)) },
                    onNutritionPlans = { navController.navigate(NutritionPlans(it)) },
                    onUnauthorized = {},
                )
            }
            composable<WorkoutPlans> { entry ->
                val route = entry.toRoute<WorkoutPlans>()
                WorkoutPlansScreen(route.clientId, workoutPlanViewModel, { navController.navigate(WorkoutPlanDetail(it)) }, {}, { navController.navigate(WorkoutPlanForm(route.clientId)) })
            }
            composable<WorkoutPlanForm> { entry ->
                val route = entry.toRoute<WorkoutPlanForm>()
                WorkoutPlanFormScreen(route.clientId, route.planId, workoutPlanViewModel) { navController.navigate(WorkoutPlanDetail(it)) }
            }
            composable<WorkoutPlanDetail> { entry ->
                val route = entry.toRoute<WorkoutPlanDetail>()
                WorkoutPlanDetailScreen(
                    route.planId,
                    workoutPlanViewModel,
                    workoutPlanStructureViewModel,
                    canManage = (homeState as? HomeUiState.Success)?.user?.role == "TRAINER",
                    onEdit = { navController.navigate(WorkoutPlanForm((homeState as? HomeUiState.Success)?.user?.id ?: 0L, route.planId)) },
                    onUnauthorized = {},
                )
            }
            composable<NutritionPlans> { entry ->
                val route = entry.toRoute<NutritionPlans>()
                NutritionPlansScreen(
                    clientId = route.clientId,
                    viewModel = nutritionPlanViewModel,
                    canManage = (homeState as? HomeUiState.Success)?.user?.role == "TRAINER",
                    onPlanSelected = { navController.navigate(NutritionPlanDetail(it)) },
                    onCreate = { navController.navigate(NutritionPlanForm(route.clientId)) },
                    onUnauthorized = {},
                    onAccessDenied = {
                        navController.navigate(Home) {
                            popUpTo(MainGraph)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<NutritionPlanDetail> { entry ->
                val route = entry.toRoute<NutritionPlanDetail>()
                NutritionPlanDetailScreen(
                    planId = route.planId,
                    viewModel = nutritionPlanViewModel,
                    canManage = (homeState as? HomeUiState.Success)?.user?.role == "TRAINER",
                    onEdit = { clientId, planId ->
                        navController.navigate(NutritionPlanForm(clientId, planId))
                    },
                    onUnauthorized = {},
                    onAccessDenied = {
                        navController.navigate(Home) {
                            popUpTo(MainGraph)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<NutritionPlanForm> { entry ->
                val route = entry.toRoute<NutritionPlanForm>()
                NutritionPlanFormScreen(
                    clientId = route.clientId,
                    planId = route.planId,
                    viewModel = nutritionPlanViewModel,
                    canManage = (homeState as? HomeUiState.Success)?.user?.role == "TRAINER",
                    onSaved = { navController.navigate(NutritionPlanDetail(it)) },
                    onUnauthorized = {},
                    onAccessDenied = {
                        navController.navigate(Home) {
                            popUpTo(MainGraph)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<Foods> {
                val canManage =
                    (homeState as? HomeUiState.Success)?.user?.role == "TRAINER"
                FoodsScreen(
                    viewModel = foodViewModel,
                    canManage = canManage,
                    onFoodSelected = { navController.navigate(FoodDetail(it)) },
                    onCreate = { navController.navigate(FoodForm()) },
                    onEdit = { navController.navigate(FoodForm(it)) },
                    onUnauthorized = {},
                )
            }
            composable<FoodDetail> { entry ->
                val route = entry.toRoute<FoodDetail>()
                val canManage =
                    (homeState as? HomeUiState.Success)?.user?.role == "TRAINER"
                FoodDetailScreen(
                    foodId = route.foodId,
                    viewModel = foodViewModel,
                    canManage = canManage,
                    onEdit = { navController.navigate(FoodForm(it)) },
                    onUnauthorized = {},
                )
            }
            composable<FoodForm> { entry ->
                val route = entry.toRoute<FoodForm>()
                FoodFormScreen(
                    foodId = route.foodId,
                    viewModel = foodViewModel,
                    onSaved = { navController.navigate(FoodDetail(it)) },
                    onUnauthorized = {},
                )
            }
            composable<MyNutrition> {
                MyNutritionScreen(
                    viewModel = myNutritionViewModel,
                    onPlanSelected = { navController.navigate(MyNutritionPlan(it)) },
                    onUnauthorized = {},
                )
            }
            composable<MyNutritionPlan> { entry ->
                val route = entry.toRoute<MyNutritionPlan>()
                MyNutritionPlanScreen(
                    planId = route.planId,
                    viewModel = myNutritionViewModel,
                    onUnauthorized = {},
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
                    onUnauthorized = {},
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
                    onUnauthorized = {},
                )
            }
            composable<WorkoutTemplateForm> { backStackEntry ->
                val route = backStackEntry.toRoute<WorkoutTemplateForm>()
                WorkoutTemplateFormScreen(
                    templateId = route.templateId,
                    viewModel = workoutTemplatesViewModel,
                    onSaved = { navController.popBackStack() },
                    onUnauthorized = {},
                )
            }
        }
    }
}
