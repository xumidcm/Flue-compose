package com.flue.launcher.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun rememberPressedState(): MutableState<Boolean> = remember { mutableStateOf(false) }

fun Modifier.instantPressGesture(
    pressedState: MutableState<Boolean>,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(onClick, enabled) {
        detectTapGestures(
            onPress = {
                pressedState.value = true
                val released = tryAwaitRelease()
                pressedState.value = false
                if (released) onClick()
            }
        )
    }
}
