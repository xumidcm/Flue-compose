package com.flue.launcher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.ui.theme.WatchColors

private val BatteryOuterShape = RoundedCornerShape(14.dp)
private val BatteryInnerShape = RoundedCornerShape(10.dp)
private val BatteryLabelStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
fun WatchBatteryPill(
    level: Int,
    charging: Boolean,
    modifier: Modifier = Modifier,
    sizeScale: Float = 1f
) {
    val tint = when {
        level <= 10 -> WatchColors.ActiveRed
        level <= 20 -> WatchColors.ActiveOrange
        else -> WatchColors.ActiveGreen
    }
    val backgroundTint = if (charging) Color(0xFF1F6C35) else Color(0xFF141414)

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = sizeScale
            scaleY = sizeScale
        },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(24.dp)
                .clip(BatteryOuterShape)
                .background(backgroundTint),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(16.dp)
                    .clip(BatteryInnerShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((level / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(BatteryInnerShape)
                        .background(tint.copy(alpha = 0.24f))
                )
                Text(
                    text = "${level}%",
                    color = tint,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = BatteryLabelStyle,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (charging) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color(0xFFFFD54A),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .size(13.dp)
            )
        }
    }
}
