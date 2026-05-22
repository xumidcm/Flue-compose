package com.flue.launcher.ui.drawer

internal fun fisheyeMinScale(strengthPercent: Int): Float {
    val strength = strengthPercent.coerceIn(0, 200) / 100f
    return (1f - 0.42f * strength).coerceIn(0.16f, 1f)
}

internal fun edgeSpacingCompressionHorizontalOffset(
    centerX: Float,
    rowCenterY: Float,
    screenCenterX: Float,
    screenCenterY: Float,
    screenHeight: Float,
    itemSize: Float,
    strengthPercent: Int,
    enabled: Boolean
): Float {
    if (!enabled || screenHeight <= 0f || itemSize <= 0f) return 0f
    val horizontalDistance = centerX - screenCenterX
    if (kotlin.math.abs(horizontalDistance) < 0.5f) return 0f
    val rowDistanceFromCenter = rowCenterY - screenCenterY
    val edgeProgress = (kotlin.math.abs(rowDistanceFromCenter) / (screenHeight * 0.5f)).coerceIn(0f, 1f)
    val eased = edgeProgress * edgeProgress * (3f - 2f * edgeProgress)
    val strength = strengthPercent.coerceIn(0, 200) / 100f
    val compression = (0.18f * strength * eased).coerceIn(0f, 0.32f)
    return -horizontalDistance * compression
}
