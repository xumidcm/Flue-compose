package com.flue.launcher.ui.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun GestureHost(
    screenState: ScreenState,
    onStateChange: (ScreenState) -> Unit,
    modifier: Modifier = Modifier,
    sideScreenEnabled: Boolean = false,
    showNotification: Boolean = true,
    showControlCenter: Boolean = false,
    content: @Composable () -> Unit
) {
    var totalDx by remember { mutableFloatStateOf(0f) }
    var totalDy by remember { mutableFloatStateOf(0f) }
    var consumed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(screenState) {
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                        consumed = false
                    },
                    onDrag = { change, dragAmount ->
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y

                        if (!consumed && (abs(totalDx) > 80 || abs(totalDy) > 80)) {
                            consumed = true
                            val isVertical = abs(totalDy) > abs(totalDx)
                            val isHorizontal = !isVertical

                            when (screenState) {
                                ScreenState.Face -> {
                                    if (isVertical && totalDy < -80) {
                                        onStateChange(ScreenState.Apps)
                                        change.consume()
                                    } else if (isHorizontal && totalDx > 80 && sideScreenEnabled) {
                                        onStateChange(ScreenState.Stack)
                                        change.consume()
                                    } else if (isHorizontal && totalDx < -80 && showControlCenter) {
                                        onStateChange(ScreenState.ControlCenter)
                                        change.consume()
                                    }
                                }

                                ScreenState.Notifications -> Unit

                                ScreenState.ControlCenter -> {
                                    if (isHorizontal && totalDx > 80) {
                                        onStateChange(ScreenState.Face)
                                        change.consume()
                                    }
                                }

                                ScreenState.Apps -> Unit
                                ScreenState.App -> Unit
                                ScreenState.Settings -> Unit
                                ScreenState.Stack -> {
                                    if (isHorizontal && totalDx < -80) {
                                        onStateChange(ScreenState.Face)
                                        change.consume()
                                    } else if (isVertical && totalDy < -80 && showNotification) {
                                        onStateChange(ScreenState.Notifications)
                                        change.consume()
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {}
                )
            }
    ) {
        content()
    }
}
