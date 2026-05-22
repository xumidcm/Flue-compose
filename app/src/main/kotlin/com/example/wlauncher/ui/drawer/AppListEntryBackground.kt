package com.flue.launcher.ui.drawer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlin.math.max

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun BoxScope.AppListEntryBackground(
    maxWidth: Dp,
    maxHeight: Dp,
    visuals: AppListEntryVisuals,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (visuals.backgroundFillProgress > 0.001f) {
            drawRect(
                color = color.copy(alpha = visuals.backgroundFillProgress)
            )
        }
        if (visuals.backgroundProgress > 0.001f) {
            val maxSide = max(size.width, size.height)
            val radius = maxSide * (0.1344f + visuals.backgroundProgress * 0.8736f)
            drawCircle(
                color = color.copy(alpha = visuals.backgroundProgress),
                radius = radius,
                center = center
            )
        }
    }
}
