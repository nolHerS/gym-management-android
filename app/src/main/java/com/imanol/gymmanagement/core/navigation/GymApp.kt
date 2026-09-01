package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme
import com.imanol.gymmanagement.feature.auth.presentation.LoginViewModel

@Composable
fun GymApp(loginViewModel: LoginViewModel) {
    GymTheme {
        GymNavHost(loginViewModel)
    }
}
