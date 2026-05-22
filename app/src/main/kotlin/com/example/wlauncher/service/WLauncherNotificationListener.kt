package com.flue.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotifData(
    val key: String,
    val packageName: String,
    val groupKey: String?,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: ImageBitmap?,
    val isClearable: Boolean,
    val contentIntentAvailable: Boolean,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean,
    val isForegroundService: Boolean
)

class WLauncherNotificationListener : NotificationListenerService() {

    companion object {
        private const val NOTIFICATION_ICON_SIZE_PX = 96
        private val IM_AVATAR_PACKAGES = setOf(
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.tencent.tim"
        )
        private val _notifications = MutableStateFlow<List<NotifData>>(emptyList())
        val notifications: StateFlow<List<NotifData>> = _notifications.asStateFlow()

        private var instance: WLauncherNotificationListener? = null
        private val pendingIntentMap = mutableMapOf<String, PendingIntent?>()
        private val packageIconCache = mutableMapOf<String, ImageBitmap?>()

        fun isConnected() = instance != null

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissNotifications(keys: List<String>) {
            val service = instance ?: return
            keys.forEach(service::cancelNotification)
        }

        fun openNotification(key: String): Boolean {
            val pendingIntent = synchronized(pendingIntentMap) { pendingIntentMap[key] } ?: return false
            return runCatching {
                pendingIntent.send()
            }.isSuccess
        }
    }

    override fun onListenerConnected() {
        instance = this
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
        _notifications.value = emptyList()
        synchronized(pendingIntentMap) { pendingIntentMap.clear() }
        synchronized(packageIconCache) { packageIconCache.clear() }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val sbns = activeNotifications ?: return
            val pm = applicationContext.packageManager
            _notifications.value = sbns
                .filter { sbn ->
                    val flags = sbn.notification.flags
                    val isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
                    val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
                    val isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
                    !isGroupSummary && !isOngoing && !isForegroundService
                }
                .sortedByDescending { it.postTime }
                .take(20)
                .map { sbn ->
                    val n = sbn.notification
                    val extras = n.extras
                    val flags = n.flags
                    val appLabel = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                    } catch (_: Exception) { sbn.packageName }

                    val iconBitmap = resolveNotificationIcon(sbn, n)

                    NotifData(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        groupKey = sbn.groupKey,
                        appLabel = appLabel,
                        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                        time = sbn.postTime,
                        icon = iconBitmap,
                        isClearable = sbn.isClearable,
                        contentIntentAvailable = n.contentIntent != null,
                        isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
                        isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
                        isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
                    )
                }.also { notifications ->
                    synchronized(pendingIntentMap) {
                        pendingIntentMap.clear()
                        sbns.forEach { sbn ->
                            pendingIntentMap[sbn.key] = sbn.notification.contentIntent
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    private fun resolveNotificationIcon(
        sbn: StatusBarNotification,
        notification: Notification
    ): ImageBitmap? {
        return resolveMessagingAvatar(sbn, notification)
            ?: resolveLargeNotificationIcon(notification)
            ?: resolveApplicationIcon(sbn.packageName)
            ?: resolveSmallNotificationIcon(notification)
    }

    private fun resolveMessagingAvatar(
        sbn: StatusBarNotification,
        notification: Notification
    ): ImageBitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        if (sbn.packageName !in IM_AVATAR_PACKAGES) return null
        return readMessagingPersons(notification.extras)
            .mapNotNull { it.icon?.toImageBitmap() }
            .firstOrNull()
    }

    private fun readMessagingPersons(extras: Bundle): Sequence<Person> {
        val live = extras.getParcelableArray(Notification.EXTRA_MESSAGES).orEmpty().asSequence()
        val history = extras.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES).orEmpty().asSequence()
        return (live + history)
            .mapNotNull { parcelable ->
                (parcelable as? Bundle)?.let(::extractSenderPerson)
            }
    }

    private fun extractSenderPerson(bundle: Bundle): Person? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable("sender_person", Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable("sender_person") as? Person
        }
    }

    private fun resolveLargeNotificationIcon(notification: Notification): ImageBitmap? {
        val extras = notification.extras
        val notificationLargeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()
        } else {
            null
        }
        val iconCandidates = listOfNotNull<Icon>(
            notificationLargeIcon,
            extras.get(Notification.EXTRA_LARGE_ICON_BIG) as? Icon,
            extras.get(Notification.EXTRA_LARGE_ICON) as? Icon
        )
        iconCandidates.firstNotNullOfOrNull { it.toImageBitmap() }?.let { return it }

        val bitmapCandidates = listOfNotNull(
            extras.get(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap,
            extras.get(Notification.EXTRA_LARGE_ICON) as? Bitmap
        )
        return bitmapCandidates.firstNotNullOfOrNull { it.scaleToNotificationIcon() }
    }

    private fun resolveApplicationIcon(packageName: String): ImageBitmap? {
        synchronized(packageIconCache) {
            if (packageIconCache.containsKey(packageName)) {
                return packageIconCache[packageName]
            }
        }
        val resolved = runCatching {
            applicationContext.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(NOTIFICATION_ICON_SIZE_PX, NOTIFICATION_ICON_SIZE_PX)
                .asImageBitmap()
        }.getOrNull()
        synchronized(packageIconCache) {
            packageIconCache[packageName] = resolved
        }
        return resolved
    }

    private fun resolveSmallNotificationIcon(notification: Notification): ImageBitmap? {
        return runCatching {
            notification.smallIcon
                ?.loadDrawable(applicationContext)
                ?.toBitmap(NOTIFICATION_ICON_SIZE_PX, NOTIFICATION_ICON_SIZE_PX)
                ?.asImageBitmap()
        }.getOrNull()
    }

    private fun Icon.toImageBitmap(): ImageBitmap? {
        return runCatching {
            loadDrawable(applicationContext)
                ?.toBitmap(NOTIFICATION_ICON_SIZE_PX, NOTIFICATION_ICON_SIZE_PX)
                ?.asImageBitmap()
        }.getOrNull()
    }

    private fun Bitmap.scaleToNotificationIcon(): ImageBitmap {
        return if (width == NOTIFICATION_ICON_SIZE_PX && height == NOTIFICATION_ICON_SIZE_PX) {
            asImageBitmap()
        } else {
            Bitmap.createScaledBitmap(
                this,
                NOTIFICATION_ICON_SIZE_PX,
                NOTIFICATION_ICON_SIZE_PX,
                true
            ).asImageBitmap()
        }
    }
}
