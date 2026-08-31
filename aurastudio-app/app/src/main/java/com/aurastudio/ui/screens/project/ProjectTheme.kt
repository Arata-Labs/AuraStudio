package com.aurastudio.ui.screens.project

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** CodeAssist `Ca` radius token port. */
internal object ProjectRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 9.dp
    val control: Dp = 12.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 24.dp
    val sheet: Dp = 26.dp
    val pill: Dp = 999.dp
}

/** CodeAssist `Motion` token port (durations in ms + easings). */
internal object ProjectMotion {
    const val FAST = 160
    const val BASE = 240
    const val SLOW = 380

    /** Calm settle (drawer/dock detent animations). */
    val quiet = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Guarded ease for short fade/expand clips. */
    val soft = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)

    /** Expressive spring feel (press feedback, chip growth). */
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

private val RUN_GREEN_DARK = Color(0xFF34D058)
private val RUN_GREEN_LIGHT = Color(0xFF29A847)
private val WARN_AMBER_DARK = Color(0xFFFFB340)
private val WARN_AMBER_LIGHT = Color(0xFFD98300)
private val EDITOR_BG_DARK = Color(0xFF1B1C1F)
private val EDITOR_BG_LIGHT = Color(0xFFFAF9F6)
private val GLASS_THICK_DARK = Color(0xFF18191C)
private val GLASS_THICK_LIGHT = Color(0xFFF8F7F4)
private val GIT_MODIFIED_DARK = Color(0xFFFFC44D)
private val GIT_MODIFIED_LIGHT = Color(0xFFC98A00)

/** CodeAssist `Ide.colors.run` / `success` green. */
@Composable
internal fun runGreen(): Color = if (isSystemInDarkTheme()) RUN_GREEN_DARK else RUN_GREEN_LIGHT

/** CodeAssist `Ide.colors.warning` amber. */
@Composable
internal fun warnAmber(): Color = if (isSystemInDarkTheme()) WARN_AMBER_DARK else WARN_AMBER_LIGHT

/** CodeAssist `Ide.colors.editorBg` / `consoleBg`. */
@Composable
internal fun editorBg(): Color = if (isSystemInDarkTheme()) EDITOR_BG_DARK else EDITOR_BG_LIGHT

/** CodeAssist `Ide.colors.gitModified` — the dirty-dot pill. */
@Composable
internal fun gitModified(): Color = if (isSystemInDarkTheme()) GIT_MODIFIED_DARK else GIT_MODIFIED_LIGHT

/** CodeAssist `Ide.colors.glassThick` (dock / sheet fill). */
@Composable
internal fun glassThick(): Color = if (isSystemInDarkTheme()) {
    GLASS_THICK_DARK.copy(alpha = 0.86f)
} else {
    GLASS_THICK_LIGHT.copy(alpha = 0.88f)
}

/** CodeAssist `Ide.colors.glassEdgeTop` — the 1px top hairline of glass surfaces. */
@Composable
internal fun glassEdgeTop(): Color = Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.9f)