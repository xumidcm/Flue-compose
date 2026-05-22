package com.flue.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap

class ShortcutReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val added = ShortcutInstallHandler.handle(this, intent)
        Toast.makeText(
            this,
            if (added) "已添加到应用列表" else "无法添加快捷方式",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}

class ShortcutInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val added = ShortcutInstallHandler.handle(context, intent)
        if (added) {
            Toast.makeText(context, "已添加到应用列表", Toast.LENGTH_SHORT).show()
        }
    }
}

private object ShortcutInstallHandler {
    fun handle(context: Context, intent: Intent?): Boolean {
        return when (intent?.action) {
            ACTION_INSTALL_SHORTCUT -> handleLegacyShortcut(context, intent)
            LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT -> handlePinnedShortcut(context, intent)
            else -> false
        }
    }

    private fun handlePinnedShortcut(context: Context, sourceIntent: Intent?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || sourceIntent == null) return false
        val request = sourceIntent.getParcelableCompat(
            LauncherApps.EXTRA_PIN_ITEM_REQUEST,
            LauncherApps.PinItemRequest::class.java
        ) ?: return false
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return false
        val shortcutInfo = request.shortcutInfo ?: return false
        val added = FlueApplication.repositories(context).appRepository.addPinnedShortcut(shortcutInfo)
        if (added) {
            runCatching { request.accept() }
        }
        return added
    }

    private fun handleLegacyShortcut(context: Context, sourceIntent: Intent?): Boolean {
        if (sourceIntent == null) return false
        val shortcutIntent = sourceIntent.getParcelableCompat(
            Intent.EXTRA_SHORTCUT_INTENT,
            Intent::class.java
        ) ?: return false
        val label = sourceIntent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
        val icon = sourceIntent.getParcelableCompat(
            Intent.EXTRA_SHORTCUT_ICON,
            Bitmap::class.java
        ) ?: loadLegacyShortcutIcon(context, sourceIntent)
        return FlueApplication.repositories(context).appRepository.addLegacyShortcut(
            label = label,
            intent = shortcutIntent,
            icon = icon
        )
    }

    private fun loadLegacyShortcutIcon(context: Context, sourceIntent: Intent): Bitmap? {
        val iconResource = sourceIntent.getParcelableCompat(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource::class.java
        ) ?: return null
        return runCatching {
            val resources = context.packageManager.getResourcesForApplication(iconResource.packageName)
            val resourceName = iconResource.resourceName
            val id = if (':' in resourceName && '/' in resourceName) {
                val packageName = resourceName.substringBefore(':')
                val typeAndName = resourceName.substringAfter(':')
                val type = typeAndName.substringBefore('/')
                val entryName = typeAndName.substringAfter('/', typeAndName)
                resources.getIdentifier(entryName, type, packageName)
            } else {
                resources.getIdentifier(resourceName, null, iconResource.packageName)
            }
            if (id == 0) return@runCatching null
            ResourcesCompat.getDrawable(resources, id, null)
                ?.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    private fun <T : Parcelable> Intent.getParcelableCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    private const val ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT"
}
