package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.imanol.gymmanagement.feature.auth.presentation.LoginScreen
import com.imanol.gymmanagement.feature.auth.presentation.SessionState
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.auth.presentation.SplashScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeScreen

@Composable
fun GymNavHost(loginViewModel: LoginViewModel) {
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
                    onLogout = {
                        loginViewModel.logout()
                        navController.navigate(Login) {
                            popUpTo(Home) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
