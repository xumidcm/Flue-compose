package com.flue.launcher.watchface

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.view.View
import com.dudu.wearlauncher.model.WatchFaceBridge
import com.dudu.wearlauncher.ui.WatchSurfaceBaseActivity
import com.flue.launcher.EXTRA_SETTINGS_DESTINATION
import com.flue.launcher.EXTRA_SETTINGS_RETURN_TO_FACE
import com.flue.launcher.SETTINGS_DESTINATION_WATCH_FACES
import com.flue.launcher.SettingsActivity
import dalvik.system.DexClassLoader

object LunchWatchFaceRuntime {
    private const val LEGACY_SURFACE_CLASS = "com.dudu.wearlauncher.model.WatchSurface"
    private const val LUNCH_PACKAGE = "com.dudu.wearlauncher"
    private const val LUNCH_CHOOSER_CLASS = "com.dudu.wearlauncher.ui.home.ChooseWatchFaceActivity"

    fun loadExternalWatchFace(hostContext: Context, descriptor: LunchWatchFaceDescriptor): LunchWatchFaceLoadResult {
        require(!descriptor.isBuiltin) { "Built-in watchface should not use external loader" }
        val dexLoader = createDexClassLoader(hostContext, descriptor)
        val pluginContext = createPluginContext(hostContext, descriptor, dexLoader)
        val clazz = dexLoader.loadClass(descriptor.watchFaceClassName ?: error("Missing watchface class for ${descriptor.id}"))
        val view = instantiateWatchFace(clazz, pluginContext, descriptor.sourceApkPath.orEmpty())
        return LunchWatchFaceLoadResult(descriptor = descriptor, view = view, bridge = WatchFaceBridge(view))
    }

    fun openSettings(context: Context, descriptor: LunchWatchFaceDescriptor): Boolean {
        val settingsClassName = descriptor.settingsEntryClassName ?: return false
        return runCatching {
            val loader = createDexClassLoader(context, descriptor)
            val clazz = loader.loadClass(settingsClassName)
            if (Activity::class.java.isAssignableFrom(clazz)) {
                context.startActivity(
                    Intent().apply {
                        component = ComponentName(descriptor.packageName!!, settingsClassName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } else if (isLegacyWatchSurface(clazz)) {
                context.startActivity(
                    Intent(context, WatchSurfaceBaseActivity::class.java).apply {
                        putExtra("wfName", descriptor.watchFaceName)
                        putExtra("wsfClassName", settingsClassName)
                        putExtra("packageName", descriptor.packageName)
                        putExtra("apkPath", descriptor.sourceApkPath)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } else {
                return false
            }
            true
        }.getOrDefault(false)
    }

    fun instantiateWatchSurface(hostContext: Context, descriptor: LunchWatchFaceDescriptor, className: String): View {
        val loader = createDexClassLoader(hostContext, descriptor)
        val pluginContext = createPluginContext(hostContext, descriptor, loader, useLegacySurfaceScale = true)
        val clazz = loader.loadClass(className)
        val instance = instantiateLegacyView(clazz, pluginContext, descriptor.sourceApkPath.orEmpty())
        require(instance is View) { "Settings surface is not a View" }
        return instance
    }

    fun createDexClassLoader(context: Context, descriptor: LunchWatchFaceDescriptor): DexClassLoader {
        val apkPath = descriptor.sourceApkPath ?: error("Missing apk path for ${descriptor.id}")
        return LunchWatchFaceScanner.createDexClassLoader(context, apkPath)
    }

    private fun createPluginContext(
        hostContext: Context,
        descriptor: LunchWatchFaceDescriptor,
        classLoader: ClassLoader,
        useLegacySurfaceScale: Boolean = false
    ): Context {
        val packageName = descriptor.packageName ?: error("Missing package name for ${descriptor.id}")
        val base = hostContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        val fitContext = createFitDisplayContext(base, if (useLegacySurfaceScale) 360 else 320)
        injectClassLoader(fitContext, classLoader)
        return PluginRuntimeContext(
            base = fitContext,
            hostContext = hostContext,
            descriptor = descriptor,
            pluginClassLoader = classLoader
        )
    }

    private fun createFitDisplayContext(context: Context, referenceWidth: Int): Context {
        return runCatching {
            val density = context.resources.displayMetrics.widthPixels.toFloat() / referenceWidth.toFloat()
            val configuration = Configuration(context.resources.configuration)
            configuration.smallestScreenWidthDp = 320
            configuration.densityDpi = (320f * density).toInt()
            context.createConfigurationContext(configuration)
        }.getOrElse { context }
    }

    private fun injectClassLoader(context: Context, classLoader: ClassLoader) {
        runCatching {
            findField(context.javaClass, "mClassLoader")?.let { field ->
                field.isAccessible = true
                field.set(context, classLoader)
            }
        }
    }

    private fun instantiateWatchFace(clazz: Class<*>, pluginContext: Context, apkPath: String): View {
        return try {
            clazz.getConstructor(Context::class.java).newInstance(pluginContext) as View
        } catch (_: NoSuchMethodException) {
            instantiateLegacyView(clazz, pluginContext, apkPath) as View
        }
    }

    private fun instantiateLegacyView(clazz: Class<*>, context: Context, apkPath: String): Any {
        return try {
            clazz.getConstructor(Context::class.java, String::class.java).newInstance(context, apkPath)
        } catch (_: NoSuchMethodException) {
            clazz.getConstructor(Context::class.java).newInstance(context)
        }
    }

    private fun isLegacyWatchSurface(clazz: Class<*>): Boolean {
        var current: Class<*>? = clazz
        while (current != null) {
            if (current.name == LEGACY_SURFACE_CLASS) return true
            current = current.superclass
        }
        return false
    }

    private fun findField(type: Class<*>, name: String): java.lang.reflect.Field? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    private class PluginRuntimeContext(
        base: Context,
        private val hostContext: Context,
        private val descriptor: LunchWatchFaceDescriptor,
        private val pluginClassLoader: ClassLoader
    ) : ContextWrapper(base) {

        override fun getClassLoader(): ClassLoader = pluginClassLoader

        override fun getApplicationContext(): Context = this

        override fun startActivity(intent: Intent?) {
            if (intent == null) return
            hostContext.startActivity(routeIntent(intent))
        }

        override fun startActivity(intent: Intent?, options: Bundle?) {
            if (intent == null) return
            hostContext.startActivity(routeIntent(intent), options)
        }

        private fun routeIntent(source: Intent): Intent {
            val component = source.component
            val matchesLunchChooser =
                component?.packageName == LUNCH_PACKAGE &&
                    component.className == LUNCH_CHOOSER_CLASS

            if (!matchesLunchChooser) {
                return Intent(source).apply {
                    if (hostContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            return Intent(hostContext, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SETTINGS_DESTINATION, SETTINGS_DESTINATION_WATCH_FACES)
                putExtra(EXTRA_SETTINGS_RETURN_TO_FACE, true)
                if (hostContext !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
