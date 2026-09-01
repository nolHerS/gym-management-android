package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.imanol.gymmanagement.feature.auth.presentation.LoginScreen
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.auth.presentation.SplashScreen
import com.imanol.gymmanagement.feature.home.presentation.HomeScreen

@Composable
fun GymNavHost(loginViewModel: LoginViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthGraph,
    ) {
        navigation<AuthGraph>(startDestination = Splash) {
            composable<Splash> {
                SplashScreen(
                    onContinue = { navController.navigate(Login) },
                )
            }
            composable<Login> {
                LoginScreen(viewModel = loginViewModel)
            }
        }

        navigation<MainGraph>(startDestination = Home) {
            composable<Home> {
                HomeScreen()
            }
        }
    }
}
