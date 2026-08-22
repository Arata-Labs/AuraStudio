package com.hinohara.aurastudio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SeedColor = Color(0xFF6366F1)

val LocalIsAppDark = staticCompositionLocalOf { true }

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = IndigoDark,
    secondary = Cyan40,
    onSecondary = Color.White,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Purple40,
    onTertiary = Color.White,
    tertiaryContainer = Purple90,
    onTertiaryContainer = Color(0xFF380052),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    surfaceContainer = Color(0xFFF0EDF5),
    surfaceContainerLow = Color(0xFFFAF8FF),
    surfaceContainerHigh = Color(0xFFE5E2EC),
)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = IndigoDark,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo90,
    secondary = Cyan80,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Cyan40,
    onSecondaryContainer = Cyan90,
    tertiary = Purple80,
    onTertiary = Color(0xFF380052),
    tertiaryContainer = Purple40,
    onTertiaryContainer = Purple90,
    background = Color(0xFF111118),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1A1A24),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF252536),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    surfaceContainer = Color(0xFF1E1E2A),
    surfaceContainerLow = Color(0xFF181820),
    surfaceContainerHigh = Color(0xFF262638),
)

const val THEME_DARK = 0
const val THEME_LIGHT = 1
const val THEME_SYSTEM = 2

@Composable
fun AuraStudioTheme(
    themeMode: Int = THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        THEME_DARK -> true
        THEME_LIGHT -> false
        else -> isSystemDark
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsAppDark provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
