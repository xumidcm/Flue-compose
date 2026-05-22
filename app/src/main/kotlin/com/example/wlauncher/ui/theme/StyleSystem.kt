package com.flue.launcher.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Stable
data class LauncherStyle(
    val uiStyle: UiStyle,
    val screenBackground: Color,
    val screenScrim: Brush,
    val cardColor: Color,
    val pressedCardColor: Color,
    val selectedCardColor: Color,
    val outlineColor: Color,
    val titleColor: Color,
    val bodyColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val positiveColor: Color,
    val topBarChipColor: Color,
    val topBarTextColor: Color,
    val appListBaseColor: Color,
    val appListFadeColor: Color,
    val appListPressedOverlay: Color,
    val cardShape: Shape,
    val compactShape: Shape,
    val bubbleShape: Shape,
    val watchFaceOverlayStrength: Float,
    val faceSwitchEnterSpec: FiniteAnimationSpec<Float>,
    val faceSwitchExitSpec: FiniteAnimationSpec<Float>,
    val pageMotionSpec: FiniteAnimationSpec<Float>,
    val launchMaskEnterSpec: FiniteAnimationSpec<Float>,
    val launchMaskExitSpec: FiniteAnimationSpec<Float>,
    val pressScaleSpec: FiniteAnimationSpec<Float>,
    val switchThumbTravel: Float
)

private val LocalLauncherStyle = compositionLocalOf { appleWatchLauncherStyle(darkMode = true) }

val UiStyle.isMaterial: Boolean
    get() = this == UiStyle.MATERIAL_3

fun appleWatchLauncherStyle(darkMode: Boolean): LauncherStyle {
    val card = if (darkMode) Color(0x1FFFFFFF) else Color(0x16000000)
    return LauncherStyle(
        uiStyle = UiStyle.APPLE_WATCH,
        screenBackground = if (darkMode) Color.Black else Color(0xFFF7F7FA),
        screenScrim = Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = if (darkMode) 0.0f else 0.05f),
                Color.Transparent,
                Color.Black.copy(alpha = if (darkMode) 0.34f else 0.10f)
            )
        ),
        cardColor = card,
        pressedCardColor = if (darkMode) Color(0xFF1A1A1D) else Color(0xFFE8E8ED),
        selectedCardColor = WatchColors.ActiveCyan.copy(alpha = if (darkMode) 0.16f else 0.20f),
        outlineColor = Color.White.copy(alpha = if (darkMode) 0.05f else 0.08f),
        titleColor = if (darkMode) Color.White else Color(0xFF101216),
        bodyColor = if (darkMode) Color.White else Color(0xFF101216),
        secondaryTextColor = WatchColors.TextTertiary,
        accentColor = WatchColors.ActiveCyan,
        positiveColor = WatchColors.ActiveGreen,
        topBarChipColor = Color.White.copy(alpha = if (darkMode) 0.08f else 0.10f),
        topBarTextColor = if (darkMode) Color.White else Color(0xFF16181D),
        appListBaseColor = card,
        appListFadeColor = Color.Black.copy(alpha = if (darkMode) 0.72f else 0.08f),
        appListPressedOverlay = Color.White.copy(alpha = if (darkMode) 0.08f else 0.04f),
        cardShape = RoundedCornerShape(24.dp),
        compactShape = RoundedCornerShape(18.dp),
        bubbleShape = CircleShape,
        watchFaceOverlayStrength = 1f,
        faceSwitchEnterSpec = tween(durationMillis = 220, delayMillis = 70),
        faceSwitchExitSpec = tween(durationMillis = 180),
        pageMotionSpec = tween(durationMillis = 220),
        launchMaskEnterSpec = tween(durationMillis = 140),
        launchMaskExitSpec = tween(durationMillis = 180),
        pressScaleSpec = spring(stiffness = 820f, dampingRatio = 0.74f),
        switchThumbTravel = 22f
    )
}

fun materialLauncherStyle(darkMode: Boolean): LauncherStyle {
    val bg = if (darkMode) Color(0xFF0D1118) else Color(0xFFF5F1EA)
    val surface = if (darkMode) Color(0xFF18212D) else Color(0xFFE8E1D6)
    val surfaceAlt = if (darkMode) Color(0xFF243244) else Color(0xFFD9D0C2)
    val accent = if (darkMode) Color(0xFF8CB4FF) else Color(0xFF295EA8)
    val text = if (darkMode) Color(0xFFF4F7FB) else Color(0xFF1E2938)
    return LauncherStyle(
        uiStyle = UiStyle.MATERIAL_3,
        screenBackground = bg,
        screenScrim = Brush.verticalGradient(
            listOf(
                accent.copy(alpha = if (darkMode) 0.16f else 0.12f),
                Color.Transparent,
                surface.copy(alpha = 0.86f)
            )
        ),
        cardColor = surface,
        pressedCardColor = surfaceAlt,
        selectedCardColor = accent.copy(alpha = if (darkMode) 0.22f else 0.18f),
        outlineColor = accent.copy(alpha = if (darkMode) 0.22f else 0.16f),
        titleColor = text,
        bodyColor = text,
        secondaryTextColor = text.copy(alpha = 0.72f),
        accentColor = accent,
        positiveColor = if (darkMode) Color(0xFF7AD982) else Color(0xFF1C7B39),
        topBarChipColor = surfaceAlt,
        topBarTextColor = text,
        appListBaseColor = surface,
        appListFadeColor = bg.copy(alpha = 0.92f),
        appListPressedOverlay = accent.copy(alpha = if (darkMode) 0.12f else 0.08f),
        cardShape = RoundedCornerShape(32.dp),
        compactShape = RoundedCornerShape(24.dp),
        bubbleShape = RoundedCornerShape(30.dp),
        watchFaceOverlayStrength = 1.25f,
        faceSwitchEnterSpec = tween(durationMillis = 300, delayMillis = 20),
        faceSwitchExitSpec = tween(durationMillis = 220),
        pageMotionSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        launchMaskEnterSpec = tween(durationMillis = 220),
        launchMaskExitSpec = tween(durationMillis = 220),
        pressScaleSpec = spring(stiffness = 560f, dampingRatio = 0.82f),
        switchThumbTravel = 20f
    )
}

@Composable
fun ProvideLauncherStyle(
    uiStyle: UiStyle,
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    val style = remember(uiStyle, darkMode) {
        if (uiStyle.isMaterial) materialLauncherStyle(darkMode) else appleWatchLauncherStyle(darkMode)
    }
    CompositionLocalProvider(LocalLauncherStyle provides style, content = content)
}

object LauncherTheme {
    val style: LauncherStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalLauncherStyle.current
}

fun styleAwareEnterTransition(style: LauncherStyle): EnterTransition = EnterTransition.None

fun styleAwareExitTransition(style: LauncherStyle): ExitTransition = ExitTransition.None
