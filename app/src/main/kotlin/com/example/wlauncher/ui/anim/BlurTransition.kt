package com.flue.launcher.ui.anim

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.flue.launcher.ui.navigation.ScreenState

/**
 * 每个层在某个状态下的 scale / blur / opacity 目标值。
 * 完整复刻 HTML 原型中的 CSS transition 参数。
 */
data class LayerAnimValues(
    val scale: Float = 1f,
    val blur: Float = 0f,    // dp
    val alpha: Float = 1f,
    val translationY: Float = 0f  // 占屏幕高度比例 (-1f = 向上移出, 1f = 向下移出)
)

/**
 * 获取"表盘层"在各状态下的动画目标值。
 * 对应 HTML: #faces-layer 在不同 state- class 下的 CSS。
 */
fun faceLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.Face -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f)
    ScreenState.Apps, ScreenState.Settings -> LayerAnimValues(scale = 2.5f, blur = 15f, alpha = 0f)
    ScreenState.App -> LayerAnimValues(scale = 2.5f, blur = 15f, alpha = 0f)
    ScreenState.Stack -> LayerAnimValues(scale = 0.85f, blur = 5f, alpha = 0.3f)
    ScreenState.Notifications -> LayerAnimValues(scale = 0.85f, blur = 5f, alpha = 0.3f)
    ScreenState.ControlCenter -> LayerAnimValues(scale = 0.9f, blur = 8f, alpha = 0.5f)
}

/**
 * 获取"应用列表层"在各状态下的动画目标值。
 * 对应 HTML: #app-list 在不同 state- class 下的 CSS。
 */
fun appListLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.Face -> LayerAnimValues(scale = 0.2f, blur = 10f, alpha = 0f)
    ScreenState.Apps -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f)
    ScreenState.Settings -> LayerAnimValues(scale = 0.9f, blur = 8f, alpha = 0.3f)
    ScreenState.App -> LayerAnimValues(scale = 4f, blur = 10f, alpha = 0f)
    else -> LayerAnimValues(scale = 0.2f, blur = 10f, alpha = 0f)
}

/**
 * 获取"应用视图层"在各状态下的动画目标值。
 * 对应 HTML: #app-view 在不同 state- class 下的 CSS。
 */
fun appViewLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.App -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f)
    ScreenState.Apps -> LayerAnimValues(scale = 0f, blur = 0f, alpha = 0f)
    else -> LayerAnimValues(scale = 0f, blur = 0f, alpha = 0f)
}

/**
 * 获取"智能叠放层"在各状态下的动画目标值。
 */
fun stackLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.Stack -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f, translationY = 0f)
    ScreenState.Apps -> LayerAnimValues(scale = 1.5f, blur = 12f, alpha = 0f, translationY = -0.5f)
    else -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 0f, translationY = 1f)
}

/**
 * 获取"通知层"在各状态下的动画目标值。
 */
fun notificationLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.Notifications -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f, translationY = 0f)
    else -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 0f, translationY = 1f)
}

/**
 * 获取"控制中心层"在各状态下的动画目标值。
 */
fun controlCenterLayerValues(state: ScreenState): LayerAnimValues = when (state) {
    ScreenState.ControlCenter -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 1f, translationY = 0f)
    else -> LayerAnimValues(scale = 1f, blur = 0f, alpha = 0f, translationY = 1f)
}

// 带轻微回弹的 spring，模拟 HTML 的 cubic-bezier(0.32, 1.2, 0.4, 1)
val TransitionSpring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness = Spring.StiffnessMediumLow
)

val AlphaSpec = tween<Float>(durationMillis = 400)
