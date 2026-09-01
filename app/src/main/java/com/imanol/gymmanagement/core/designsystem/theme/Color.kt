package com.imanol.gymmanagement.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val GymBlue = Color(0xFF1565C0)
private val GymBlueLight = Color(0xFF90CAF9)
private val GymBlueDark = Color(0xFF0D47A1)
private val GymBlueGrey = Color(0xFF455A64)
private val GymBlueGreyLight = Color(0xFFB0BEC5)
private val GymGreen = Color(0xFF2E7D32)
private val GymGreenLight = Color(0xFFA5D6A7)

val GymLightColorScheme = lightColorScheme(
    primary = GymBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = GymBlueGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4EB),
    onSecondaryContainer = Color(0xFF0C1D22),
    tertiary = GymGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB9F6B7),
    onTertiaryContainer = Color(0xFF002106),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF191C20)
)

val GymDarkColorScheme = darkColorScheme(
    primary = GymBlueLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = GymBlueDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = GymBlueGreyLight,
    onSecondary = Color(0xFF213338),
    secondaryContainer = Color(0xFF374A50),
    onSecondaryContainer = Color(0xFFD0E4EB),
    tertiary = GymGreenLight,
    onTertiary = Color(0xFF00390B),
    tertiaryContainer = Color(0xFF005313),
    onTertiaryContainer = Color(0xFFB9F6B7),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE1E2E6)
)
