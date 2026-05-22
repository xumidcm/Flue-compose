package com.flue.launcher.ui.input

import androidx.compose.foundation.focusable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlin.math.abs
import kotlin.math.sign

enum class DrawerInputMode {
    Honeycomb,
    List
}

enum class DrawerInputSource {
    Rotary,
    MouseWheel
}

suspend fun FocusRequester.requestFocusAfterFirstFrame() {
    withFrameNanos { }
    runCatching { requestFocus() }
}

fun tunedRotaryScrollDelta(verticalScrollPixels: Float, multiplier: Float = 1f): Float {
    val magnitude = abs(verticalScrollPixels)
    if (magnitude < 0.01f) return 0f

    // Wear OS crown/bezel hardware differs a lot. Watch4-style bezel pulses are often
    // extremely small, so we give tiny deltas a minimum usable step instead of treating
    // them as noise.
    val tunedMagnitude = when {
        magnitude < 0.12f -> 18f
        magnitude < 0.28f -> 24f
        magnitude < 0.75f -> magnitude * 42f
        magnitude < 2.5f -> magnitude * 18f
        magnitude < 8f -> magnitude * 6f
        magnitude < 24f -> magnitude * 2.4f
        else -> magnitude * 1.15f
    }
    return sign(verticalScrollPixels) * tunedMagnitude * multiplier
}

fun normalizeDrawerScrollDelta(
    verticalScrollPixels: Float,
    source: DrawerInputSource,
    mode: DrawerInputMode
): Float {
    val signedInput = when (source) {
        DrawerInputSource.Rotary -> verticalScrollPixels
        DrawerInputSource.MouseWheel -> verticalScrollPixels
    }
    val magnitude = abs(signedInput)
    if (magnitude < 0.01f) return 0f

    val tunedMagnitude = when (source) {
        DrawerInputSource.Rotary -> when {
            magnitude < 0.10f -> 22f
            magnitude < 0.24f -> 30f
            magnitude < 0.75f -> magnitude * 50f
            magnitude < 2.5f -> magnitude * 22f
            magnitude < 8f -> magnitude * 7f
            magnitude < 24f -> magnitude * 2.8f
            else -> magnitude * 1.25f
        }
        DrawerInputSource.MouseWheel -> when {
            magnitude < 0.24f -> 44f
            magnitude < 1.2f -> magnitude * 72f
            magnitude < 4f -> magnitude * 52f
            else -> magnitude * 40f
        }
    }

    val modeMultiplier = when (mode) {
        DrawerInputMode.Honeycomb -> when (source) {
            DrawerInputSource.Rotary -> 0.80f
            DrawerInputSource.MouseWheel -> 0.86f
        }
        DrawerInputMode.List -> when (source) {
            DrawerInputSource.Rotary -> 1.02f
            DrawerInputSource.MouseWheel -> 1.08f
        }
    }

    return sign(signedInput) * tunedMagnitude * modeMultiplier
}

fun Modifier.flueRotaryScrollable(
    focusRequester: FocusRequester,
    multiplier: Float = 1f,
    onScroll: (Float) -> Unit
): Modifier {
    fun consume(deltaPixels: Float): Boolean {
        val tuned = tunedRotaryScrollDelta(deltaPixels, multiplier)
        if (tuned == 0f) return false
        onScroll(tuned)
        return true
    }

    return this
        .focusRequester(focusRequester)
        .focusable()
        .onPreRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .onRotaryScrollEvent { consume(it.verticalScrollPixels) }
}

fun Modifier.flueDrawerRotaryScrollable(
    focusRequester: FocusRequester,
    mode: DrawerInputMode,
    onScroll: (Float) -> Unit
): Modifier {
    fun consume(deltaPixels: Float): Boolean {
        val tuned = normalizeDrawerScrollDelta(
            verticalScrollPixels = deltaPixels,
            source = DrawerInputSource.Rotary,
            mode = mode
        )
        if (tuned == 0f) return false
        onScroll(tuned)
        return true
    }

    return this
        .focusRequester(focusRequester)
        .focusable()
        .onPreRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .onRotaryScrollEvent { consume(it.verticalScrollPixels) }
}
