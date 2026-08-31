package com.aurastudio.ui.screens.project

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** CodeAssist `pressScale` — bouncy scale-to-0.96 press feedback via an interaction source. */
@Composable
internal fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = ProjectMotion.spring,
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * CodeAssist `IconButtonCa` — a square compact toolbar control (denser than M3's 48dp default so it fits
 * the top bar). An [active] control gets a `secondaryContainer` fill + `onSecondaryContainer` tint;
 * inactive is transparent with an `onSurfaceVariant` glyph.
 */
@Composable
internal fun IconButtonCa(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconSize: Int = 20,
    boxSize: Int = 34,
    tint: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val resolvedTint = tint ?: if (active) scheme.onSecondaryContainer else scheme.onSurfaceVariant
    Box(
        modifier
            .size(boxSize.dp)
            .pressScale(interaction)
            .background(
                if (active) scheme.secondaryContainer else Color.Transparent,
                MaterialTheme.shapes.small,
            )
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(iconSize.dp), tint = resolvedTint)
    }
}

/** CodeAssist `CaDropdownMenu` — rounded, hairline-bordered elevated panel drop-in for [DropdownMenu]. */
@Composable
internal fun CaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(ProjectRadius.xl),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/** CodeAssist `CaMenuItem` — a `DropdownMenuItem` whose leading icon sits tight against the text
 * (M3's default leadingIcon slot forces a wide 12dp+ fixed gap). */
@Composable
internal fun CaMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    contentDescription: String? = null,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
    textColor: Color = androidx.compose.material3.LocalContentColor.current,
    iconSpacing: Dp = 6.dp,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = contentDescription,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(iconSpacing))
                }
                Text(text, color = textColor)
            }
        },
        onClick = onClick,
    )
}

/** CodeAssist `ProjectTile` — rounded gradient tile with the project's bold initial. */
@Composable
internal fun ProjectTile(name: String, size: Dp = 32.dp, radius: Dp = ProjectRadius.sm) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    listOf(primary, lerp(primary, Color.Black, 0.45f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** CodeAssist `Chip` — a soft pill with a tonal fill (status / hints / meta). */
@Composable
internal fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = 22.dp)
            .background(fill, RoundedCornerShape(ProjectRadius.pill))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}