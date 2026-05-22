package com.flue.launcher.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
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
    val isForegroundService: Boolean,
    val isSystemHidden: Boolean,
    val isNoisyOngoing: Boolean,
    val actions: List<NotifActionData> = emptyList()
)

data class NotifActionData(
    val key: String,
    val title: String
)

class WLauncherNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WLauncherNotif"
        private const val NOTIFICATION_ICON_SIZE_PX = 96
        private val IM_AVATAR_PACKAGES = setOf(
            "com.tencent.mobileqq",
            "com.tencent.qqlite",
            "com.tencent.mm",
            "com.tencent.tim"
        )
        private val _notifications = MutableStateFlow<List<NotifData>>(emptyList())
        val notifications: StateFlow<List<NotifData>> = _notifications.asStateFlow()

        private var instance: WLauncherNotificationListener? = null
        private val pendingIntentMap = mutableMapOf<String, PendingIntent?>()
        private val pendingActionIntentMap = mutableMapOf<String, PendingIntent>()
        private val packageIconCache = mutableMapOf<String, ImageBitmap?>()

        fun isConnected() = instance != null

        fun requestRebindIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || instance != null) return
            runCatching {
                requestRebind(ComponentName(context, WLauncherNotificationListener::class.java))
                Log.d(TAG, "Requested notification listener rebind")
            }.onFailure { error ->
                Log.e(TAG, "Failed to request notification listener rebind", error)
            }
        }

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

        fun runNotificationAction(actionKey: String): Boolean {
            val pendingIntent = synchronized(pendingActionIntentMap) {
                pendingActionIntentMap[actionKey]
            } ?: return false
            return runCatching {
                pendingIntent.send()
            }.isSuccess
        }

        private fun actionKey(notificationKey: String, actionIndex: Int): String {
            return "$notificationKey/action/$actionIndex"
        }
    }

    override fun onListenerConnected() {
        instance = this
        Log.d(TAG, "Notification listener connected")
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
        Log.d(TAG, "Notification listener disconnected")
        _notifications.value = emptyList()
        synchronized(pendingIntentMap) { pendingIntentMap.clear() }
        synchronized(pendingActionIntentMap) { pendingActionIntentMap.clear() }
        synchronized(packageIconCache) { packageIconCache.clear() }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        val sbns = runCatching { activeNotifications.orEmpty() }
            .onFailure { error ->
                Log.e(TAG, "Failed to read active notifications", error)
            }
            .getOrDefault(emptyArray())
        val pm = applicationContext.packageManager
        val parsedNotifications = sbns
            .filter { sbn ->
                val flags = sbn.notification.flags
                val isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
                !isGroupSummary
            }
            .sortedByDescending { it.postTime }
            .take(20)
            .mapNotNull { sbn ->
                runCatching { buildNotifData(pm, sbn) }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to parse notification ${sbn.key} from ${sbn.packageName}", error)
                    }
                    .getOrNull()
            }

        _notifications.value = parsedNotifications
        synchronized(pendingIntentMap) {
            pendingIntentMap.clear()
            parsedNotifications.forEach { notification ->
                pendingIntentMap[notification.key] = sbns
                    .firstOrNull { it.key == notification.key }
                    ?.notification
                    ?.contentIntent
            }
        }
        synchronized(pendingActionIntentMap) {
            pendingActionIntentMap.clear()
            parsedNotifications.forEach { notification ->
                val sbn = sbns.firstOrNull { it.key == notification.key } ?: return@forEach
                sbn.notification.actions.orEmpty().forEachIndexed { index, action ->
                    val actionIntent = action.actionIntent
                    val title = action.title?.toString()?.trim().orEmpty()
                    if (actionIntent != null && title.isNotBlank()) {
                        pendingActionIntentMap[actionKey(sbn.key, index)] = actionIntent
                    }
                }
            }
        }
        Log.d(TAG, "Notification refresh complete: active=${sbns.size}, visible=${parsedNotifications.size}")
    }

    private fun buildNotifData(
        pm: android.content.pm.PackageManager,
        sbn: StatusBarNotification
    ): NotifData {
        val notification = sbn.notification
        val extras = notification.extras
        val flags = notification.flags
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val appLabel = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        return NotifData(
            key = sbn.key,
            packageName = sbn.packageName,
            groupKey = sbn.groupKey,
            appLabel = appLabel,
            title = title,
            text = text,
            time = sbn.postTime,
            icon = resolveNotificationIcon(sbn, notification),
            isClearable = sbn.isClearable,
            contentIntentAvailable = notification.contentIntent != null,
            isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isSystemHidden = sbn.isSystemHiddenNotification(),
            isNoisyOngoing = isNoisyOngoingNotification(sbn, appLabel, title, text),
            actions = notification.actions.orEmpty().mapIndexedNotNull { index, action ->
                val title = action.title?.toString()?.trim().orEmpty()
                if (title.isBlank() || action.actionIntent == null) {
                    null
                } else {
                    NotifActionData(
                        key = actionKey(sbn.key, index),
                        title = title
                    )
                }
            }
        )
    }

    private fun StatusBarNotification.isSystemHiddenNotification(): Boolean {
        val ranking = Ranking()
        val hasRanking = runCatching {
            currentRanking.getRanking(key, ranking)
        }.getOrDefault(false)
        if (!hasRanking) return false

        return runCatching {
            val blockedByImportance = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                ranking.importance == NotificationManager.IMPORTANCE_NONE
            val blockedByChannel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                ranking.channel?.importance == NotificationManager.IMPORTANCE_NONE
            val suspendedBySystem = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                ranking.isSuspended
            blockedByImportance || blockedByChannel || suspendedBySystem
        }.getOrDefault(false)
    }

    private fun isNoisyOngoingNotification(
        sbn: StatusBarNotification,
        appLabel: String,
        title: String,
        text: String
    ): Boolean {
        val flags = sbn.notification.flags
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 ||
            flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        if (!ongoing) return false
        val haystack = listOf(appLabel, title, text, sbn.packageName)
            .joinToString(" ")
            .lowercase()
        return listOf(
            "正在运行",
            "正在其他应用上层显示",
            "正在显示在其他应用上层",
            "running",
            "displaying over other apps",
            "shown over other apps"
        ).any { token -> token in haystack }
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
        return runCatching {
            readMessagingPersons(notification.extras)
                .mapNotNull { it.icon?.toImageBitmap() }
                .firstOrNull()
        }.onFailure { error ->
            Log.w(TAG, "Failed to resolve messaging avatar for ${sbn.packageName}", error)
        }.getOrNull()
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
