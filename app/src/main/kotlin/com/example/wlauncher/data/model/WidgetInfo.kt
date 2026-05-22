package com.flue.launcher.data.model

import android.content.ComponentName

data class WidgetInfo(
    val label: String,
    val packageName: String,
    val providerClassName: String,
    val widgetId: Int = -1,
    val minHeightDp: Int = 0,
    val configureClassName: String? = null
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, providerClassName)

    val widgetKey: String
        get() = "$packageName/$providerClassName"
}

sealed interface SideScreenItem {
    data class App(val app: AppInfo) : SideScreenItem
    data class Widget(val widget: WidgetInfo) : SideScreenItem
}
