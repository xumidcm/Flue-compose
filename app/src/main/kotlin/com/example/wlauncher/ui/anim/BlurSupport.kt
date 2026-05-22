package com.flue.launcher.ui.anim

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.cloudy
import kotlin.math.roundToInt

@Composable
fun Modifier.platformBlur(
    blurRadiusDp: Float,
    enabled: Boolean
): Modifier = composed {
    val density = LocalDensity.current
    val radiusDp = ((blurRadiusDp.coerceAtLeast(0f) * 2f).roundToInt() / 2f).coerceAtLeast(0f)
    if (!enabled || radiusDp < 0.5f) {
        this
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val radiusPx = with(density) { radiusDp.dp.toPx() }
        val blurEffect = remember(radiusPx) {
            RenderEffect.createBlurEffect(
                radiusPx,
                radiusPx,
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
        this.graphicsLayer {
            renderEffect = blurEffect
        }
    } else {
        this.cloudy(radius = radiusDp.roundToInt().coerceAtLeast(1))
    }
}
