package com.imanol.gymmanagement.core.navigation

import androidx.compose.runtime.Composable
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymApp() {
    GymTheme {
        GymNavHost()
    }
}
