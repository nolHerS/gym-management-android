package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel
import com.imanol.gymmanagement.feature.home.presentation.HomeViewModel

@Composable
fun GymApp(loginViewModel: LoginViewModel, homeViewModel: HomeViewModel) {
    GymTheme {
        GymNavHost(loginViewModel, homeViewModel)
    }
}
