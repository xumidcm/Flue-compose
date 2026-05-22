package com.flue.launcher.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val cachedIcon: ImageBitmap,
    val cachedBlurredIcon: ImageBitmap
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}
