package com.flue.launcher.ui.input

import androidx.compose.foundation.focusable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.sign

private const val ROTARY_HAPTIC_MIN_INTERVAL_MS = 90L

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

    // Rotary hardware reports very different units across Wear OS and Android watches.
    // Keep tiny pulses usable, but cap large pulses so one crown tick cannot jump pages.
    val tunedMagnitude = when {
        magnitude < 0.12f -> 10f
        magnitude < 0.28f -> 14f
        magnitude < 0.75f -> magnitude * 24f
        magnitude < 2.5f -> magnitude * 12f
        magnitude < 8f -> magnitude * 5f
        magnitude < 24f -> magnitude * 2.2f
        else -> 64f
    }
    val maxStep = 72f * multiplier.coerceIn(0.5f, 1.25f)
    return sign(verticalScrollPixels) * (tunedMagnitude * multiplier).coerceAtMost(maxStep)
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
            magnitude < 0.10f -> 10f
            magnitude < 0.24f -> 14f
            magnitude < 0.75f -> magnitude * 24f
            magnitude < 2.5f -> magnitude * 12f
            magnitude < 8f -> magnitude * 5f
            magnitude < 24f -> magnitude * 2f
            else -> 58f
        }
        DrawerInputSource.MouseWheel -> when {
            magnitude < 0.24f -> 18f
            magnitude < 1.2f -> magnitude * 34f
            magnitude < 4f -> magnitude * 28f
            else -> 76f
        }
    }

    val modeMultiplier = when (mode) {
        DrawerInputMode.Honeycomb -> when (source) {
            DrawerInputSource.Rotary -> 0.68f
            DrawerInputSource.MouseWheel -> 0.76f
        }
        DrawerInputMode.List -> when (source) {
            DrawerInputSource.Rotary -> 0.82f
            DrawerInputSource.MouseWheel -> 0.92f
        }
    }
    val maxStep = when (mode) {
        DrawerInputMode.Honeycomb -> when (source) {
            DrawerInputSource.Rotary -> 54f
            DrawerInputSource.MouseWheel -> 72f
        }
        DrawerInputMode.List -> when (source) {
            DrawerInputSource.Rotary -> 64f
            DrawerInputSource.MouseWheel -> 82f
        }
    }

    return sign(signedInput) * (tunedMagnitude * modeMultiplier).coerceAtMost(maxStep)
}

private fun Modifier.restoreRotaryFocusOnTouch(focusRequester: FocusRequester): Modifier =
    pointerInput(focusRequester) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    runCatching { focusRequester.requestFocus() }
                }
            }
        }
    }

private fun Modifier.tunedPointerWheelScrollable(
    multiplier: Float,
    onScroll: (Float) -> Unit
): Modifier =
    pointerInput(multiplier) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val rawDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    val tuned = tunedRotaryScrollDelta(rawDelta, multiplier)
                    if (tuned != 0f) {
                        onScroll(-tuned)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }

fun Modifier.flueRotaryScrollable(
    focusRequester: FocusRequester,
    multiplier: Float = 1f,
    onRotaryScroll: (() -> Unit)? = null,
    onScroll: (Float) -> Unit
): Modifier {
    var lastHapticAt = 0L
    fun consume(deltaPixels: Float): Boolean {
        val tuned = tunedRotaryScrollDelta(-deltaPixels, multiplier)
        if (tuned == 0f) return false
        if (onRotaryScroll != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastHapticAt >= ROTARY_HAPTIC_MIN_INTERVAL_MS) {
                lastHapticAt = now
                onRotaryScroll()
            }
        }
        onScroll(tuned)
        return true
    }

    return this
        .onPreRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .onRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .tunedPointerWheelScrollable(multiplier, onScroll)
        .restoreRotaryFocusOnTouch(focusRequester)
        .focusRequester(focusRequester)
        .focusable()
}

fun Modifier.flueDrawerRotaryScrollable(
    focusRequester: FocusRequester,
    mode: DrawerInputMode,
    onRotaryScroll: (() -> Unit)? = null,
    onScroll: (Float) -> Unit
): Modifier {
    var lastHapticAt = 0L
    fun consume(deltaPixels: Float): Boolean {
        val tuned = normalizeDrawerScrollDelta(
            verticalScrollPixels = -deltaPixels,
            source = DrawerInputSource.Rotary,
            mode = mode
        )
        if (tuned == 0f) return false
        if (onRotaryScroll != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastHapticAt >= ROTARY_HAPTIC_MIN_INTERVAL_MS) {
                lastHapticAt = now
                onRotaryScroll()
            }
        }
        onScroll(tuned)
        return true
    }

    return this
        .onPreRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .onRotaryScrollEvent { consume(it.verticalScrollPixels) }
        .restoreRotaryFocusOnTouch(focusRequester)
        .focusRequester(focusRequester)
        .focusable()
}
