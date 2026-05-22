package com.flue.launcher.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private const val SWIPE_DISMISS_THRESHOLD_RATIO = 0.32f
private const val SWIPE_DISMISS_VELOCITY = 1400f

@Composable
internal fun SwipeRevealDeleteContainer(
    target: NotificationRevealTarget,
    revealedTarget: NotificationRevealTarget?,
    onRevealTargetChange: (NotificationRevealTarget?) -> Unit,
    enabled: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    actionHeight: androidx.compose.ui.unit.Dp? = null,
    content: @Composable () -> Unit
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val offsetX = remember(target) { Animatable(0f) }
    val scale = remember(target) { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var dismissed by remember(target) { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !dismissed,
        exit = shrinkVertically(
            animationSpec = tween(220),
            shrinkTowards = androidx.compose.ui.Alignment.Top
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = (0.82f + scale.value * 0.18f).coerceIn(0f, 1f)
                }
                .pointerInput(target, enabled, touchSlop, dismissed) {
                    if (!enabled || dismissed) return@pointerInput
                    awaitEachGesture {
                        val down = awaitPrimaryDown()
                        val pointerId = down.id
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        var totalX = 0f
                        var totalY = 0f
                        var horizontalLock = false

                        onRevealTargetChange(null)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            tracker.addPosition(change.uptimeMillis, change.position)
                            if (!change.pressed || change.changedToUpIgnoreConsumed()) break

                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y

                            if (!horizontalLock) {
                                val pastSlop = abs(totalX) > touchSlop || abs(totalY) > touchSlop
                                if (pastSlop) {
                                    val horizontalDominant = abs(totalX) > abs(totalY) * 1.15f
                                    if (!horizontalDominant) break
                                    horizontalLock = true
                                }
                            }

                            if (horizontalLock) {
                                change.consume()
                                val nextOffset = (offsetX.value + delta.x).coerceIn(-widthPx * 1.15f, widthPx * 1.15f)
                                val nextScale = (1f - (abs(nextOffset) / widthPx) * 0.08f).coerceIn(0.92f, 1f)
                                scope.launch {
                                    offsetX.snapTo(nextOffset)
                                    scale.snapTo(nextScale)
                                }
                            }
                        }

                        val velocityX = tracker.calculateVelocity().x
                        val shouldDismiss = horizontalLock && (
                            abs(offsetX.value) > widthPx * SWIPE_DISMISS_THRESHOLD_RATIO ||
                                abs(velocityX) > SWIPE_DISMISS_VELOCITY
                            )

                        if (shouldDismiss) {
                            val direction = if (offsetX.value == 0f) {
                                sign(velocityX).takeIf { it != 0f } ?: 1f
                            } else {
                                sign(offsetX.value)
                            }
                            scope.launch {
                                launch { scale.animateTo(0.9f, tween(180)) }
                                offsetX.animateTo(direction * widthPx * 1.35f, tween(240))
                                dismissed = true
                                delay(140)
                                onDelete()
                            }
                        } else {
                            scope.launch {
                                launch {
                                    scale.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.74f, stiffness = 580f)
                                    )
                                }
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.74f, stiffness = 580f)
                                )
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPrimaryDown():
    androidx.compose.ui.input.pointer.PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change != null) return change
    }
}
