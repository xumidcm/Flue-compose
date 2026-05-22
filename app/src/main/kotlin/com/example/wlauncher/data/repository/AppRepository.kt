package com.flue.launcher.data.repository

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.iconpack.IconPackMapping
import com.flue.launcher.iconpack.IconPackScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppRepository(private val context: Context) {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private var customOrder: List<String> = emptyList()
    private var customOrderIndexMap: Map<String, Int> = emptyMap()
    private var hiddenComponents: Set<String> = emptySet()
    private var currentIconSize = 128
    private var iconPackPackage: String? = null
    private var useLegacyCircularIcons = true
    private var refreshGeneration = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val installTimeCache = mutableMapOf<String, Long>()
    private val iconCache = object : LinkedHashMap<String, Pair<ImageBitmap, ImageBitmap>>(160, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<ImageBitmap, ImageBitmap>>?): Boolean {
            return size > 160
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refreshAsync()
        }
    }

    init {
        refreshAsync()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
    }

    fun setCustomOrder(order: List<String>) {
        customOrder = order
        customOrderIndexMap = order.withIndex().associate { it.value to it.index }
        reorder()
    }

    fun setHiddenComponents(components: Set<String>) {
        hiddenComponents = components
        applyFilters()
    }

    fun setIconPackPackage(packageName: String?) {
        iconPackPackage = packageName?.takeIf { it.isNotBlank() }
        synchronized(iconCache) { iconCache.clear() }
        refreshAsync(currentIconSize)
    }

    fun setLegacyCircularIconsEnabled(enabled: Boolean) {
        if (useLegacyCircularIcons == enabled) return
        useLegacyCircularIcons = enabled
        synchronized(iconCache) { iconCache.clear() }
        refreshAsync(currentIconSize)
    }

    private fun reorder() {
        _allApps.value = sortApps(_allApps.value)
        applyFilters()
    }

    fun refresh(iconSize: Int = currentIconSize) {
        val generation = synchronized(this) {
            refreshGeneration += 1
            refreshGeneration
        }
        val previousIconSize = currentIconSize
        currentIconSize = iconSize
        if (previousIconSize != iconSize) {
            synchronized(iconCache) { iconCache.clear() }
        }
        val iconPackPackageSnapshot = iconPackPackage
        val useLegacyCircularIconsSnapshot = useLegacyCircularIcons
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        val myPackage = context.packageName

        val iconPackMapping: IconPackMapping? = iconPackPackageSnapshot?.let { IconPackScanner.loadMapping(context, it) }

        val resolveList = resolveInfos
            .filter { ri ->
                !(ri.activityInfo.packageName == myPackage &&
                    ri.activityInfo.name == "com.flue.launcher.LauncherActivity")
            }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
        val loadedApps = ArrayList<AppInfo>(resolveList.size)

        resolveList.forEach { ri ->
            val packageName = ri.activityInfo.packageName
            val componentKey = "$packageName/${ri.activityInfo.name}"
            val iconCacheKey = "${iconPackPackageSnapshot ?: "sys"}|$componentKey|$iconSize|${if (useLegacyCircularIconsSnapshot) "legacy" else "plain"}"
            val cachedIcons = synchronized(iconCache) { iconCache[iconCacheKey] } ?: run {
                val packedIcon = iconPackMapping?.let { IconPackScanner.loadIconDrawable(context, it, componentKey) }
                val resolvedIconDrawable = packedIcon ?: ri.loadIcon(pm)
                val baseBitmap = if (useLegacyCircularIconsSnapshot && packedIcon == null) {
                    createCircularBitmap(
                        drawableToBitmap(resolvedIconDrawable, iconSize),
                        edgeInsetPx = iconSize * 0.015f
                    )
                } else {
                    resolvedIconDrawable.toBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                }
                val sharp = baseBitmap.asImageBitmap()
                val softened = createSoftenedBitmap(baseBitmap).asImageBitmap()
                synchronized(iconCache) {
                    iconCache[iconCacheKey] = sharp to softened
                }
                sharp to softened
            }
            loadedApps += AppInfo(
                label = ri.loadLabel(pm).toString(),
                packageName = packageName,
                activityName = ri.activityInfo.name,
                cachedIcon = cachedIcons.first,
                cachedBlurredIcon = cachedIcons.second
            )
        }

        if (synchronized(this) { generation != refreshGeneration }) return

        val knownPackages = loadedApps.mapTo(linkedSetOf()) { it.packageName }
        synchronized(installTimeCache) {
            installTimeCache.keys.retainAll(knownPackages)
        }

        _allApps.value = sortApps(loadedApps)
        applyFilters()
    }

    fun launchApp(appInfo: AppInfo): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(appInfo.packageName, appInfo.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val options = android.app.ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        return try {
            context.startActivity(intent, options.toBundle())
            true
        } catch (_: ActivityNotFoundException) {
            refreshAsync()
            false
        } catch (_: SecurityException) {
            refreshAsync()
            false
        }
    }

    fun destroy() {
        scope.cancel()
        synchronized(iconCache) { iconCache.clear() }
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }
    }

    private fun refreshAsync(iconSize: Int = currentIconSize) {
        scope.launch {
            refresh(iconSize)
        }
    }

    private fun createSoftenedBitmap(source: Bitmap): Bitmap {
        val downscaled = Bitmap.createScaledBitmap(
            source,
            (source.width * 0.25f).toInt().coerceAtLeast(1),
            (source.height * 0.25f).toInt().coerceAtLeast(1),
            true
        )
        return Bitmap.createScaledBitmap(downscaled, source.width, source.height, true)
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createCircularBitmap(source: Bitmap, edgeInsetPx: Float = 0f): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isFilterBitmap = true
            isDither = true
        }
        val radius = (minOf(source.width, source.height) / 2f - edgeInsetPx).coerceAtLeast(0f)
        canvas.drawCircle(source.width / 2f, source.height / 2f, radius, paint)
        return output
    }

    private fun orderRank(app: AppInfo): Int {
        if (customOrder.isEmpty()) return Int.MAX_VALUE
        return customOrderIndexMap[app.componentKey]
            ?: customOrderIndexMap[app.packageName]
            ?: Int.MAX_VALUE
    }

    private fun packageInstallTime(packageName: String): Long {
        synchronized(installTimeCache) {
            installTimeCache[packageName]?.let { return it }
        }
        val installTime = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0).firstInstallTime
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
        synchronized(installTimeCache) {
            installTimeCache[packageName] = installTime
        }
        return installTime
    }

    private fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        return if (customOrder.isNotEmpty()) {
            apps.sortedWith(
                compareBy<AppInfo> { orderRank(it) }
                    .thenBy { packageInstallTime(it.packageName) }
                    .thenBy { it.label.lowercase() }
            )
        } else {
            apps.sortedWith(
                compareBy<AppInfo> { packageInstallTime(it.packageName) }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    private fun applyFilters() {
        _apps.value = _allApps.value.filterNot { app ->
            hiddenComponents.contains(app.componentKey) || hiddenComponents.contains(app.packageName)
        }
    }
}
