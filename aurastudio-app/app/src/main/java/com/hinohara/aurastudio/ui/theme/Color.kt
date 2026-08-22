package com.hinohara.aurastudio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// AuraStudio Brand Colors (fallback when dynamic color unavailable)
val Indigo40 = Color(0xFF6366F1)
val Indigo80 = Color(0xFFC7D2FE)
val Indigo90 = Color(0xFFE0E7FF)
val IndigoDark = Color(0xFF3730A3)

val Cyan40 = Color(0xFF06B6D4)
val Cyan80 = Color(0xFF67E8F9)
val Cyan90 = Color(0xFFCFFAFE)

val Purple40 = Color(0xFF9333EA)
val Purple80 = Color(0xFFD8B4FE)
val Purple90 = Color(0xFFF3E8FF)

// Semantic Colors
val Green40 = Color(0xFF16A34A)
val Green80 = Color(0xFF86EFAC)
val Amber40 = Color(0xFFF59E0B)
val Amber80 = Color(0xFFFDE68A)
val Red40 = Color(0xFFDC2626)
val Red80 = Color(0xFFFCA5A5)

// Gradient Colors
val GradientStart = Color(0xFF6366F1)
val GradientMid = Color(0xFF8B5CF6)
val GradientEnd = Color(0xFFA855F7)
val GradientHealthyStart = Color(0xFF16A34A)
val GradientHealthyEnd = Color(0xFF22D3EE)
val GradientWarningStart = Color(0xFFF59E0B)
val GradientWarningEnd = Color(0xFFEF4444)
val GradientCriticalStart = Color(0xFFEF4444)
val GradientCriticalEnd = Color(0xFFDC2626)

// Card Background Colors — Dark
val CardDark = Color(0xFF1E1E2E)
val CardDarkLight = Color(0xFFF5F5FA)
val CardSurface = Color(0xFF252536)
val CardSurfaceLight = Color(0xFFE8E8F0)
val CardSurfaceLightMode = Color(0xFFEEEDF5)

// Terminal Colors — Dark
val TerminalBg = Color(0xFF0D1117)
val TerminalBgLight = Color(0xFFF6F8FA)
val TerminalFg = Color(0xFFC9D1D9)
val TerminalFgLight = Color(0xFF24292F)
val TerminalGreen = Color(0xFF7EE787)
val TerminalGreenLight = Color(0xFF1A7F37)
val TerminalRed = Color(0xFFFF7B72)
val TerminalRedLight = Color(0xFFCF222E)
val TerminalYellow = Color(0xFFD29922)
val TerminalYellowLight = Color(0xFF9A6700)
val TerminalBlue = Color(0xFF79C0FF)
val TerminalBlueLight = Color(0xFF0969DA)
val TerminalMagenta = Color(0xFFD2A8FF)
val TerminalMagentaLight = Color(0xFF8250DF)
val TerminalCyan = Color(0xFF56D4DD)
val TerminalCyanLight = Color(0xFF1B7C83)

// Dark Theme Fallback
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkSurface = Color(0xFF1A1A24)
val DarkSurfaceElevated = Color(0xFF222236)

// Light Theme Fallback
val LightOnSurfaceVariant = Color(0xFF49454F)
val LightSurface = Color(0xFFFFFBFF)
val LightSurfaceElevated = Color(0xFFF3F0F7)

// ─── Theme-aware color helpers ────────────────────────────────────
@Composable
@ReadOnlyComposable
fun cardBg(): Color = if (LocalIsAppDark.current) CardDark else CardDarkLight

@Composable
@ReadOnlyComposable
fun cardContentBg(): Color = if (LocalIsAppDark.current) CardSurface else CardSurfaceLightMode

@Composable
@ReadOnlyComposable
fun termBg(): Color = if (LocalIsAppDark.current) TerminalBg else TerminalBgLight

@Composable
@ReadOnlyComposable
fun termFg(): Color = if (LocalIsAppDark.current) TerminalFg else TerminalFgLight

@Composable
@ReadOnlyComposable
fun termGreen(): Color = if (LocalIsAppDark.current) TerminalGreen else TerminalGreenLight

@Composable
@ReadOnlyComposable
fun termSurface(): Color = if (LocalIsAppDark.current) DarkSurface else LightSurface
