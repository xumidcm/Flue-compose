package com.flue.launcher.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.data.repository.AppRepository
import com.flue.launcher.iconpack.IconPackDescriptor
import com.flue.launcher.iconpack.IconPackScanner
import com.flue.launcher.ui.navigation.LayoutMode
import com.flue.launcher.ui.navigation.ScreenState
import com.flue.launcher.ui.notification.NotificationEntryUi
import com.flue.launcher.ui.notification.NotificationGroupUi
import com.flue.launcher.ui.notification.NotificationRevealTarget
import com.flue.launcher.service.WLauncherNotificationListener
import com.flue.launcher.watchface.BUILT_IN_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_VIDEO_WATCHFACE_ID
import com.flue.launcher.watchface.WatchClockPosition
import com.flue.launcher.watchface.WatchClockColorMode
import com.flue.launcher.watchface.InternalWatchFaceStorage
import com.flue.launcher.watchface.LunchWatchFaceDescriptor
import com.flue.launcher.watchface.LunchWatchFaceRegistry
import com.flue.launcher.watchface.LunchWatchFaceScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        val KEY_SIDE_SCREEN_ENABLED = booleanPreferencesKey("side_screen_enabled")
        val KEY_SIDE_SCREEN_SHORTCUTS = stringPreferencesKey("side_screen_shortcuts")
        val KEY_BLUR = booleanPreferencesKey("blur_enabled")
        val KEY_EDGE_BLUR = booleanPreferencesKey("edge_blur_enabled")
        val KEY_LOW_RES = booleanPreferencesKey("low_res_icons")
        val KEY_LEGACY_CIRCULAR_ICONS = booleanPreferencesKey("legacy_circular_icons")
        val KEY_ANIMATION_OVERRIDE = booleanPreferencesKey("animation_override_enabled")
        val KEY_SPLASH_ICON = booleanPreferencesKey("splash_icon")
        val KEY_SPLASH_DELAY = intPreferencesKey("splash_delay")
        val KEY_APP_ORDER = stringPreferencesKey("app_order")
        val KEY_HONEYCOMB_COLS = intPreferencesKey("honeycomb_cols")
        val KEY_HONEYCOMB_TOP_BLUR = intPreferencesKey("honeycomb_top_blur")
        val KEY_HONEYCOMB_BOTTOM_BLUR = intPreferencesKey("honeycomb_bottom_blur")
        val KEY_HONEYCOMB_TOP_FADE = intPreferencesKey("honeycomb_top_fade")
        val KEY_HONEYCOMB_BOTTOM_FADE = intPreferencesKey("honeycomb_bottom_fade")
        val KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION = booleanPreferencesKey("honeycomb_fast_scroll_optimization")
        val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val KEY_NOTIFICATION_SETTING_MIGRATED = booleanPreferencesKey("notification_setting_migrated")
        val KEY_HIDDEN_APPS = stringPreferencesKey("hidden_apps")
        val KEY_ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
        val KEY_SELECTED_WATCHFACE_ID = stringPreferencesKey("selected_watchface_id")
        val KEY_LAST_WATCHFACE_ERROR = stringPreferencesKey("last_watchface_error")
        val KEY_BUILTIN_PHOTO_PATH = stringPreferencesKey("builtin_photo_path")
        val KEY_BUILTIN_VIDEO_PATH = stringPreferencesKey("builtin_video_path")
        val KEY_PHOTO_CLOCK_POSITION = stringPreferencesKey("photo_clock_position")
        val KEY_VIDEO_CLOCK_POSITION = stringPreferencesKey("video_clock_position")
        val KEY_PHOTO_CLOCK_SIZE = intPreferencesKey("photo_clock_size")
        val KEY_VIDEO_CLOCK_SIZE = intPreferencesKey("video_clock_size")
        val KEY_PHOTO_CLOCK_BOLD = booleanPreferencesKey("photo_clock_bold")
        val KEY_VIDEO_CLOCK_BOLD = booleanPreferencesKey("video_clock_bold")
        val KEY_VIDEO_FILL_SCREEN = booleanPreferencesKey("video_fill_screen")
        val KEY_VIDEO_CLOCK_COLOR_MODE = stringPreferencesKey("video_clock_color_mode")
        val KEY_BUILTIN_MANAGER_THUMBNAILS = booleanPreferencesKey("builtin_manager_thumbnails")
        val KEY_HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
        private const val SIDE_SCREEN_SLOT_COUNT = 9
    }

    private val store = application.dataStore
    private val appRepository = AppRepository(application)

    val allApps: StateFlow<List<AppInfo>> = appRepository.allApps
    val apps: StateFlow<List<AppInfo>> = appRepository.apps
    private val _screenState = MutableStateFlow(ScreenState.Face)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.Honeycomb)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()
    private val _sideScreenEnabled = MutableStateFlow(true)
    val sideScreenEnabled: StateFlow<Boolean> = _sideScreenEnabled.asStateFlow()
    private val _sideScreenShortcuts = MutableStateFlow(List(SIDE_SCREEN_SLOT_COUNT) { null as String? })
    val sideScreenShortcuts: StateFlow<List<String?>> = _sideScreenShortcuts.asStateFlow()

    private val _blurEnabled = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _edgeBlurEnabled = MutableStateFlow(false)
    val edgeBlurEnabled: StateFlow<Boolean> = _edgeBlurEnabled.asStateFlow()

    private val _lowResIcons = MutableStateFlow(false)
    val lowResIcons: StateFlow<Boolean> = _lowResIcons.asStateFlow()

    private val _animationOverrideEnabled = MutableStateFlow(true)
    val animationOverrideEnabled: StateFlow<Boolean> = _animationOverrideEnabled.asStateFlow()

    private val _splashDelay = MutableStateFlow(500)
    val splashDelay: StateFlow<Int> = _splashDelay.asStateFlow()

    private val _appOrder = MutableStateFlow<List<String>>(emptyList())
    val appOrder: StateFlow<List<String>> = _appOrder.asStateFlow()

    private val _honeycombCols = MutableStateFlow(3)
    val honeycombCols: StateFlow<Int> = _honeycombCols.asStateFlow()
    private val _legacyCircularIcons = MutableStateFlow(true)
    val legacyCircularIcons: StateFlow<Boolean> = _legacyCircularIcons.asStateFlow()

    private val _honeycombTopBlur = MutableStateFlow(4)
    val honeycombTopBlur: StateFlow<Int> = _honeycombTopBlur.asStateFlow()

    private val _honeycombBottomBlur = MutableStateFlow(4)
    val honeycombBottomBlur: StateFlow<Int> = _honeycombBottomBlur.asStateFlow()

    private val _honeycombTopFade = MutableStateFlow(56)
    val honeycombTopFade: StateFlow<Int> = _honeycombTopFade.asStateFlow()

    private val _honeycombBottomFade = MutableStateFlow(56)
    val honeycombBottomFade: StateFlow<Int> = _honeycombBottomFade.asStateFlow()

    private val _honeycombFastScrollOptimization = MutableStateFlow(true)
    val honeycombFastScrollOptimization: StateFlow<Boolean> = _honeycombFastScrollOptimization.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification.asStateFlow()
    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> = _notificationAccessGranted.asStateFlow()
    private val _expandedNotificationGroups = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingDismissedNotificationKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _sideScreenPreviewGroups = MutableStateFlow<List<NotificationGroupUi>>(emptyList())
    val sideScreenPreviewGroups: StateFlow<List<NotificationGroupUi>> = _sideScreenPreviewGroups.asStateFlow()
    private val _notificationGroups = MutableStateFlow<List<NotificationGroupUi>>(emptyList())
    val notificationGroups: StateFlow<List<NotificationGroupUi>> = _notificationGroups.asStateFlow()
    private val _revealedNotificationTarget = MutableStateFlow<NotificationRevealTarget?>(null)
    val revealedNotificationTarget: StateFlow<NotificationRevealTarget?> = _revealedNotificationTarget.asStateFlow()

    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _availableIconPacks = MutableStateFlow<List<IconPackDescriptor>>(emptyList())
    val availableIconPacks: StateFlow<List<IconPackDescriptor>> = _availableIconPacks.asStateFlow()

    private val _selectedIconPackPackage = MutableStateFlow<String?>(null)
    val selectedIconPackPackage: StateFlow<String?> = _selectedIconPackPackage.asStateFlow()

    private val _availableWatchFaces = MutableStateFlow(LunchWatchFaceScanner.builtInDescriptors())
    val availableWatchFaces: StateFlow<List<LunchWatchFaceDescriptor>> = _availableWatchFaces.asStateFlow()

    private val _selectedWatchFaceId = MutableStateFlow(BUILT_IN_WATCHFACE_ID)
    val selectedWatchFaceId: StateFlow<String> = _selectedWatchFaceId.asStateFlow()

    private val _selectedWatchFace = MutableStateFlow(LunchWatchFaceScanner.builtInDescriptor(BUILT_IN_WATCHFACE_ID))
    val selectedWatchFace: StateFlow<LunchWatchFaceDescriptor> = _selectedWatchFace.asStateFlow()

    private val _watchFaceSelectionReady = MutableStateFlow(false)
    val watchFaceSelectionReady: StateFlow<Boolean> = _watchFaceSelectionReady.asStateFlow()

    private val _watchFaceRefreshToken = MutableStateFlow(0)
    val watchFaceRefreshToken: StateFlow<Int> = _watchFaceRefreshToken.asStateFlow()

    private val _watchFaceLastError = MutableStateFlow<String?>(null)
    val watchFaceLastError: StateFlow<String?> = _watchFaceLastError.asStateFlow()

    private val _builtInPhotoPath = MutableStateFlow<String?>(null)
    val builtInPhotoPath: StateFlow<String?> = _builtInPhotoPath.asStateFlow()

    private val _builtInVideoPath = MutableStateFlow<String?>(null)
    val builtInVideoPath: StateFlow<String?> = _builtInVideoPath.asStateFlow()

    private val _builtInPhotoClockPosition = MutableStateFlow(WatchClockPosition.CENTER)
    val builtInPhotoClockPosition: StateFlow<WatchClockPosition> = _builtInPhotoClockPosition.asStateFlow()

    private val _builtInVideoClockPosition = MutableStateFlow(WatchClockPosition.CENTER)
    val builtInVideoClockPosition: StateFlow<WatchClockPosition> = _builtInVideoClockPosition.asStateFlow()

    private val _builtInPhotoClockSize = MutableStateFlow(64)
    val builtInPhotoClockSize: StateFlow<Int> = _builtInPhotoClockSize.asStateFlow()

    private val _builtInVideoClockSize = MutableStateFlow(64)
    val builtInVideoClockSize: StateFlow<Int> = _builtInVideoClockSize.asStateFlow()

    private val _builtInPhotoClockBold = MutableStateFlow(false)
    val builtInPhotoClockBold: StateFlow<Boolean> = _builtInPhotoClockBold.asStateFlow()

    private val _builtInVideoClockBold = MutableStateFlow(false)
    val builtInVideoClockBold: StateFlow<Boolean> = _builtInVideoClockBold.asStateFlow()

    private val _builtInVideoFillScreen = MutableStateFlow(true)
    val builtInVideoFillScreen: StateFlow<Boolean> = _builtInVideoFillScreen.asStateFlow()
    private val _builtInVideoClockColorMode = MutableStateFlow(WatchClockColorMode.AUTO)
    val builtInVideoClockColorMode: StateFlow<WatchClockColorMode> = _builtInVideoClockColorMode.asStateFlow()

    private val _builtInManagerThumbnails = MutableStateFlow(true)
    val builtInManagerThumbnails: StateFlow<Boolean> = _builtInManagerThumbnails.asStateFlow()
    private val _hideFromRecents = MutableStateFlow(true)
    val hideFromRecents: StateFlow<Boolean> = _hideFromRecents.asStateFlow()

    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()
    private val _currentLaunchIcon = MutableStateFlow<ImageBitmap?>(null)
    val currentLaunchIcon: StateFlow<ImageBitmap?> = _currentLaunchIcon.asStateFlow()
    private val _launchSourceState = MutableStateFlow(ScreenState.Apps)
    val launchSourceState: StateFlow<ScreenState> = _launchSourceState.asStateFlow()

    private var launchingExternalApp = false
    private var returnStateAfterExternalLaunch = ScreenState.Apps
    private var launchJob: Job? = null
    private var watchFacePrefsHydrated = false
    private var watchFaceScanHydrated = false
    private var lastWatchFaceRefreshAt = 0L
    private val pendingWriteJobs = ConcurrentHashMap<String, Job>()
    private var pendingNotificationClearJob: Job? = null
    private var refreshIconsJob: Job? = null
    init {
        refreshNotificationAccess()
        viewModelScope.launch {
            combine(
                WLauncherNotificationListener.notifications,
                _expandedNotificationGroups,
                _pendingDismissedNotificationKeys
            ) { notifications, expandedPackages, pendingDismissedKeys ->
                val filteredNotifications = notifications.filterNot { it.key in pendingDismissedKeys }
                val activeNotificationKeys = notifications.asSequence().map { it.key }.toSet()
                Triple(
                    buildNotificationGroups(filteredNotifications, emptySet()),
                    buildNotificationGroups(filteredNotifications, expandedPackages),
                    pendingDismissedKeys.intersect(activeNotificationKeys)
                )
            }.collect { (previewGroups, groups, remainingPendingDismissedKeys) ->
                updateStableNotificationGroups(previewGroups, groups)
                if (_pendingDismissedNotificationKeys.value != remainingPendingDismissedKeys) {
                    _pendingDismissedNotificationKeys.value = remainingPendingDismissedKeys
                }
                _expandedNotificationGroups.value =
                    _expandedNotificationGroups.value.intersect(groups.map(NotificationGroupUi::packageName).toSet())
                val currentRevealTarget = _revealedNotificationTarget.value
                val validRevealTarget = when (currentRevealTarget) {
                    is NotificationRevealTarget.Group -> groups.any { it.packageName == currentRevealTarget.packageName }
                    is NotificationRevealTarget.Entry -> groups.any { group -> group.entries.any { it.key == currentRevealTarget.key } }
                    null -> true
                }
                if (!validRevealTarget) {
                    _revealedNotificationTarget.value = null
                }
            }
        }
        refreshIconPacks()
        viewModelScope.launch(Dispatchers.IO) {
            store.data.collect { prefs ->
                val loadedLayout = prefs[KEY_LAYOUT]?.let {
                    try {
                        LayoutMode.valueOf(it)
                    } catch (_: Exception) {
                        LayoutMode.Honeycomb
                    }
                } ?: LayoutMode.Honeycomb
                if (_layoutMode.value != loadedLayout) _layoutMode.value = loadedLayout

                val loadedSideScreenEnabled = prefs[KEY_SIDE_SCREEN_ENABLED] ?: true
                if (_sideScreenEnabled.value != loadedSideScreenEnabled) _sideScreenEnabled.value = loadedSideScreenEnabled

                val loadedSideScreenShortcuts = parseSideScreenShortcuts(prefs[KEY_SIDE_SCREEN_SHORTCUTS])
                if (_sideScreenShortcuts.value != loadedSideScreenShortcuts) {
                    _sideScreenShortcuts.value = loadedSideScreenShortcuts
                }

                val loadedBlur = prefs[KEY_BLUR] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                if (_blurEnabled.value != loadedBlur) _blurEnabled.value = loadedBlur

                val loadedEdgeBlur = prefs[KEY_EDGE_BLUR] ?: false
                if (_edgeBlurEnabled.value != loadedEdgeBlur) _edgeBlurEnabled.value = loadedEdgeBlur

                val loadedLowRes = prefs[KEY_LOW_RES] ?: false
                if (_lowResIcons.value != loadedLowRes) {
                    _lowResIcons.value = loadedLowRes
                    refreshIcons()
                }

                val loadedLegacyCircularIcons = true
                if (_legacyCircularIcons.value != loadedLegacyCircularIcons) {
                    _legacyCircularIcons.value = loadedLegacyCircularIcons
                    appRepository.setLegacyCircularIconsEnabled(loadedLegacyCircularIcons)
                }
                if (prefs[KEY_LEGACY_CIRCULAR_ICONS] != true) {
                    store.edit { it[KEY_LEGACY_CIRCULAR_ICONS] = true }
                }

                val loadedAnimationOverride = prefs[KEY_ANIMATION_OVERRIDE] ?: true
                if (_animationOverrideEnabled.value != loadedAnimationOverride) {
                    _animationOverrideEnabled.value = loadedAnimationOverride
                }

                val loadedSplashIcon = prefs[KEY_SPLASH_ICON] ?: true
                if (_splashIcon.value != loadedSplashIcon) _splashIcon.value = loadedSplashIcon

                val loadedSplashDelay = (prefs[KEY_SPLASH_DELAY] ?: 500).coerceIn(300, 1500)
                if (_splashDelay.value != loadedSplashDelay) _splashDelay.value = loadedSplashDelay

                val loadedOrder = prefs[KEY_APP_ORDER]
                    ?.takeIf { it.isNotEmpty() }
                    ?.split(",")
                    ?: emptyList()
                if (_appOrder.value != loadedOrder) {
                    _appOrder.value = loadedOrder
                    appRepository.setCustomOrder(loadedOrder)
                }

                val loadedHoneycombCols = (prefs[KEY_HONEYCOMB_COLS] ?: 3).coerceIn(3, 6)
                if (_honeycombCols.value != loadedHoneycombCols) _honeycombCols.value = loadedHoneycombCols

                val loadedTopBlur = (prefs[KEY_HONEYCOMB_TOP_BLUR] ?: 4).coerceIn(0, 48)
                if (_honeycombTopBlur.value != loadedTopBlur) _honeycombTopBlur.value = loadedTopBlur

                val loadedBottomBlur = (prefs[KEY_HONEYCOMB_BOTTOM_BLUR] ?: 4).coerceIn(0, 48)
                if (_honeycombBottomBlur.value != loadedBottomBlur) _honeycombBottomBlur.value = loadedBottomBlur

                val loadedTopFade = (prefs[KEY_HONEYCOMB_TOP_FADE] ?: 56).coerceIn(0, 160)
                if (_honeycombTopFade.value != loadedTopFade) _honeycombTopFade.value = loadedTopFade

                val loadedBottomFade = (prefs[KEY_HONEYCOMB_BOTTOM_FADE] ?: 56).coerceIn(0, 160)
                if (_honeycombBottomFade.value != loadedBottomFade) _honeycombBottomFade.value = loadedBottomFade

                val loadedFastScrollOptimization = prefs[KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION] ?: true
                if (_honeycombFastScrollOptimization.value != loadedFastScrollOptimization) {
                    _honeycombFastScrollOptimization.value = loadedFastScrollOptimization
                }

                val notificationSettingMigrated = prefs[KEY_NOTIFICATION_SETTING_MIGRATED] ?: false
                val loadedShowNotification = if (notificationSettingMigrated) {
                    prefs[KEY_SHOW_NOTIFICATION] ?: true
                } else {
                    true
                }
                if (_showNotification.value != loadedShowNotification) _showNotification.value = loadedShowNotification
                if (!notificationSettingMigrated) {
                    store.edit {
                        it[KEY_SHOW_NOTIFICATION] = true
                        it[KEY_NOTIFICATION_SETTING_MIGRATED] = true
                    }
                }

                val hidden = prefs[KEY_HIDDEN_APPS]
                    ?.split(",")
                    ?.filter(String::isNotBlank)
                    ?.toSet()
                    ?: emptySet()
                if (_hiddenApps.value != hidden) {
                    _hiddenApps.value = hidden
                    appRepository.setHiddenComponents(hidden)
                }

                val selectedPack = prefs[KEY_ICON_PACK_PACKAGE]
                if (_selectedIconPackPackage.value != selectedPack) {
                    _selectedIconPackPackage.value = selectedPack
                    appRepository.setIconPackPackage(selectedPack)
                }

                val loadedWatchFaceId = prefs[KEY_SELECTED_WATCHFACE_ID] ?: BUILT_IN_WATCHFACE_ID
                if (_selectedWatchFaceId.value != loadedWatchFaceId) _selectedWatchFaceId.value = loadedWatchFaceId

                val loadedWatchFaceError = prefs[KEY_LAST_WATCHFACE_ERROR]
                if (_watchFaceLastError.value != loadedWatchFaceError) _watchFaceLastError.value = loadedWatchFaceError

                val loadedPhotoPath = prefs[KEY_BUILTIN_PHOTO_PATH]
                if (_builtInPhotoPath.value != loadedPhotoPath) _builtInPhotoPath.value = loadedPhotoPath

                val loadedVideoPath = prefs[KEY_BUILTIN_VIDEO_PATH]
                if (_builtInVideoPath.value != loadedVideoPath) _builtInVideoPath.value = loadedVideoPath

                val loadedPhotoClockPosition = prefs[KEY_PHOTO_CLOCK_POSITION]
                    ?.let(::parseClockPosition)
                    ?: WatchClockPosition.CENTER
                if (_builtInPhotoClockPosition.value != loadedPhotoClockPosition) {
                    _builtInPhotoClockPosition.value = loadedPhotoClockPosition
                }

                val loadedVideoClockPosition = prefs[KEY_VIDEO_CLOCK_POSITION]
                    ?.let(::parseClockPosition)
                    ?: WatchClockPosition.CENTER
                if (_builtInVideoClockPosition.value != loadedVideoClockPosition) {
                    _builtInVideoClockPosition.value = loadedVideoClockPosition
                }

                val loadedPhotoClockSize = (prefs[KEY_PHOTO_CLOCK_SIZE] ?: 64).coerceIn(28, 92)
                if (_builtInPhotoClockSize.value != loadedPhotoClockSize) _builtInPhotoClockSize.value = loadedPhotoClockSize

                val loadedVideoClockSize = (prefs[KEY_VIDEO_CLOCK_SIZE] ?: 64).coerceIn(28, 92)
                if (_builtInVideoClockSize.value != loadedVideoClockSize) _builtInVideoClockSize.value = loadedVideoClockSize

                val loadedPhotoClockBold = prefs[KEY_PHOTO_CLOCK_BOLD] ?: false
                if (_builtInPhotoClockBold.value != loadedPhotoClockBold) _builtInPhotoClockBold.value = loadedPhotoClockBold

                val loadedVideoClockBold = prefs[KEY_VIDEO_CLOCK_BOLD] ?: false
                if (_builtInVideoClockBold.value != loadedVideoClockBold) _builtInVideoClockBold.value = loadedVideoClockBold

                val loadedVideoFillScreen = prefs[KEY_VIDEO_FILL_SCREEN] ?: true
                if (_builtInVideoFillScreen.value != loadedVideoFillScreen) _builtInVideoFillScreen.value = loadedVideoFillScreen
                val loadedVideoClockColorMode = prefs[KEY_VIDEO_CLOCK_COLOR_MODE]
                    ?.let(::parseClockColorMode)
                    ?: WatchClockColorMode.AUTO
                if (_builtInVideoClockColorMode.value != loadedVideoClockColorMode) {
                    _builtInVideoClockColorMode.value = loadedVideoClockColorMode
                }

                val loadedManagerThumbnails = prefs[KEY_BUILTIN_MANAGER_THUMBNAILS] ?: true
                if (_builtInManagerThumbnails.value != loadedManagerThumbnails) _builtInManagerThumbnails.value = loadedManagerThumbnails

                val loadedHideFromRecents = prefs[KEY_HIDE_FROM_RECENTS] ?: true
                if (_hideFromRecents.value != loadedHideFromRecents) _hideFromRecents.value = loadedHideFromRecents

                watchFacePrefsHydrated = true
                syncSelectedWatchFace()
            }
        }
        refreshWatchFaces()
    }

    fun setState(state: ScreenState) {
        _screenState.value = state
        _revealedNotificationTarget.value = null
    }

    fun openApp(
        appInfo: AppInfo,
        origin: Offset = Offset(0.5f, 0.5f),
        launchDelayMs: Long = _splashDelay.value.toLong(),
        returnState: ScreenState = ScreenState.Apps
    ) {
        returnStateAfterExternalLaunch = returnState
        _launchSourceState.value = returnState
        _currentApp.value = appInfo
        _currentLaunchIcon.value = appInfo.cachedIcon
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App
        _revealedNotificationTarget.value = null

        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            delay(launchDelayMs)
            val launched = appRepository.launchApp(appInfo)
            if (launched) {
                launchingExternalApp = true
            } else {
                launchingExternalApp = false
                _currentApp.value = null
                _currentLaunchIcon.value = null
                _screenState.value = returnStateAfterExternalLaunch
                Toast.makeText(getApplication(), "应用无法启动，已刷新列表", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openNotification(
        key: String,
        origin: Offset = Offset(0.5f, 0.5f),
        returnState: ScreenState = ScreenState.Stack,
        launchDelayMs: Long = _splashDelay.value.toLong()
    ): Boolean {
        val targetEntry = _notificationGroups.value
            .asSequence()
            .flatMap { it.entries.asSequence() }
            .firstOrNull { it.key == key }
            ?: return false
        returnStateAfterExternalLaunch = returnState
        _launchSourceState.value = returnState
        _currentApp.value = null
        _currentLaunchIcon.value = null
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App
        _revealedNotificationTarget.value = null

        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            delay(launchDelayMs)
            val opened = WLauncherNotificationListener.openNotification(key)
            if (opened) {
                launchingExternalApp = true
            } else {
                launchingExternalApp = false
                _currentLaunchIcon.value = null
                _screenState.value = returnStateAfterExternalLaunch
            }
        }
        return true
    }

    fun onReturnToLauncher() {
        if (launchingExternalApp) {
            launchingExternalApp = false
            _currentLaunchIcon.value = null
            _screenState.value = returnStateAfterExternalLaunch
            _revealedNotificationTarget.value = null
        }
    }

    fun hasPendingExternalLaunchReturn(): Boolean = launchingExternalApp

    fun handleHomePress() {
        when (_screenState.value) {
            ScreenState.Face -> _screenState.value = ScreenState.Apps
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            ScreenState.App -> {
                launchJob?.cancel()
                launchJob = null
                launchingExternalApp = false
                _currentLaunchIcon.value = null
                _screenState.value = returnStateAfterExternalLaunch
                _revealedNotificationTarget.value = null
            }
            else -> _screenState.value = ScreenState.Face
        }
    }

    fun handleBackPress() {
        when (_screenState.value) {
            ScreenState.Face -> Unit
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            ScreenState.App -> {
                launchJob?.cancel()
                launchJob = null
                launchingExternalApp = false
                _currentLaunchIcon.value = null
                _screenState.value = returnStateAfterExternalLaunch
                _revealedNotificationTarget.value = null
            }
            ScreenState.Settings -> _screenState.value = ScreenState.Apps
            ScreenState.Stack -> _screenState.value = ScreenState.Face
            ScreenState.Notifications -> _screenState.value = if (_sideScreenEnabled.value) ScreenState.Stack else ScreenState.Face
            ScreenState.ControlCenter -> _screenState.value = ScreenState.Face
        }
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        persist { store.edit { it[KEY_LAYOUT] = mode.name } }
    }

    fun setSideScreenEnabled(enabled: Boolean) {
        _sideScreenEnabled.value = enabled
        if (!enabled && _screenState.value == ScreenState.Stack) {
            _screenState.value = ScreenState.Face
        }
        persist { store.edit { it[KEY_SIDE_SCREEN_ENABLED] = enabled } }
    }

    fun setBlurEnabled(enabled: Boolean) {
        _blurEnabled.value = enabled
        if (!enabled) _edgeBlurEnabled.value = false
        persist {
            store.edit {
                it[KEY_BLUR] = enabled
                if (!enabled) it[KEY_EDGE_BLUR] = false
            }
        }
    }

    fun setEdgeBlurEnabled(enabled: Boolean) {
        val value = enabled && _blurEnabled.value
        _edgeBlurEnabled.value = value
        persist { store.edit { it[KEY_EDGE_BLUR] = value } }
    }

    fun setLowResIcons(enabled: Boolean) {
        _lowResIcons.value = enabled
        refreshIcons()
        persist { store.edit { it[KEY_LOW_RES] = enabled } }
    }

    fun setLegacyCircularIconsEnabled(enabled: Boolean) {
        _legacyCircularIcons.value = enabled
        appRepository.setLegacyCircularIconsEnabled(enabled)
        persist { store.edit { it[KEY_LEGACY_CIRCULAR_ICONS] = enabled } }
    }

    fun setAnimationOverrideEnabled(enabled: Boolean) {
        _animationOverrideEnabled.value = enabled
        persist { store.edit { it[KEY_ANIMATION_OVERRIDE] = enabled } }
    }

    fun setSplashIcon(enabled: Boolean) {
        _splashIcon.value = enabled
        persist { store.edit { it[KEY_SPLASH_ICON] = enabled } }
    }

    fun setSplashDelay(ms: Int) {
        _splashDelay.value = ms.coerceIn(300, 1500)
        val value = _splashDelay.value
        persistDebounced("splash_delay") { store.edit { it[KEY_SPLASH_DELAY] = value } }
    }

    fun setAppOrder(order: List<String>) {
        _appOrder.value = order
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.setCustomOrder(order)
        }
        persist { store.edit { it[KEY_APP_ORDER] = order.joinToString(",") } }
    }

    fun setHoneycombCols(cols: Int) {
        _honeycombCols.value = cols.coerceIn(3, 6)
        val savedValue = _honeycombCols.value
        persistDebounced("key_honeycomb_cols") { store.edit { it[KEY_HONEYCOMB_COLS] = savedValue } }
    }

    fun setHoneycombTopBlur(value: Int) {
        _honeycombTopBlur.value = value.coerceIn(0, 48)
        val savedValue = _honeycombTopBlur.value
        persistDebounced("key_honeycomb_top_blur") { store.edit { it[KEY_HONEYCOMB_TOP_BLUR] = savedValue } }
    }

    fun setHoneycombBottomBlur(value: Int) {
        _honeycombBottomBlur.value = value.coerceIn(0, 48)
        val savedValue = _honeycombBottomBlur.value
        persistDebounced("key_honeycomb_bottom_blur") { store.edit { it[KEY_HONEYCOMB_BOTTOM_BLUR] = savedValue } }
    }

    fun setHoneycombTopFade(value: Int) {
        _honeycombTopFade.value = value.coerceIn(0, 160)
        val savedValue = _honeycombTopFade.value
        persistDebounced("key_honeycomb_top_fade") { store.edit { it[KEY_HONEYCOMB_TOP_FADE] = savedValue } }
    }

    fun setHoneycombBottomFade(value: Int) {
        _honeycombBottomFade.value = value.coerceIn(0, 160)
        val savedValue = _honeycombBottomFade.value
        persistDebounced("key_honeycomb_bottom_fade") { store.edit { it[KEY_HONEYCOMB_BOTTOM_FADE] = savedValue } }
    }

    fun setHoneycombFastScrollOptimization(enabled: Boolean) {
        _honeycombFastScrollOptimization.value = enabled
        persist { store.edit { it[KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION] = enabled } }
    }

    fun setShowNotification(show: Boolean) {
        if (!show && _screenState.value == ScreenState.Notifications) {
            _screenState.value = if (_sideScreenEnabled.value) ScreenState.Stack else ScreenState.Face
            _revealedNotificationTarget.value = null
        }
        _showNotification.value = show
        persist {
            store.edit {
                it[KEY_SHOW_NOTIFICATION] = show
                it[KEY_NOTIFICATION_SETTING_MIGRATED] = true
            }
        }
    }

    fun setAppHidden(componentKey: String, hidden: Boolean) {
        val next = _hiddenApps.value.toMutableSet().apply {
            if (hidden) add(componentKey) else remove(componentKey)
        }
        setHiddenApps(next)
    }

    fun setHiddenApps(components: Set<String>) {
        val next = components.filter(String::isNotBlank).toSet()
        _hiddenApps.value = next
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.setHiddenComponents(next)
        }
        persist {
            store.edit {
                if (next.isEmpty()) it.remove(KEY_HIDDEN_APPS)
                else it[KEY_HIDDEN_APPS] = next.joinToString(",")
            }
        }
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            val packs = withContext(Dispatchers.IO) {
                IconPackScanner.scanInstalled(getApplication())
            }
            _availableIconPacks.value = packs
        }
    }

    fun setIconPackPackage(packageName: String?) {
        _selectedIconPackPackage.value = packageName?.takeIf { it.isNotBlank() }
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.setIconPackPackage(_selectedIconPackPackage.value)
        }
        persist {
            store.edit {
                if (_selectedIconPackPackage.value.isNullOrBlank()) {
                    it.remove(KEY_ICON_PACK_PACKAGE)
                } else {
                    it[KEY_ICON_PACK_PACKAGE] = _selectedIconPackPackage.value!!
                }
            }
        }
    }

    fun refreshWatchFaces(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastWatchFaceRefreshAt < 25_000L) return
        lastWatchFaceRefreshAt = now
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                LunchWatchFaceScanner.builtInDescriptors() + LunchWatchFaceScanner.scanInstalled(getApplication())
            }
            _availableWatchFaces.value = scanned
            LunchWatchFaceRegistry.update(scanned)
            watchFaceScanHydrated = true
            syncSelectedWatchFace()
        }
    }

    fun selectWatchFace(id: String) {
        _selectedWatchFaceId.value = id
        syncSelectedWatchFace()
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        persist { store.edit { it[KEY_SELECTED_WATCHFACE_ID] = id } }
    }

    fun fallbackToBuiltIn(error: String? = null) {
        _selectedWatchFaceId.value = BUILT_IN_WATCHFACE_ID
        _watchFaceLastError.value = error
        syncSelectedWatchFace()
        persist {
            store.edit {
                it[KEY_SELECTED_WATCHFACE_ID] = BUILT_IN_WATCHFACE_ID
                if (error.isNullOrBlank()) {
                    it.remove(KEY_LAST_WATCHFACE_ERROR)
                } else {
                    it[KEY_LAST_WATCHFACE_ERROR] = error
                }
            }
        }
    }

    fun clearWatchFaceError() {
        _watchFaceLastError.value = null
        persist { store.edit { it.remove(KEY_LAST_WATCHFACE_ERROR) } }
    }

    fun setBuiltInPhotoPath(path: String?) {
        _builtInPhotoPath.value = path
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        if (path.isNullOrBlank()) InternalWatchFaceStorage.clearPhoto(getApplication())
        persist {
            store.edit {
                if (path.isNullOrBlank()) it.remove(KEY_BUILTIN_PHOTO_PATH)
                else it[KEY_BUILTIN_PHOTO_PATH] = path
            }
        }
    }

    fun setBuiltInVideoPath(path: String?) {
        _builtInVideoPath.value = path
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        if (path.isNullOrBlank()) InternalWatchFaceStorage.clearVideo(getApplication())
        persist {
            store.edit {
                if (path.isNullOrBlank()) it.remove(KEY_BUILTIN_VIDEO_PATH)
                else it[KEY_BUILTIN_VIDEO_PATH] = path
            }
        }
    }

    fun setBuiltInPhotoClockPosition(position: WatchClockPosition) {
        _builtInPhotoClockPosition.value = position
        persist { store.edit { it[KEY_PHOTO_CLOCK_POSITION] = position.name } }
    }

    fun setBuiltInVideoClockPosition(position: WatchClockPosition) {
        _builtInVideoClockPosition.value = position
        persist { store.edit { it[KEY_VIDEO_CLOCK_POSITION] = position.name } }
    }

    fun setBuiltInPhotoClockSize(sizeSp: Int) {
        _builtInPhotoClockSize.value = sizeSp.coerceIn(28, 92)
        val value = _builtInPhotoClockSize.value
        persistDebounced("photo_clock_size") { store.edit { it[KEY_PHOTO_CLOCK_SIZE] = value } }
    }

    fun setBuiltInVideoClockSize(sizeSp: Int) {
        _builtInVideoClockSize.value = sizeSp.coerceIn(28, 92)
        val value = _builtInVideoClockSize.value
        persistDebounced("video_clock_size") { store.edit { it[KEY_VIDEO_CLOCK_SIZE] = value } }
    }

    fun setBuiltInPhotoClockBold(enabled: Boolean) {
        _builtInPhotoClockBold.value = enabled
        persist { store.edit { it[KEY_PHOTO_CLOCK_BOLD] = enabled } }
    }

    fun setBuiltInVideoClockBold(enabled: Boolean) {
        _builtInVideoClockBold.value = enabled
        persist { store.edit { it[KEY_VIDEO_CLOCK_BOLD] = enabled } }
    }

    fun setBuiltInVideoFillScreen(fillScreen: Boolean) {
        _builtInVideoFillScreen.value = fillScreen
        persist { store.edit { it[KEY_VIDEO_FILL_SCREEN] = fillScreen } }
    }

    fun setBuiltInVideoClockColorMode(mode: WatchClockColorMode) {
        _builtInVideoClockColorMode.value = mode
        persist { store.edit { it[KEY_VIDEO_CLOCK_COLOR_MODE] = mode.name } }
    }

    fun setBuiltInManagerThumbnails(enabled: Boolean) {
        _builtInManagerThumbnails.value = enabled
        persist { store.edit { it[KEY_BUILTIN_MANAGER_THUMBNAILS] = enabled } }
    }

    fun setHideFromRecents(enabled: Boolean) {
        _hideFromRecents.value = enabled
        persist { store.edit { it[KEY_HIDE_FROM_RECENTS] = enabled } }
    }

    fun requestWatchFaceRefresh() {
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
    }

    fun setSideScreenShortcut(slotIndex: Int, componentKey: String?) {
        if (slotIndex !in 0 until SIDE_SCREEN_SLOT_COUNT) return
        val next = _sideScreenShortcuts.value.toMutableList().apply {
            this[slotIndex] = componentKey?.takeIf { it.isNotBlank() }
        }
        _sideScreenShortcuts.value = next
        persist {
            store.edit { prefs ->
                val serialized = serializeSideScreenShortcuts(next)
                if (serialized.isEmpty()) {
                    prefs.remove(KEY_SIDE_SCREEN_SHORTCUTS)
                } else {
                    prefs[KEY_SIDE_SCREEN_SHORTCUTS] = serialized
                }
            }
        }
    }

    fun removeSideScreenShortcut(slotIndex: Int) {
        setSideScreenShortcut(slotIndex, null)
    }

    fun toggleNotificationGroup(packageName: String) {
        if (packageName.isBlank()) return
        _expandedNotificationGroups.value = _expandedNotificationGroups.value.toMutableSet().apply {
            if (!add(packageName)) remove(packageName)
        }
        _revealedNotificationTarget.value = null
    }

    fun dismissNotificationGroup(packageName: String) {
        val keys = _notificationGroups.value
            .firstOrNull { it.packageName == packageName }
            ?.entries
            ?.filter(NotificationEntryUi::isClearable)
            ?.map(NotificationEntryUi::key)
            .orEmpty()
        if (keys.isNotEmpty()) {
            markNotificationsDismissed(keys)
            WLauncherNotificationListener.dismissNotifications(keys)
        }
        _expandedNotificationGroups.value = _expandedNotificationGroups.value - packageName
        _revealedNotificationTarget.value = null
    }

    fun dismissNotification(key: String) {
        if (key.isBlank()) return
        markNotificationsDismissed(listOf(key))
        WLauncherNotificationListener.dismissNotification(key)
        _revealedNotificationTarget.value = null
    }

    fun setRevealedNotificationTarget(target: NotificationRevealTarget?) {
        _revealedNotificationTarget.value = target
    }

    fun refreshNotificationAccess() {
        val component = ComponentName(getApplication(), WLauncherNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            "enabled_notification_listeners"
        )
        val granted = enabledListeners
            ?.split(':')
            ?.mapNotNull { ComponentName.unflattenFromString(it) }
            ?.any { it.packageName == component.packageName && it.className == component.className }
            ?: false
        _notificationAccessGranted.value = granted || WLauncherNotificationListener.isConnected()
    }

    private fun markNotificationsDismissed(keys: Collection<String>) {
        if (keys.isEmpty()) return
        _pendingDismissedNotificationKeys.value = _pendingDismissedNotificationKeys.value + keys
    }

    fun swapApps(fromIndex: Int, toIndex: Int) {
        val current = apps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setAppOrder(current.map { it.componentKey })
        }
    }

    fun resetSettings() {
        val defaultBlurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        _layoutMode.value = LayoutMode.Honeycomb
        _sideScreenEnabled.value = true
        _sideScreenShortcuts.value = List(SIDE_SCREEN_SLOT_COUNT) { null }
        _blurEnabled.value = defaultBlurEnabled
        _edgeBlurEnabled.value = false
        _lowResIcons.value = false
        _animationOverrideEnabled.value = true
        _splashIcon.value = true
        _splashDelay.value = 500
        _honeycombCols.value = 3
        _legacyCircularIcons.value = true
        _honeycombTopBlur.value = 4
        _honeycombBottomBlur.value = 4
        _honeycombTopFade.value = 56
        _honeycombBottomFade.value = 56
        _honeycombFastScrollOptimization.value = true
        _showNotification.value = true
        _hiddenApps.value = emptySet()
        _selectedIconPackPackage.value = null
        _selectedWatchFaceId.value = BUILT_IN_WATCHFACE_ID
        _selectedWatchFace.value = LunchWatchFaceScanner.builtInDescriptor(BUILT_IN_WATCHFACE_ID)
        _watchFaceLastError.value = null
        _builtInPhotoClockPosition.value = WatchClockPosition.CENTER
        _builtInVideoClockPosition.value = WatchClockPosition.CENTER
        _builtInPhotoClockSize.value = 64
        _builtInVideoClockSize.value = 64
        _builtInPhotoClockBold.value = false
        _builtInVideoClockBold.value = false
        _builtInVideoFillScreen.value = true
        _builtInVideoClockColorMode.value = WatchClockColorMode.AUTO
        _builtInManagerThumbnails.value = true
        _hideFromRecents.value = true
        appRepository.setHiddenComponents(emptySet())
        appRepository.setIconPackPackage(null)
        appRepository.setLegacyCircularIconsEnabled(true)
        LunchWatchFaceRegistry.setCurrentSelectedId(BUILT_IN_WATCHFACE_ID)
        refreshIcons()
        persist {
            store.edit {
                it[KEY_LAYOUT] = LayoutMode.Honeycomb.name
                it[KEY_SIDE_SCREEN_ENABLED] = true
                it[KEY_BLUR] = defaultBlurEnabled
                it[KEY_EDGE_BLUR] = false
                it[KEY_LOW_RES] = false
                it[KEY_ANIMATION_OVERRIDE] = true
                it[KEY_SPLASH_ICON] = true
                it[KEY_SPLASH_DELAY] = 500
                it[KEY_HONEYCOMB_COLS] = 3
                it[KEY_HONEYCOMB_TOP_BLUR] = 4
                it[KEY_HONEYCOMB_BOTTOM_BLUR] = 4
                it[KEY_HONEYCOMB_TOP_FADE] = 56
                it[KEY_HONEYCOMB_BOTTOM_FADE] = 56
                it[KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION] = true
                it[KEY_LEGACY_CIRCULAR_ICONS] = true
                it[KEY_SHOW_NOTIFICATION] = true
                it[KEY_NOTIFICATION_SETTING_MIGRATED] = true
                it.remove(KEY_SIDE_SCREEN_SHORTCUTS)
                it.remove(KEY_HIDDEN_APPS)
                it.remove(KEY_ICON_PACK_PACKAGE)
                it[KEY_SELECTED_WATCHFACE_ID] = BUILT_IN_WATCHFACE_ID
                it[KEY_PHOTO_CLOCK_POSITION] = WatchClockPosition.CENTER.name
                it[KEY_VIDEO_CLOCK_POSITION] = WatchClockPosition.CENTER.name
                it[KEY_PHOTO_CLOCK_SIZE] = 64
                it[KEY_VIDEO_CLOCK_SIZE] = 64
                it[KEY_PHOTO_CLOCK_BOLD] = false
                it[KEY_VIDEO_CLOCK_BOLD] = false
                it[KEY_VIDEO_FILL_SCREEN] = true
                it[KEY_VIDEO_CLOCK_COLOR_MODE] = WatchClockColorMode.AUTO.name
                it[KEY_BUILTIN_MANAGER_THUMBNAILS] = true
                it[KEY_HIDE_FROM_RECENTS] = true
                it.remove(KEY_LAST_WATCHFACE_ERROR)
            }
        }
    }

    private fun parseClockColorMode(raw: String): WatchClockColorMode {
        return runCatching { WatchClockColorMode.valueOf(raw) }.getOrDefault(WatchClockColorMode.AUTO)
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }

    private fun persistDebounced(tag: String, delayMs: Long = 120, block: suspend () -> Unit) {
        pendingWriteJobs.remove(tag)?.cancel()
        pendingWriteJobs[tag] = viewModelScope.launch(Dispatchers.IO) {
            delay(delayMs)
            block()
            pendingWriteJobs.remove(tag)
        }
    }

    private fun parseClockPosition(value: String): WatchClockPosition =
        runCatching { WatchClockPosition.valueOf(value) }.getOrDefault(WatchClockPosition.CENTER)

    private fun syncSelectedWatchFace() {
        val requestedId = _selectedWatchFaceId.value.ifBlank { BUILT_IN_WATCHFACE_ID }
        val available = _availableWatchFaces.value
        val match = available.firstOrNull { it.id == requestedId }

        when {
            match != null -> {
                _selectedWatchFace.value = match
                LunchWatchFaceRegistry.setCurrentSelectedId(match.id)
            }
            watchFaceScanHydrated -> {
                val fallback = available.firstOrNull { it.id == BUILT_IN_WATCHFACE_ID }
                    ?: available.firstOrNull()
                    ?: LunchWatchFaceScanner.builtInDescriptor(BUILT_IN_WATCHFACE_ID)
                _selectedWatchFace.value = fallback
                _selectedWatchFaceId.value = fallback.id
                LunchWatchFaceRegistry.setCurrentSelectedId(fallback.id)
                if (watchFacePrefsHydrated && requestedId != fallback.id) {
                    persist {
                        store.edit { it[KEY_SELECTED_WATCHFACE_ID] = fallback.id }
                    }
                }
            }
            else -> {
                _selectedWatchFace.value = LunchWatchFaceScanner.builtInDescriptor(BUILT_IN_WATCHFACE_ID)
                LunchWatchFaceRegistry.setCurrentSelectedId(requestedId)
            }
        }

        _watchFaceSelectionReady.value = watchFacePrefsHydrated && watchFaceScanHydrated
    }

    private fun refreshIcons() {
        refreshIconsJob?.cancel()
        refreshIconsJob = viewModelScope.launch(Dispatchers.IO) {
            appRepository.refresh(if (_lowResIcons.value) 64 else 128)
        }
    }

    private fun parseSideScreenShortcuts(raw: String?): List<String?> {
        val parts = raw
            ?.split('|')
            ?.map { it.ifBlank { null } }
            .orEmpty()
        return List(SIDE_SCREEN_SLOT_COUNT) { index -> parts.getOrNull(index) }
    }

    private fun serializeSideScreenShortcuts(shortcuts: List<String?>): String {
        val normalized = List(SIDE_SCREEN_SLOT_COUNT) { index ->
            shortcuts.getOrNull(index)?.takeIf { it.isNotBlank() } ?: ""
        }
        return if (normalized.all(String::isEmpty)) "" else normalized.joinToString("|")
    }

    private fun updateStableNotificationGroups(
        previewGroups: List<NotificationGroupUi>,
        groups: List<NotificationGroupUi>
    ) {
        if (previewGroups.isNotEmpty() || groups.isNotEmpty()) {
            pendingNotificationClearJob?.cancel()
            pendingNotificationClearJob = null
            _sideScreenPreviewGroups.value = previewGroups
            _notificationGroups.value = groups
            return
        }
        if (_sideScreenPreviewGroups.value.isEmpty() && _notificationGroups.value.isEmpty()) {
            return
        }
        pendingNotificationClearJob?.cancel()
        pendingNotificationClearJob = viewModelScope.launch {
            delay(220)
            _sideScreenPreviewGroups.value = emptyList()
            _notificationGroups.value = emptyList()
        }
    }

    private fun buildNotificationGroups(
        notifications: List<com.flue.launcher.service.NotifData>,
        expandedPackages: Set<String>
    ): List<NotificationGroupUi> {
        return notifications
            .groupBy { it.packageName }
            .values
            .map { group ->
                val children = group
                    .sortedByDescending { it.time }
                    .map { item ->
                        NotificationEntryUi(
                            key = item.key,
                            packageName = item.packageName,
                            groupKey = item.groupKey,
                            appLabel = item.appLabel,
                            title = item.title,
                            text = item.text,
                            time = item.time,
                            icon = item.icon,
                            isClearable = item.isClearable,
                            contentIntentAvailable = item.contentIntentAvailable,
                            isOngoing = item.isOngoing,
                            isForegroundService = item.isForegroundService
                        )
                    }
                val latest = children.first()
                NotificationGroupUi(
                    packageName = latest.packageName,
                    appLabel = latest.appLabel,
                    headerTitle = latest.appLabel,
                    icon = latest.icon,
                    latestTime = latest.time,
                    latestSummary = latest.text.ifBlank { latest.title.ifBlank { latest.appLabel } },
                    entries = children,
                    visiblePreviewEntries = children.take(1),
                    hiddenPreviewCount = (children.size - 1).coerceAtLeast(0),
                    expanded = latest.packageName in expandedPackages
                )
            }
            .sortedByDescending { it.latestTime }
    }

    override fun onCleared() {
        refreshIconsJob?.cancel()
        pendingNotificationClearJob?.cancel()
        pendingWriteJobs.values.forEach(Job::cancel)
        pendingWriteJobs.clear()
        super.onCleared()
        appRepository.destroy()
    }
}
