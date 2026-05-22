package com.flue.launcher.data.repository

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.flue.launcher.data.model.WidgetInfo

class WidgetRepository(private val context: Context) {
    private data class ProviderSnapshot(
        val widgets: List<WidgetInfo>,
        val providersByKey: Map<String, AppWidgetProviderInfo>
    )

    @Volatile private var providerSnapshot: ProviderSnapshot? = null

    private val appWidgetManager: AppWidgetManager
        get() = AppWidgetManager.getInstance(context)

    fun getAllWidgets(): List<WidgetInfo> {
        return snapshot().widgets
    }

    fun getWidgetByKey(widgetKey: String, widgetId: Int = -1): WidgetInfo? {
        return findProviderInfo(widgetKey)?.toWidgetInfo(widgetId = widgetId)
    }

    fun findProviderInfo(widgetKey: String): AppWidgetProviderInfo? {
        return snapshot().providersByKey[widgetKey]
    }

    fun invalidateProviders() {
        providerSnapshot = null
    }

    fun parseSlotValue(raw: String?): WidgetInfo? {
        if (raw.isNullOrBlank()) return null
        val separatorIndex = raw.indexOf(':')
        val widgetId = if (separatorIndex > 0) raw.substring(0, separatorIndex).toIntOrNull() else null
        val widgetKey = if (separatorIndex > 0) raw.substring(separatorIndex + 1) else raw
        return getWidgetByKey(widgetKey = widgetKey, widgetId = widgetId ?: -1)
    }

    fun extractWidgetId(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val separatorIndex = raw.indexOf(':')
        return if (separatorIndex <= 0) {
            null
        } else {
            raw.substring(0, separatorIndex).toIntOrNull()
        }
    }

    fun serializeSlotValue(widget: WidgetInfo): String {
        return "${widget.widgetId}:${widget.widgetKey}"
    }

    private fun AppWidgetProviderInfo.toWidgetInfo(widgetId: Int = -1): WidgetInfo {
        val label = runCatching { loadLabel(context.packageManager) }.getOrDefault(provider.shortClassName)
        return WidgetInfo(
            label = label,
            packageName = provider.packageName,
            providerClassName = provider.className,
            widgetId = widgetId,
            minHeightDp = minHeight.coerceAtLeast(0),
            configureClassName = configure?.className
        )
    }

    private fun snapshot(): ProviderSnapshot {
        providerSnapshot?.let { return it }
        return synchronized(this) {
            providerSnapshot ?: appWidgetManager.installedProviders
                .associateBy { "${it.provider.packageName}/${it.provider.className}" }
                .let { providers ->
                    ProviderSnapshot(
                        widgets = providers.values
                            .map { providerInfo -> providerInfo.toWidgetInfo() }
                            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }),
                        providersByKey = providers
                    )
                }
                .also { providerSnapshot = it }
        }
    }
}
