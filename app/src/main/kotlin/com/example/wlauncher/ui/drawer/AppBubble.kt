package com.flue.launcher.ui.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppBubble(
    icon: ImageBitmap?,
    size: Dp = 54.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    forcePressed: Boolean = false,
    pressScaleTarget: Float = 0.9f,
    pressAnimationDelayMillis: Int = 0,
    pressAnimationDurationMillis: Int = 180,
    onPressedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val activePressed = isPressed || forcePressed
    val pressedScale by animateFloatAsState(
        targetValue = if (activePressed) pressScaleTarget else 1f,
        animationSpec = tween(
            durationMillis = pressAnimationDurationMillis,
            delayMillis = if (activePressed) pressAnimationDelayMillis else 0
        ),
        label = "bubble_scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = if (activePressed) 0.16f else 0f,
        animationSpec = tween(
            durationMillis = pressAnimationDurationMillis,
            delayMillis = if (activePressed) pressAnimationDelayMillis else 0
        ),
        label = "bubble_overlay"
    )

    LaunchedEffect(activePressed) {
        onPressedChange(activePressed)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (activePressed || pressedScale != 1f) {
                    Modifier.graphicsLayer {
                        scaleX = pressedScale
                        scaleY = pressedScale
                    }
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = pressedOverlayAlpha))
            )
        }
    }
}
