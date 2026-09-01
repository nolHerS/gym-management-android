package com.imanol.gymmanagement.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun GymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GymDarkColorScheme else GymLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GymTypography,
        shapes = GymShapes,
        content = content
    )
}
