package com.flue.launcher.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object IconPackScanner {
    private val PACK_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "org.adw.launcher.icons.ACTION_PICK_ICON_THEME",
        "org.adw.ActivityStarter.THEMES",
        "com.novalauncher.THEME"
    )

    fun scanInstalled(context: Context): List<IconPackDescriptor> {
        val pm = context.packageManager
        return PACK_ACTIONS
            .flatMap { action -> queryActivities(pm, action) }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { resolveInfo ->
                runCatching {
                    IconPackDescriptor(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(pm)?.toString().orEmpty()
                            .ifBlank { resolveInfo.activityInfo.packageName }
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }

    fun loadMapping(context: Context, packageName: String): IconPackMapping? {
        return runCatching {
            val packContext = context.createPackageContext(
                packageName,
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            val descriptor = IconPackDescriptor(
                packageName = packageName,
                label = packContext.applicationInfo.loadLabel(context.packageManager)?.toString().orEmpty()
                    .ifBlank { packageName }
            )
            val entries = parseAppFilter(packContext)
            if (entries.isEmpty()) return null
            IconPackMapping(descriptor = descriptor, componentToDrawable = entries)
        }.getOrNull()
    }

    fun loadIconDrawable(context: Context, mapping: IconPackMapping, componentKey: String): Drawable? {
        return runCatching {
            val packContext = context.createPackageContext(
                mapping.descriptor.packageName,
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            val drawableName = mapping.componentToDrawable[componentKey] ?: return null
            val resources = packContext.resources
            val resId = resources.getIdentifier(drawableName, "drawable", mapping.descriptor.packageName)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", mapping.descriptor.packageName)
            if (resId == 0) return null
            resources.getDrawable(resId, packContext.theme)
        }.getOrNull()
    }

    private fun parseAppFilter(packContext: Context): Map<String, String> {
        val wrapped = openAppFilterParser(packContext) ?: return emptyMap()
        return try {
            val parser = wrapped.parser
            val entries = linkedMapOf<String, String>()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component").orEmpty()
                    val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                    val normalized = normalizeComponent(component)
                    if (normalized != null && drawable.isNotBlank()) {
                        entries[normalized] = drawable
                    }
                }
                eventType = parser.next()
            }
            entries
        } finally {
            wrapped.close()
        }
    }

    private fun openAppFilterParser(packContext: Context): AutoCloseableXmlPullParser? {
        val packageName = packContext.packageName
        val resources = packContext.resources
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId != 0) {
            return AutoCloseableXmlPullParser(resources.getXml(xmlId))
        }
        val rawId = resources.getIdentifier("appfilter", "raw", packageName)
        if (rawId != 0) {
            val stream = resources.openRawResource(rawId)
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(stream, "utf-8")
            }
            return AutoCloseableXmlPullParser(parser, stream::close)
        }
        return runCatching {
            val stream = packContext.assets.open("appfilter.xml")
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(stream, "utf-8")
            }
            AutoCloseableXmlPullParser(parser, stream::close)
        }.getOrNull()
    }

    private fun normalizeComponent(raw: String): String? {
        val trimmed = raw.removePrefix("ComponentInfo{").removeSuffix("}").trim()
        if (!trimmed.contains("/")) return null
        val parts = trimmed.split("/", limit = 2)
        val pkg = parts[0].trim()
        val cls = parts[1].trim().let { className ->
            when {
                className.startsWith(".") -> pkg + className
                className.contains(".") -> className
                else -> "$pkg.$className"
            }
        }
        if (pkg.isBlank() || cls.isBlank()) return null
        return "$pkg/$cls"
    }

    private fun queryActivities(pm: PackageManager, action: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(Intent(action), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(action), 0)
        }

    private class AutoCloseableXmlPullParser(
        val parser: XmlPullParser,
        private val closeAction: () -> Unit = {}
    ) : AutoCloseable {
        override fun close() {
            closeAction()
        }
    }
}
