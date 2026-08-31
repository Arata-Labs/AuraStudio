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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Fling speed (px/s, expressed in dp) past which a release commits to open/close regardless of position. */
private val FlingCommit = 320.dp

/**
 * CodeAssist `PushDrawer` — a **push** navigation drawer: [content] slides right to make room for
 * [drawerContent] on the left (no scrim over the content — the screen itself moves).
 *
 * Gestures are nested-scroll aware so they never fight the code editor:
 *  - a rightward drag opens the drawer only when the content underneath consumed **no horizontal scroll
 *    for the whole gesture** (the editor was already at its horizontal start) and the gesture is
 *    horizontal-dominant; if the child scrolls sideways the child owns the axis for the rest of the stroke;
 *  - with the drawer open, the pushed content is covered by a tap-to-close catcher and a horizontal drag
 *    anywhere moves the drawer; release settles to the nearer edge, flings commit.
 *
 * [open] is the hoisted state of record; toggling it from chrome (top-bar button, back press) animates the
 * same offset the gestures drive. [onProgress] observes the live open fraction for mirroring chrome.
 */
@Composable
internal fun PushDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    maxDrawerWidth: Dp = 320.dp,
    onProgress: (Float) -> Unit = {},
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val drawerWidth = if (maxWidth - 56.dp < maxDrawerWidth) maxWidth - 56.dp else maxDrawerWidth
        val maxPx = with(density) { drawerWidth.toPx() }
        val flingPx = with(density) { FlingCommit.toPx() }
        val touchSlop = LocalViewConfiguration.current.touchSlop
        val scope = rememberCoroutineScope()
        val offset = remember { Animatable(0f) }
        offset.updateBounds(0f, maxPx)
        val drawerVisible by remember { derivedStateOf { offset.value > 0.5f } }

        fun dragBy(delta: Float) {
            val target = (offset.value + delta).coerceIn(0f, maxPx)
            scope.launch { offset.snapTo(target) }
        }

        suspend fun settle(velocityX: Float) {
            val target = when {
                velocityX > flingPx -> maxPx
                velocityX < -flingPx -> 0f
                offset.value >= maxPx / 2f -> maxPx
                else -> 0f
            }
            offset.animateTo(target, tween(ProjectMotion.BASE, easing = ProjectMotion.quiet), initialVelocity = velocityX)
            onOpenChange(target > 0f)
        }

        LaunchedEffect(open, maxPx) {
            val target = if (open) maxPx else 0f
            if (offset.value != target) offset.animateTo(target, tween(ProjectMotion.SLOW, easing = ProjectMotion.quiet))
        }

        val progress by rememberUpdatedState(onProgress)
        LaunchedEffect(maxPx) {
            snapshotFlow { if (maxPx > 0f) (offset.value / maxPx).coerceIn(0f, 1f) else 0f }
                .collect { progress(it) }
        }

        val connection = remember(maxPx, gesturesEnabled) {
            object : NestedScrollConnection {
                private var sumX = 0f
                private var sumY = 0f
                private var leakX = 0f
                private var captured = false
                private var childOwnsX = false
                private fun reset() { sumX = 0f; sumY = 0f; leakX = 0f; captured = false; childOwnsX = false }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!gesturesEnabled || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (!captured) return Offset.Zero
                    dragBy(available.x)
                    return Offset(available.x, 0f)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (!gesturesEnabled || source != NestedScrollSource.UserInput) return Offset.Zero
                    sumX += consumed.x + available.x
                    sumY += consumed.y + available.y
                    if (consumed.x != 0f) childOwnsX = true
                    if (!captured && !childOwnsX && offset.value == 0f) {
                        leakX = (leakX + available.x).coerceAtLeast(0f)
                        if (leakX > touchSlop && abs(sumX) > abs(sumY)) {
                            captured = true
                            dragBy(available.x)
                            return Offset(available.x, 0f)
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (captured) {
                        reset()
                        scope.launch { settle(available.x) }
                        return Velocity(available.x, 0f)
                    }
                    reset()
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    reset()
                    return Velocity.Zero
                }
            }
        }

        val closeDrag = rememberDraggableState { delta -> dragBy(delta) }
        val closeGesture = if (gesturesEnabled && drawerVisible) {
            Modifier.draggable(
                closeDrag,
                Orientation.Horizontal,
                onDragStopped = { velocity -> scope.launch { settle(velocity) } },
            )
        } else {
            Modifier
        }
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(connection)
                .then(closeGesture),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offset.value.roundToInt(), 0) },
            ) {
                if (drawerVisible || open) {
                    Box(
                        Modifier
                            .width(drawerWidth)
                            .fillMaxHeight()
                            .offset(x = -drawerWidth)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        drawerContent()
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    content()
                    if (drawerVisible) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(14.dp)
                                .graphicsLayer { alpha = (offset.value / maxPx).coerceIn(0f, 1f) }
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                                    ),
                                ),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) { detectTapGestures { onOpenChange(false) } },
                        )
                    }
                }
            }
        }
    }
}