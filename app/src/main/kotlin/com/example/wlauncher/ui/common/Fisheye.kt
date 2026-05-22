package com.flue.launcher.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.lerp
import kotlin.math.abs

@Composable
internal fun bottomFisheyeScale(
    listState: LazyListState,
    key: Any,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    val layoutInfo = listState.layoutInfo
    val info = layoutInfo.visibleItemsInfo.find { it.key == key } ?: return 0.92f
    val totalCount = layoutInfo.totalItemsCount.coerceAtLeast(1)
    val isTailItem = info.index >= (totalCount - 2).coerceAtLeast(0)
    val itemCenterY = info.offset + info.size / 2f
    if (itemCenterY <= screenCenterY && !isTailItem) return 1f

    val distance = abs(itemCenterY - screenCenterY)
    val normalized = (distance / (screenHeight / 2f)).coerceIn(0f, 1f)
    val baseScale = 1f - 0.14f * normalized

    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
    val flattenWindow = 4
    val startFlattenIndex = (totalCount - flattenWindow).coerceAtLeast(0)
    val nearBottomProgress = if (lastVisible == null) {
        0f
    } else {
        ((lastVisible.index - startFlattenIndex).toFloat() / flattenWindow.toFloat()).coerceIn(0f, 1f)
    }
    val edgeProgress = if (lastVisible != null && lastVisible.index >= totalCount - 1) {
        val bottomGap = (layoutInfo.viewportEndOffset - (lastVisible.offset + lastVisible.size)).coerceAtLeast(0)
        (1f - (bottomGap / (screenHeight * 0.16f)).coerceIn(0f, 1f)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rawFlattenProgress = (nearBottomProgress * 0.6f + edgeProgress * 0.4f).coerceIn(0f, 1f)
    val flattenProgress = animateFloatAsState(
        targetValue = rawFlattenProgress,
        animationSpec = tween(durationMillis = 260),
        label = "bottom_fisheye_flatten_progress"
    ).value

    val visibleItems = layoutInfo.visibleItemsInfo
    val currentVisibleOrder = visibleItems.indexOfFirst { it.key == key }.coerceAtLeast(0)
    val fromBottomOrder = (visibleItems.lastIndex - currentVisibleOrder).coerceAtLeast(0)
    val stagger = ((fromBottomOrder + 1) * 0.075f).coerceAtMost(0.32f)
    val stagedFlatten = ((flattenProgress - stagger) / (1f - stagger)).coerceIn(0f, 1f)

    return lerp(baseScale, 1f, stagedFlatten)
}
