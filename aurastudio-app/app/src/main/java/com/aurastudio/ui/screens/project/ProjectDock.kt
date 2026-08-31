package com.aurastudio.ui.screens.project

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/** CodeAssist `DockBarHeight` — the dock's collapsed (bottom-nav) height. */
internal val DockBarHeight = 60.dp

/** Fraction of the available height the half (console) detent rests at. */
private const val HalfDetentFraction = 0.6f

/** Fling speed (px/s, in dp) past which a release commits toward the flung direction's next detent. */
private val DockFlingCommit = 320.dp

/** The collapsed bar's build-state glance: Idle / Running / Succeeded / Failed. */
internal enum class DockStatus { Idle, Running, Succeeded, Failed }

/**
 * CodeAssist `BuildDock` — the project screen's bottom dock: the bottom navigation bar is the collapsed
 * state of a draggable panel whose expanded state is the build console / terminal / search panel.
 * Swiping the bar up (or tapping the status chip / the top-bar console toggle) expands it; the nav items
 * fade and sink away as the console content fades in under a sheet-style grab handle.
 *
 * Detents: bar → half (console over ~60% of the screen) → full. Drags and flings settle to the nearest;
 * from the console's own scrollables the drag is nested-scroll aware — scrolling up grows the dock to full
 * before the list scrolls, and dragging down past the log's top collapses it.
 *
 * While a build runs, the collapsed bar carries a thin accent progress line on its top edge and a compact
 * status chip (spinner → ✓/✗) at its right end. [hidden] removes the dock entirely (the soft keyboard's
 * symbol bar owns the bottom slot while typing).
 */
@Composable
internal fun ProjectDock(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    isRunning: Boolean,
    exitCode: Int?,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    bar: @Composable () -> Unit,
    console: @Composable ColumnScope.() -> Unit,
) {
    if (hidden) return
    val status = when {
        isRunning -> DockStatus.Running
        exitCode != null && exitCode == 0 -> DockStatus.Succeeded
        exitCode != null -> DockStatus.Failed
        else -> DockStatus.Idle
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val barPx = with(density) { DockBarHeight.toPx() }
        // Clamp to at least the bar height: a degenerate measure pass can hand us a maxHeight below the
        // bar, and `Animatable.updateBounds(barPx, fullPx)` then throws (lowerBound > upperBound).
        val fullPx = maxOf(barPx, constraints.maxHeight.toFloat())
        val halfPx = fullPx * HalfDetentFraction
        val flingPx = with(density) { DockFlingCommit.toPx() }
        val scope = rememberCoroutineScope()
        val height = remember { Animatable(barPx) }
        height.updateBounds(barPx, fullPx)

        fun dragBy(deltaY: Float) {
            val target = (height.value - deltaY).coerceIn(barPx, fullPx)
            scope.launch { height.snapTo(target) }
        }

        suspend fun settle(velocityY: Float) {
            val target = when {
                velocityY < -flingPx -> if (height.value < halfPx) halfPx else fullPx
                velocityY > flingPx -> if (height.value > halfPx) halfPx else barPx
                else -> floatArrayOf(barPx, halfPx, fullPx).minBy { abs(it - height.value) }
            }
            try {
                height.animateTo(target, tween(ProjectMotion.BASE, easing = ProjectMotion.quiet), initialVelocity = -velocityY)
            } finally {
                onOpenChange(target > barPx + 1f)
            }
        }

        LaunchedEffect(open, halfPx) {
            val target = when {
                !open -> barPx
                height.value > halfPx + 1f -> return@LaunchedEffect
                else -> halfPx
            }
            if (height.value != target) height.animateTo(target, tween(ProjectMotion.BASE, easing = ProjectMotion.quiet))
        }

        val settleState = rememberUpdatedState<suspend (Float) -> Unit> { v -> settle(v) }
        val nested = remember(barPx, halfPx, fullPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val dy = available.y
                    if (dy < 0 && height.value < fullPx) {
                        val next = (height.value - dy).coerceAtMost(fullPx)
                        val grown = next - height.value
                        dragBy(-grown)
                        return Offset(0f, -grown)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val dy = available.y
                    if (dy > 0 && height.value > barPx) {
                        val next = (height.value - dy).coerceAtLeast(barPx)
                        val shrunk = height.value - next
                        dragBy(shrunk)
                        return Offset(0f, shrunk)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val h = height.value
                    val atDetent = abs(h - barPx) < 0.5f || abs(h - halfPx) < 0.5f || abs(h - fullPx) < 0.5f
                    if (!atDetent) {
                        settleState.value(available.y)
                        return Velocity(0f, available.y)
                    }
                    return Velocity.Zero
                }
            }
        }

        // Bar → half progress: drives the crossfade (nav out, console + handle in). The sheet's top
        // corners are always rounded per the Material-You bottom-sheet spec.
        val p = ((height.value - barPx) / (halfPx - barPx)).coerceIn(0f, 1f)
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        // The glass box only exists while the sheet is lifted — collapsed shows just the floating pill,
        // so no translucent "container" lingers behind it.
        val glassModifier = if (height.value <= barPx + 0.5f) {
            Modifier
        } else {
            Modifier.background(glassThick(), shape)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { height.value.toDp() })
                .then(glassModifier)
                .pointerInput(Unit) { detectTapGestures { } }
                .draggable(
                    rememberDraggableState { dy -> dragBy(dy) },
                    Orientation.Vertical,
                    onDragStopped = { velocity -> settle(velocity) },
                ),
        ) {
            if (p < 1f) {
                // Collapsed face extends over the system-nav inset (bottom-bar style) so no strip of the
                // Scaffold's surface peeks out behind the pill; the band takes the editor's color so it
                // reads as the dock area, not a separate container.
                val navInset = WindowInsets.navigationBars.getBottom(density)
                val collapsedHeight = DockBarHeight + with(density) { navInset.toDp() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(collapsedHeight),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(DockBarHeight)
                            .graphicsLayer {
                                alpha = 1f - (p * 2.5f).coerceAtMost(1f)
                                translationY = p * 18.dp.toPx()
                            },
                    ) {
                        bar()
                        if (status == DockStatus.Running) {
                            LinearProgressIndicator(
                                Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent,
                            )
                        }
                    }
                }
            }
            if (p > 0f) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(nested)
                        .graphicsLayer { alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f) },
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(5.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(ProjectRadius.pill),
                                ),
                        )
                    }
                    console()
                }
            }
        }
    }
}

/**
 * `BuildStatusChip` was removed: the collapsed pill (see `ConsoleDockBar`) now carries the build-status
 * glance itself, so the redundant corner chip no longer renders.
 */