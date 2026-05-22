package com.flue.launcher.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.flue.launcher.FlueApplication
import com.flue.launcher.backup.FlueBackupManager
import com.flue.launcher.backup.FlueBackupOptions
import com.flue.launcher.backup.FlueBackupPreview
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.data.model.iconForDisplay
import com.flue.launcher.iconpack.IconPackDescriptor
import com.flue.launcher.iconpack.IconPackScanner
import com.flue.launcher.ui.theme.ThemeMode
import com.flue.launcher.ui.theme.UiStyle
import com.flue.launcher.ui.navigation.LayoutMode
import com.flue.launcher.ui.navigation.ScreenState
import com.flue.launcher.ui.notification.NotificationActionUi
import com.flue.launcher.ui.notification.NotificationEntryUi
import com.flue.launcher.ui.notification.NotificationGroupUi
import com.flue.launcher.ui.notification.NotificationRevealTarget
import com.flue.launcher.service.NotifData
import com.flue.launcher.service.WLauncherNotificationListener
import com.flue.launcher.ui.controlcenter.MusicTextSwitchAnimations
import com.flue.launcher.watchface.BUILT_IN_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_VIDEO_WATCHFACE_ID
import com.flue.launcher.watchface.WatchClockPosition
import com.flue.launcher.watchface.WatchClockColorMode
import com.flue.launcher.watchface.WatchFaceClockStyle
import com.flue.launcher.watchface.WatchFaceMd3eShape
import com.flue.launcher.watchface.InternalWatchFaceStorage
import com.flue.launcher.watchface.WatchFacePhotoCache
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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

enum class SideScreenSection(val prefValue: String) {
    Widgets("widgets"),
    Notifications("notifications")
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_UI_STYLE = stringPreferencesKey("ui_style")
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
        val KEY_HONEYCOMB_EDGE_BLUR_TENTHS = intPreferencesKey("honeycomb_edge_blur_tenths")
        val KEY_HONEYCOMB_TOP_FADE = intPreferencesKey("honeycomb_top_fade")
        val KEY_HONEYCOMB_BOTTOM_FADE = intPreferencesKey("honeycomb_bottom_fade")
        val KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION = booleanPreferencesKey("honeycomb_fast_scroll_optimization")
        val KEY_APP_LIST_FISHEYE = booleanPreferencesKey("app_list_fisheye_enabled")
        val KEY_APP_LIST_FISHEYE_RANGE_ROWS = intPreferencesKey("app_list_fisheye_range_rows")
        val KEY_APP_LIST_FISHEYE_STRENGTH_PERCENT = intPreferencesKey("app_list_fisheye_strength_percent")
        val KEY_APP_LIST_EDGE_SPACING_COMPRESSION = booleanPreferencesKey("app_list_edge_spacing_compression_enabled")
        val KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT = intPreferencesKey("app_list_left_safe_inset_percent")
        val KEY_APP_LIST_SCALE_PERCENT = intPreferencesKey("app_list_scale_percent")
        val KEY_GLOBAL_UI_SCALE_PERCENT = intPreferencesKey("global_ui_scale_percent")
        val KEY_APP_LIST_WATCHFACE_COLORS = booleanPreferencesKey("app_list_watchface_colors")
        val KEY_APP_LIST_ROW_BORDER = booleanPreferencesKey("app_list_row_border_enabled")
        val KEY_APP_LIST_FOLDERS_ENABLED = booleanPreferencesKey("app_list_folders_enabled")
        val KEY_FAST_FLOW_ANIMATION = booleanPreferencesKey("fast_flow_animation_enabled")
        val KEY_MUSIC_TEXT_SWITCH_ANIMATION = stringPreferencesKey("music_text_switch_animation")
        val KEY_TWO_TONE_ICONS = booleanPreferencesKey("two_tone_icons_enabled")
        val KEY_ICON_SHADOW = booleanPreferencesKey("app_icon_shadow_enabled")
        val KEY_CLASSIC_RETURN_ANIMATION = booleanPreferencesKey("classic_return_animation_enabled")
        val KEY_SHOW_STEP_COUNT = booleanPreferencesKey("show_step_count")
        val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val KEY_SHOW_ONGOING_NOTIFICATIONS = booleanPreferencesKey("show_ongoing_notifications")
        val KEY_ROTARY_HAPTICS_ENABLED = booleanPreferencesKey("rotary_haptics_enabled")
        val KEY_NOTIFICATION_SETTING_MIGRATED = booleanPreferencesKey("notification_setting_migrated")
        val KEY_SHOW_WIDGETS = booleanPreferencesKey("show_widgets")
        val KEY_SHOW_WIDGET_PAGE = booleanPreferencesKey("show_widget_page")
        val KEY_SHOW_CONTROL_CENTER = booleanPreferencesKey("show_control_center")
        val KEY_SHOW_MUSIC_CONTROLS = booleanPreferencesKey("show_music_controls")
        val KEY_DOUBLE_TAP_LOCK_SCREEN = booleanPreferencesKey("double_tap_lock_screen_enabled")
        val KEY_POWER_MENU_BUTTON = booleanPreferencesKey("power_menu_button_enabled")
        val KEY_WATCHFACE_CHARGING_POWER_TEXT = booleanPreferencesKey("watchface_charging_power_text")
        val KEY_WATCHFACE_STATUS_INDICATORS = booleanPreferencesKey("watchface_status_indicators")
        val KEY_WATCHFACE_BOTTOM_FADE = booleanPreferencesKey("watchface_bottom_fade_enabled")
        val KEY_DINGDINGCAT_FILL_SCREEN = booleanPreferencesKey("dingdingcat_fill_screen")
        val KEY_DINGDINGCAT_PLAYBACK_SPEED_PERCENT = intPreferencesKey("dingdingcat_playback_speed_percent")
        val KEY_DINGDINGCAT_IMPORT_UNLOCKED = booleanPreferencesKey("dingdingcat_import_unlocked")
        val KEY_SIDE_SCREEN_SECTION_ORDER = stringPreferencesKey("side_screen_section_order")
        val KEY_DEVICE_PRESET_APPLIED = booleanPreferencesKey("device_preset_applied")
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
        val KEY_WATCHFACE_FONT_PATH = stringPreferencesKey("watchface_font_path")
        val KEY_PHOTO_CLOCK_STYLE = stringPreferencesKey("photo_clock_style")
        val KEY_VIDEO_CLOCK_STYLE = stringPreferencesKey("video_clock_style")
        val KEY_PHOTO_MD3E_SHAPE = stringPreferencesKey("photo_md3e_shape")
        val KEY_VIDEO_MD3E_SHAPE = stringPreferencesKey("video_md3e_shape")
        val KEY_PHOTO_USE_THEME_TEXT_COLOR = booleanPreferencesKey("photo_use_theme_text_color")
        val KEY_VIDEO_USE_THEME_TEXT_COLOR = booleanPreferencesKey("video_use_theme_text_color")
        val KEY_PHOTO_TEXT_COLOR = intPreferencesKey("photo_text_color")
        val KEY_VIDEO_TEXT_COLOR = intPreferencesKey("video_text_color")
        val KEY_PHOTO_MD3E_AUTO_COLORS = booleanPreferencesKey("photo_md3e_auto_colors")
        val KEY_VIDEO_MD3E_AUTO_COLORS = booleanPreferencesKey("video_md3e_auto_colors")
        val KEY_PHOTO_MD3E_TEXT_COLOR = intPreferencesKey("photo_md3e_text_color")
        val KEY_VIDEO_MD3E_TEXT_COLOR = intPreferencesKey("video_md3e_text_color")
        val KEY_PHOTO_MD3E_FACE_COLOR = intPreferencesKey("photo_md3e_face_color")
        val KEY_VIDEO_MD3E_FACE_COLOR = intPreferencesKey("video_md3e_face_color")
        val KEY_PHOTO_MD3E_HOUR_COLOR = intPreferencesKey("photo_md3e_hour_color")
        val KEY_VIDEO_MD3E_HOUR_COLOR = intPreferencesKey("video_md3e_hour_color")
        val KEY_PHOTO_MD3E_MINUTE_COLOR = intPreferencesKey("photo_md3e_minute_color")
        val KEY_VIDEO_MD3E_MINUTE_COLOR = intPreferencesKey("video_md3e_minute_color")
        val KEY_PHOTO_MD3E_SECOND_COLOR = intPreferencesKey("photo_md3e_second_color")
        val KEY_VIDEO_MD3E_SECOND_COLOR = intPreferencesKey("video_md3e_second_color")
        val KEY_PHOTO_SHOW_SECONDS = booleanPreferencesKey("photo_show_seconds")
        val KEY_VIDEO_SHOW_SECONDS = booleanPreferencesKey("video_show_seconds")
        val KEY_PHOTO_CUSTOM_TEXT = stringPreferencesKey("photo_custom_text")
        val KEY_VIDEO_CUSTOM_TEXT = stringPreferencesKey("video_custom_text")
        val KEY_BUILTIN_MANAGER_THUMBNAILS = booleanPreferencesKey("builtin_manager_thumbnails")
        val KEY_HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
        val KEY_SIDE_SCREEN_WIDGETS = stringPreferencesKey("side_screen_widgets")
        val KEY_SIDE_SCREEN_SHORTCUT_ROWS = intPreferencesKey("side_screen_shortcut_rows")
        val KEY_SIDE_SCREEN_SHORTCUT_COLS = intPreferencesKey("side_screen_shortcut_cols")
        private const val SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY = 12
        private const val APP_LIST_LEFT_SAFE_INSET_MAX_PERCENT = 50
        const val DINGDINGCAT_PLAYBACK_SPEED_MIN_PERCENT = 80
        const val DINGDINGCAT_PLAYBACK_SPEED_MAX_PERCENT = 400
        const val DINGDINGCAT_PLAYBACK_SPEED_STEP_PERCENT = 10

        private fun normalizeEdgeBlurTenths(value: Int): Int {
            return ((value.coerceIn(5, 50) / 5f).roundToInt() * 5).coerceIn(5, 50)
        }

        private fun normalizeEdgeBlurTenths(value: Float): Int {
            return ((value.coerceIn(0.5f, 5f) * 2f).roundToInt() * 5).coerceIn(5, 50)
        }

        private fun normalizeLeftSafeInsetPercent(value: Int): Int {
            return ((value.coerceIn(0, APP_LIST_LEFT_SAFE_INSET_MAX_PERCENT) / 5f).roundToInt() * 5)
                .coerceIn(0, APP_LIST_LEFT_SAFE_INSET_MAX_PERCENT)
        }

        private fun normalizeAppListScalePercent(value: Int): Int {
            val clamped = value.coerceIn(50, 200)
            if (clamped == 200) return 200
            return ((clamped / 10) * 10).coerceIn(50, 200)
        }

        private fun normalizeDingDingCatPlaybackSpeedPercent(value: Int): Int {
            return ((value.coerceIn(
                DINGDINGCAT_PLAYBACK_SPEED_MIN_PERCENT,
                DINGDINGCAT_PLAYBACK_SPEED_MAX_PERCENT
            ) / DINGDINGCAT_PLAYBACK_SPEED_STEP_PERCENT.toFloat()).roundToInt() *
                DINGDINGCAT_PLAYBACK_SPEED_STEP_PERCENT)
                .coerceIn(
                    DINGDINGCAT_PLAYBACK_SPEED_MIN_PERCENT,
                    DINGDINGCAT_PLAYBACK_SPEED_MAX_PERCENT
                )
        }
    }

    private val store = application.dataStore
    private val repositories = FlueApplication.repositories(application)
    private val appRepository = repositories.appRepository

    val allApps: StateFlow<List<AppInfo>> = appRepository.allApps
    val apps: StateFlow<List<AppInfo>> = appRepository.apps
    private val _openFolder = MutableStateFlow<AppInfo?>(null)
    val openFolder: StateFlow<AppInfo?> = _openFolder.asStateFlow()
    private val _openFolderItems = MutableStateFlow<List<AppInfo>>(emptyList())
    val openFolderItems: StateFlow<List<AppInfo>> = _openFolderItems.asStateFlow()
    private val _screenState = MutableStateFlow(ScreenState.Face)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()
    private val _launcherInteractive = MutableStateFlow(true)
    val launcherInteractive: StateFlow<Boolean> = _launcherInteractive.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.Honeycomb)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()
    private val _themeMode = repositories.sharedState.themeMode
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val _uiStyle = repositories.sharedState.uiStyle
    val uiStyle: StateFlow<UiStyle> = _uiStyle.asStateFlow()
    private val _sideScreenEnabled = MutableStateFlow(true)
    val sideScreenEnabled: StateFlow<Boolean> = _sideScreenEnabled.asStateFlow()
    private val _sideScreenShortcutRows = MutableStateFlow(2)
    val sideScreenShortcutRows: StateFlow<Int> = _sideScreenShortcutRows.asStateFlow()
    private val _sideScreenShortcutCols = MutableStateFlow(3)
    val sideScreenShortcutCols: StateFlow<Int> = _sideScreenShortcutCols.asStateFlow()
    private val _sideScreenShortcuts = MutableStateFlow(List(SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) { null as String? })
    val sideScreenShortcuts: StateFlow<List<String?>> = _sideScreenShortcuts.asStateFlow()

    private val _sideScreenWidgetSlots = MutableStateFlow(emptyList<String?>())
    val sideScreenWidgetSlots: StateFlow<List<String?>> = _sideScreenWidgetSlots.asStateFlow()

    fun setWidgetSlot(slotIndex: Int, slotValue: String?) {
        if (slotIndex < 0) return
        val normalizedValue = slotValue?.takeIf { it.isNotBlank() }
        val next = _sideScreenWidgetSlots.value.toMutableList()
        when {
            slotIndex < next.size && normalizedValue == null -> next.removeAt(slotIndex)
            slotIndex < next.size -> next[slotIndex] = normalizedValue
            slotIndex == next.size && normalizedValue != null -> next.add(normalizedValue)
            else -> return
        }
        updateSideScreenWidgets(next)
    }

    fun removeWidgetSlot(slotIndex: Int) {
        if (slotIndex !in _sideScreenWidgetSlots.value.indices) return
        val next = _sideScreenWidgetSlots.value.toMutableList().apply { removeAt(slotIndex) }
        updateSideScreenWidgets(next)
    }

    fun swapWidgetSlots(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = _sideScreenWidgetSlots.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val next = current.toMutableList()
        val item = next.removeAt(fromIndex)
        next.add(toIndex, item)
        updateSideScreenWidgets(next)
    }

    private fun updateSideScreenWidgets(widgets: List<String?>) {
        val normalized = widgets.mapNotNull { value -> value?.takeIf { it.isNotBlank() } }
        _sideScreenWidgetSlots.value = normalized
        persist {
            store.edit { prefs ->
                val serialized = serializeSideScreenWidgets(normalized)
                if (serialized.isEmpty()) {
                    prefs.remove(KEY_SIDE_SCREEN_WIDGETS)
                } else {
                    prefs[KEY_SIDE_SCREEN_WIDGETS] = serialized
                }
            }
        }
    }

    fun setSideScreenShortcutRows(rows: Int) {
        _sideScreenShortcutRows.value = rows.coerceIn(1, 3)
        val savedValue = _sideScreenShortcutRows.value
        persistDebounced("side_screen_shortcut_rows") { store.edit { it[KEY_SIDE_SCREEN_SHORTCUT_ROWS] = savedValue } }
    }

    fun setSideScreenShortcutCols(cols: Int) {
        _sideScreenShortcutCols.value = cols.coerceIn(2, 4)
        val savedValue = _sideScreenShortcutCols.value
        persistDebounced("side_screen_shortcut_cols") { store.edit { it[KEY_SIDE_SCREEN_SHORTCUT_COLS] = savedValue } }
    }

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
    private val _honeycombEdgeBlurRadius = MutableStateFlow(4f)
    val honeycombEdgeBlurRadius: StateFlow<Float> = _honeycombEdgeBlurRadius.asStateFlow()

    private val _honeycombTopFade = MutableStateFlow(30)
    val honeycombTopFade: StateFlow<Int> = _honeycombTopFade.asStateFlow()

    private val _honeycombBottomFade = MutableStateFlow(30)
    val honeycombBottomFade: StateFlow<Int> = _honeycombBottomFade.asStateFlow()

    private val _honeycombFastScrollOptimization = MutableStateFlow(true)
    val honeycombFastScrollOptimization: StateFlow<Boolean> = _honeycombFastScrollOptimization.asStateFlow()
    private val _appListFisheyeEnabled = MutableStateFlow(true)
    val appListFisheyeEnabled: StateFlow<Boolean> = _appListFisheyeEnabled.asStateFlow()
    private val _appListFisheyeRangeRows = MutableStateFlow(4)
    val appListFisheyeRangeRows: StateFlow<Int> = _appListFisheyeRangeRows.asStateFlow()
    private val _appListFisheyeStrengthPercent = MutableStateFlow(100)
    val appListFisheyeStrengthPercent: StateFlow<Int> = _appListFisheyeStrengthPercent.asStateFlow()
    private val _appListEdgeSpacingCompressionEnabled = MutableStateFlow(true)
    val appListEdgeSpacingCompressionEnabled: StateFlow<Boolean> =
        _appListEdgeSpacingCompressionEnabled.asStateFlow()
    private val _appListLeftSafeInsetPercent = MutableStateFlow(0)
    val appListLeftSafeInsetPercent: StateFlow<Int> = _appListLeftSafeInsetPercent.asStateFlow()
    private val _appListScalePercent = MutableStateFlow(100)
    val appListScalePercent: StateFlow<Int> = _appListScalePercent.asStateFlow()
    private val _globalUiScalePercent = MutableStateFlow(100)
    val globalUiScalePercent: StateFlow<Int> = _globalUiScalePercent.asStateFlow()
    private val _appListWatchFaceColors = MutableStateFlow(true)
    val appListWatchFaceColors: StateFlow<Boolean> = _appListWatchFaceColors.asStateFlow()
    private val _appListRowBorderEnabled = MutableStateFlow(false)
    val appListRowBorderEnabled: StateFlow<Boolean> = _appListRowBorderEnabled.asStateFlow()
    private val _appListFoldersEnabled = MutableStateFlow(true)
    val appListFoldersEnabled: StateFlow<Boolean> = _appListFoldersEnabled.asStateFlow()
    private val _fastFlowAnimationEnabled = MutableStateFlow(false)
    val fastFlowAnimationEnabled: StateFlow<Boolean> = _fastFlowAnimationEnabled.asStateFlow()
    private val _musicTextSwitchAnimation = MutableStateFlow(MusicTextSwitchAnimations.DEFAULT_ID)
    val musicTextSwitchAnimation: StateFlow<String> = _musicTextSwitchAnimation.asStateFlow()
    private val _twoToneIconsEnabled = MutableStateFlow(false)
    val twoToneIconsEnabled: StateFlow<Boolean> = _twoToneIconsEnabled.asStateFlow()
    private val _iconShadowEnabled = MutableStateFlow(true)
    val iconShadowEnabled: StateFlow<Boolean> = _iconShadowEnabled.asStateFlow()
    private val _classicReturnAnimationEnabled = MutableStateFlow(false)
    val classicReturnAnimationEnabled: StateFlow<Boolean> = _classicReturnAnimationEnabled.asStateFlow()
    private val _showStepCount = MutableStateFlow(true)
    val showStepCount: StateFlow<Boolean> = _showStepCount.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification.asStateFlow()
    private val _showOngoingNotifications = MutableStateFlow(false)
    val showOngoingNotifications: StateFlow<Boolean> = _showOngoingNotifications.asStateFlow()
    private val _rotaryHapticsEnabled = MutableStateFlow(true)
    val rotaryHapticsEnabled: StateFlow<Boolean> = _rotaryHapticsEnabled.asStateFlow()
    private val _showWidgets = MutableStateFlow(true)
    val showWidgets: StateFlow<Boolean> = _showWidgets.asStateFlow()
    private val _showWidgetPage = MutableStateFlow(true)
    val showWidgetPage: StateFlow<Boolean> = _showWidgetPage.asStateFlow()
    private val _showControlCenter = MutableStateFlow(true)
    val showControlCenter: StateFlow<Boolean> = _showControlCenter.asStateFlow()
    private val _showMusicControls = MutableStateFlow(true)
    val showMusicControls: StateFlow<Boolean> = _showMusicControls.asStateFlow()
    private val _doubleTapLockScreenEnabled = MutableStateFlow(false)
    val doubleTapLockScreenEnabled: StateFlow<Boolean> = _doubleTapLockScreenEnabled.asStateFlow()
    private val _powerMenuButtonEnabled = MutableStateFlow(false)
    val powerMenuButtonEnabled: StateFlow<Boolean> = _powerMenuButtonEnabled.asStateFlow()
    private val _watchFaceChargingPowerText = MutableStateFlow(true)
    val watchFaceChargingPowerText: StateFlow<Boolean> = _watchFaceChargingPowerText.asStateFlow()
    private val _watchFaceStatusIndicators = MutableStateFlow(true)
    val watchFaceStatusIndicators: StateFlow<Boolean> = _watchFaceStatusIndicators.asStateFlow()
    private val _watchFaceBottomFadeEnabled = MutableStateFlow(true)
    val watchFaceBottomFadeEnabled: StateFlow<Boolean> = _watchFaceBottomFadeEnabled.asStateFlow()
    private val _dingDingCatFillScreen = MutableStateFlow(false)
    val dingDingCatFillScreen: StateFlow<Boolean> = _dingDingCatFillScreen.asStateFlow()
    private val _dingDingCatPlaybackSpeedPercent = MutableStateFlow(100)
    val dingDingCatPlaybackSpeedPercent: StateFlow<Int> = _dingDingCatPlaybackSpeedPercent.asStateFlow()
    private val _dingDingCatImportUnlocked = MutableStateFlow(false)
    val dingDingCatImportUnlocked: StateFlow<Boolean> = _dingDingCatImportUnlocked.asStateFlow()
    private val _sideScreenSectionOrder = MutableStateFlow(defaultSideScreenSectionOrder())
    val sideScreenSectionOrder: StateFlow<List<SideScreenSection>> = _sideScreenSectionOrder.asStateFlow()
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

    private val _builtInWatchFaceFontPath = MutableStateFlow<String?>(null)
    val builtInWatchFaceFontPath: StateFlow<String?> = _builtInWatchFaceFontPath.asStateFlow()

    private val _builtInPhotoClockStyle = MutableStateFlow(WatchFaceClockStyle.DIGITAL)
    val builtInPhotoClockStyle: StateFlow<WatchFaceClockStyle> = _builtInPhotoClockStyle.asStateFlow()

    private val _builtInVideoClockStyle = MutableStateFlow(WatchFaceClockStyle.DIGITAL)
    val builtInVideoClockStyle: StateFlow<WatchFaceClockStyle> = _builtInVideoClockStyle.asStateFlow()

    private val _builtInPhotoMd3eShape = MutableStateFlow(WatchFaceMd3eShape.COOKIE)
    val builtInPhotoMd3eShape: StateFlow<WatchFaceMd3eShape> = _builtInPhotoMd3eShape.asStateFlow()

    private val _builtInVideoMd3eShape = MutableStateFlow(WatchFaceMd3eShape.COOKIE)
    val builtInVideoMd3eShape: StateFlow<WatchFaceMd3eShape> = _builtInVideoMd3eShape.asStateFlow()

    private val _builtInPhotoUseThemeTextColor = MutableStateFlow(true)
    val builtInPhotoUseThemeTextColor: StateFlow<Boolean> = _builtInPhotoUseThemeTextColor.asStateFlow()

    private val _builtInVideoUseThemeTextColor = MutableStateFlow(true)
    val builtInVideoUseThemeTextColor: StateFlow<Boolean> = _builtInVideoUseThemeTextColor.asStateFlow()

    private val _builtInPhotoTextColorArgb = MutableStateFlow(0xFFFFFFFF.toInt())
    val builtInPhotoTextColorArgb: StateFlow<Int> = _builtInPhotoTextColorArgb.asStateFlow()

    private val _builtInVideoTextColorArgb = MutableStateFlow(0xFFFFFFFF.toInt())
    val builtInVideoTextColorArgb: StateFlow<Int> = _builtInVideoTextColorArgb.asStateFlow()

    private val _builtInPhotoMd3eAutoColors = MutableStateFlow(true)
    val builtInPhotoMd3eAutoColors: StateFlow<Boolean> = _builtInPhotoMd3eAutoColors.asStateFlow()

    private val _builtInVideoMd3eAutoColors = MutableStateFlow(true)
    val builtInVideoMd3eAutoColors: StateFlow<Boolean> = _builtInVideoMd3eAutoColors.asStateFlow()

    private val _builtInPhotoMd3eTextColorArgb = MutableStateFlow(0xFF202938.toInt())
    val builtInPhotoMd3eTextColorArgb: StateFlow<Int> = _builtInPhotoMd3eTextColorArgb.asStateFlow()

    private val _builtInVideoMd3eTextColorArgb = MutableStateFlow(0xFF202938.toInt())
    val builtInVideoMd3eTextColorArgb: StateFlow<Int> = _builtInVideoMd3eTextColorArgb.asStateFlow()

    private val _builtInPhotoMd3eFaceColorArgb = MutableStateFlow(0xFFEAF1FF.toInt())
    val builtInPhotoMd3eFaceColorArgb: StateFlow<Int> = _builtInPhotoMd3eFaceColorArgb.asStateFlow()

    private val _builtInVideoMd3eFaceColorArgb = MutableStateFlow(0xFFEAF1FF.toInt())
    val builtInVideoMd3eFaceColorArgb: StateFlow<Int> = _builtInVideoMd3eFaceColorArgb.asStateFlow()

    private val _builtInPhotoMd3eHourColorArgb = MutableStateFlow(0xFF334155.toInt())
    val builtInPhotoMd3eHourColorArgb: StateFlow<Int> = _builtInPhotoMd3eHourColorArgb.asStateFlow()

    private val _builtInVideoMd3eHourColorArgb = MutableStateFlow(0xFF334155.toInt())
    val builtInVideoMd3eHourColorArgb: StateFlow<Int> = _builtInVideoMd3eHourColorArgb.asStateFlow()

    private val _builtInPhotoMd3eMinuteColorArgb = MutableStateFlow(0xFF5F84B6.toInt())
    val builtInPhotoMd3eMinuteColorArgb: StateFlow<Int> = _builtInPhotoMd3eMinuteColorArgb.asStateFlow()

    private val _builtInVideoMd3eMinuteColorArgb = MutableStateFlow(0xFF5F84B6.toInt())
    val builtInVideoMd3eMinuteColorArgb: StateFlow<Int> = _builtInVideoMd3eMinuteColorArgb.asStateFlow()

    private val _builtInPhotoMd3eSecondColorArgb = MutableStateFlow(0xFF806EA4.toInt())
    val builtInPhotoMd3eSecondColorArgb: StateFlow<Int> = _builtInPhotoMd3eSecondColorArgb.asStateFlow()

    private val _builtInVideoMd3eSecondColorArgb = MutableStateFlow(0xFF806EA4.toInt())
    val builtInVideoMd3eSecondColorArgb: StateFlow<Int> = _builtInVideoMd3eSecondColorArgb.asStateFlow()

    private val _builtInPhotoShowSeconds = MutableStateFlow(false)
    val builtInPhotoShowSeconds: StateFlow<Boolean> = _builtInPhotoShowSeconds.asStateFlow()

    private val _builtInVideoShowSeconds = MutableStateFlow(false)
    val builtInVideoShowSeconds: StateFlow<Boolean> = _builtInVideoShowSeconds.asStateFlow()

    private val _builtInPhotoCustomText = MutableStateFlow("")
    val builtInPhotoCustomText: StateFlow<String> = _builtInPhotoCustomText.asStateFlow()

    private val _builtInVideoCustomText = MutableStateFlow("")
    val builtInVideoCustomText: StateFlow<String> = _builtInVideoCustomText.asStateFlow()

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
    private var watchFaceRefreshJob: Job? = null
    private val pendingWriteJobs = ConcurrentHashMap<String, Job>()
    private var pendingNotificationClearJob: Job? = null
    private var refreshIconsJob: Job? = null
    init {
        refreshNotificationAccess()
        viewModelScope.launch {
            combine(
                WLauncherNotificationListener.notifications,
                _expandedNotificationGroups,
                _pendingDismissedNotificationKeys,
                _showOngoingNotifications
            ) { notifications, expandedPackages, pendingDismissedKeys, showOngoing ->
                val visibleNotifications = notifications.filter { shouldShowNotification(it, showOngoing) }
                val filteredNotifications = visibleNotifications.filterNot { it.key in pendingDismissedKeys }
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

                val loadedThemeMode = prefs[KEY_THEME_MODE]?.let {
                    try {
                        ThemeMode.valueOf(it)
                    } catch (_: Exception) {
                        ThemeMode.SYSTEM
                    }
                } ?: ThemeMode.SYSTEM
                if (_themeMode.value != loadedThemeMode) _themeMode.value = loadedThemeMode

                val loadedUiStyle = prefs[KEY_UI_STYLE]?.let {
                    try {
                        UiStyle.valueOf(it)
                    } catch (_: Exception) {
                        UiStyle.APPLE_WATCH
                    }
                } ?: UiStyle.APPLE_WATCH
                if (_uiStyle.value != loadedUiStyle) _uiStyle.value = loadedUiStyle

                val loadedSideScreenEnabled = prefs[KEY_SIDE_SCREEN_ENABLED] ?: true
                if (_sideScreenEnabled.value != loadedSideScreenEnabled) _sideScreenEnabled.value = loadedSideScreenEnabled

                val loadedShortcutRows = (prefs[KEY_SIDE_SCREEN_SHORTCUT_ROWS] ?: 2).coerceIn(1, 3)
                if (_sideScreenShortcutRows.value != loadedShortcutRows) {
                    _sideScreenShortcutRows.value = loadedShortcutRows
                }

                val loadedShortcutCols = (prefs[KEY_SIDE_SCREEN_SHORTCUT_COLS] ?: 3).coerceIn(2, 4)
                if (_sideScreenShortcutCols.value != loadedShortcutCols) {
                    _sideScreenShortcutCols.value = loadedShortcutCols
                }

                val loadedSideScreenShortcuts = parseSideScreenShortcuts(prefs[KEY_SIDE_SCREEN_SHORTCUTS])
                if (_sideScreenShortcuts.value != loadedSideScreenShortcuts) {
                    _sideScreenShortcuts.value = loadedSideScreenShortcuts
                }

                val loadedWidgets = parseSideScreenWidgets(prefs[KEY_SIDE_SCREEN_WIDGETS])
                if (_sideScreenWidgetSlots.value != loadedWidgets) {
                    _sideScreenWidgetSlots.value = loadedWidgets
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

                val devicePresetApplied = prefs[KEY_DEVICE_PRESET_APPLIED] ?: false
                val shouldApplyDevicePreset = !devicePresetApplied && isXiaomi17ProSeriesDevice()
                val hasSavedHoneycombCols = prefs[KEY_HONEYCOMB_COLS] != null
                val honeycombColsDefault = if (shouldApplyDevicePreset && !hasSavedHoneycombCols) 2 else 3

                val loadedHoneycombCols = (prefs[KEY_HONEYCOMB_COLS] ?: honeycombColsDefault).coerceIn(1, 5)
                if (_honeycombCols.value != loadedHoneycombCols) _honeycombCols.value = loadedHoneycombCols

                val loadedTopBlur = (prefs[KEY_HONEYCOMB_TOP_BLUR] ?: 4).coerceIn(0, 48)
                if (_honeycombTopBlur.value != loadedTopBlur) _honeycombTopBlur.value = loadedTopBlur

                val loadedBottomBlur = (prefs[KEY_HONEYCOMB_BOTTOM_BLUR] ?: 4).coerceIn(0, 48)
                if (_honeycombBottomBlur.value != loadedBottomBlur) _honeycombBottomBlur.value = loadedBottomBlur

                val loadedEdgeBlurTenths = (prefs[KEY_HONEYCOMB_EDGE_BLUR_TENTHS]
                    ?: (((loadedTopBlur + loadedBottomBlur) / 2f) * 10f).roundToInt())
                    .let(::normalizeEdgeBlurTenths)
                val loadedEdgeBlurRadius = loadedEdgeBlurTenths / 10f
                if (_honeycombEdgeBlurRadius.value != loadedEdgeBlurRadius) {
                    _honeycombEdgeBlurRadius.value = loadedEdgeBlurRadius
                }

                val loadedTopFade = (prefs[KEY_HONEYCOMB_TOP_FADE] ?: 30).coerceIn(0, 160)
                if (_honeycombTopFade.value != loadedTopFade) _honeycombTopFade.value = loadedTopFade

                val loadedBottomFade = (prefs[KEY_HONEYCOMB_BOTTOM_FADE] ?: 30).coerceIn(0, 160)
                if (_honeycombBottomFade.value != loadedBottomFade) _honeycombBottomFade.value = loadedBottomFade

                val loadedFastScrollOptimization = prefs[KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION] ?: true
                if (_honeycombFastScrollOptimization.value != loadedFastScrollOptimization) {
                    _honeycombFastScrollOptimization.value = loadedFastScrollOptimization
                }

                val loadedFisheyeEnabled = prefs[KEY_APP_LIST_FISHEYE] ?: true
                if (_appListFisheyeEnabled.value != loadedFisheyeEnabled) {
                    _appListFisheyeEnabled.value = loadedFisheyeEnabled
                }

                val loadedFisheyeRangeRows = (prefs[KEY_APP_LIST_FISHEYE_RANGE_ROWS] ?: 4).coerceIn(1, 8)
                if (_appListFisheyeRangeRows.value != loadedFisheyeRangeRows) {
                    _appListFisheyeRangeRows.value = loadedFisheyeRangeRows
                }

                val loadedFisheyeStrength = (prefs[KEY_APP_LIST_FISHEYE_STRENGTH_PERCENT] ?: 100).coerceIn(0, 200)
                if (_appListFisheyeStrengthPercent.value != loadedFisheyeStrength) {
                    _appListFisheyeStrengthPercent.value = loadedFisheyeStrength
                }

                val hasSavedLeftSafeInset = prefs[KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT] != null
                val loadedLeftSafeInset = (prefs[KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT]
                    ?: if (shouldApplyDevicePreset && !hasSavedLeftSafeInset && isXiaomi17ProSeriesDevice()) 20 else 0)
                    .let(::normalizeLeftSafeInsetPercent)
                if (_appListLeftSafeInsetPercent.value != loadedLeftSafeInset) {
                    _appListLeftSafeInsetPercent.value = loadedLeftSafeInset
                }

                val loadedEdgeSpacingCompression = prefs[KEY_APP_LIST_EDGE_SPACING_COMPRESSION] ?: true
                if (_appListEdgeSpacingCompressionEnabled.value != loadedEdgeSpacingCompression) {
                    _appListEdgeSpacingCompressionEnabled.value = loadedEdgeSpacingCompression
                }

                val loadedAppListScale = normalizeAppListScalePercent(prefs[KEY_APP_LIST_SCALE_PERCENT] ?: 100)
                if (_appListScalePercent.value != loadedAppListScale) {
                    _appListScalePercent.value = loadedAppListScale
                }

                val loadedGlobalUiScale = (prefs[KEY_GLOBAL_UI_SCALE_PERCENT] ?: 100).coerceIn(50, 150)
                if (_globalUiScalePercent.value != loadedGlobalUiScale) {
                    _globalUiScalePercent.value = loadedGlobalUiScale
                }

                val loadedAppListWatchFaceColors = prefs[KEY_APP_LIST_WATCHFACE_COLORS] ?: true
                if (_appListWatchFaceColors.value != loadedAppListWatchFaceColors) {
                    _appListWatchFaceColors.value = loadedAppListWatchFaceColors
                }

                val loadedAppListRowBorder = prefs[KEY_APP_LIST_ROW_BORDER] ?: false
                if (_appListRowBorderEnabled.value != loadedAppListRowBorder) {
                    _appListRowBorderEnabled.value = loadedAppListRowBorder
                }

                val loadedAppListFoldersEnabled = prefs[KEY_APP_LIST_FOLDERS_ENABLED] ?: true
                if (_appListFoldersEnabled.value != loadedAppListFoldersEnabled) {
                    _appListFoldersEnabled.value = loadedAppListFoldersEnabled
                }

                val loadedFastFlowEnabled = prefs[KEY_FAST_FLOW_ANIMATION] ?: false
                if (_fastFlowAnimationEnabled.value != loadedFastFlowEnabled) {
                    _fastFlowAnimationEnabled.value = loadedFastFlowEnabled
                }

                val loadedMusicTextSwitchAnimation = MusicTextSwitchAnimations.normalizeId(
                    prefs[KEY_MUSIC_TEXT_SWITCH_ANIMATION]
                )
                if (_musicTextSwitchAnimation.value != loadedMusicTextSwitchAnimation) {
                    _musicTextSwitchAnimation.value = loadedMusicTextSwitchAnimation
                }

                val loadedTwoToneIcons = prefs[KEY_TWO_TONE_ICONS] ?: false
                if (_twoToneIconsEnabled.value != loadedTwoToneIcons) {
                    _twoToneIconsEnabled.value = loadedTwoToneIcons
                    appRepository.setTwoToneIconsEnabled(loadedTwoToneIcons)
                }

                val loadedIconShadow = prefs[KEY_ICON_SHADOW] ?: true
                if (_iconShadowEnabled.value != loadedIconShadow) {
                    _iconShadowEnabled.value = loadedIconShadow
                }

                val loadedClassicReturnAnimation = prefs[KEY_CLASSIC_RETURN_ANIMATION] ?: false
                if (_classicReturnAnimationEnabled.value != loadedClassicReturnAnimation) {
                    _classicReturnAnimationEnabled.value = loadedClassicReturnAnimation
                }

                val loadedShowStepCount = prefs[KEY_SHOW_STEP_COUNT] ?: true
                if (_showStepCount.value != loadedShowStepCount) {
                    _showStepCount.value = loadedShowStepCount
                }

                val loadedWatchFaceChargingPowerText = prefs[KEY_WATCHFACE_CHARGING_POWER_TEXT] ?: true
                if (_watchFaceChargingPowerText.value != loadedWatchFaceChargingPowerText) {
                    _watchFaceChargingPowerText.value = loadedWatchFaceChargingPowerText
                }

                val loadedWatchFaceStatusIndicators = prefs[KEY_WATCHFACE_STATUS_INDICATORS] ?: true
                if (_watchFaceStatusIndicators.value != loadedWatchFaceStatusIndicators) {
                    _watchFaceStatusIndicators.value = loadedWatchFaceStatusIndicators
                }

                val loadedWatchFaceBottomFade = prefs[KEY_WATCHFACE_BOTTOM_FADE] ?: true
                if (_watchFaceBottomFadeEnabled.value != loadedWatchFaceBottomFade) {
                    _watchFaceBottomFadeEnabled.value = loadedWatchFaceBottomFade
                }

                val loadedDingDingCatFillScreen = prefs[KEY_DINGDINGCAT_FILL_SCREEN] ?: false
                if (_dingDingCatFillScreen.value != loadedDingDingCatFillScreen) {
                    _dingDingCatFillScreen.value = loadedDingDingCatFillScreen
                }

                val loadedDingDingCatPlaybackSpeed = normalizeDingDingCatPlaybackSpeedPercent(
                    prefs[KEY_DINGDINGCAT_PLAYBACK_SPEED_PERCENT] ?: 100
                )
                if (_dingDingCatPlaybackSpeedPercent.value != loadedDingDingCatPlaybackSpeed) {
                    _dingDingCatPlaybackSpeedPercent.value = loadedDingDingCatPlaybackSpeed
                }

                val loadedDingDingCatImportUnlocked = prefs[KEY_DINGDINGCAT_IMPORT_UNLOCKED] ?: false
                if (_dingDingCatImportUnlocked.value != loadedDingDingCatImportUnlocked) {
                    _dingDingCatImportUnlocked.value = loadedDingDingCatImportUnlocked
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

                val loadedShowOngoingNotifications = prefs[KEY_SHOW_ONGOING_NOTIFICATIONS] ?: false
                if (_showOngoingNotifications.value != loadedShowOngoingNotifications) {
                    _showOngoingNotifications.value = loadedShowOngoingNotifications
                }

                val loadedRotaryHaptics = prefs[KEY_ROTARY_HAPTICS_ENABLED] ?: true
                if (_rotaryHapticsEnabled.value != loadedRotaryHaptics) {
                    _rotaryHapticsEnabled.value = loadedRotaryHaptics
                }

                val loadedShowWidgets = prefs[KEY_SHOW_WIDGETS] ?: true
                if (_showWidgets.value != loadedShowWidgets) {
                    _showWidgets.value = loadedShowWidgets
                }

                val loadedShowWidgetPage = prefs[KEY_SHOW_WIDGET_PAGE] ?: true
                if (_showWidgetPage.value != loadedShowWidgetPage) {
                    _showWidgetPage.value = loadedShowWidgetPage
                }

                val loadedShowControlCenter = prefs[KEY_SHOW_CONTROL_CENTER] ?: true
                if (_showControlCenter.value != loadedShowControlCenter) {
                    _showControlCenter.value = loadedShowControlCenter
                }

                val loadedShowMusicControls = prefs[KEY_SHOW_MUSIC_CONTROLS] ?: true
                if (_showMusicControls.value != loadedShowMusicControls) {
                    _showMusicControls.value = loadedShowMusicControls
                }

                val loadedDoubleTapLockScreen = prefs[KEY_DOUBLE_TAP_LOCK_SCREEN] ?: false
                if (_doubleTapLockScreenEnabled.value != loadedDoubleTapLockScreen) {
                    _doubleTapLockScreenEnabled.value = loadedDoubleTapLockScreen
                }

                val loadedPowerMenuButton = prefs[KEY_POWER_MENU_BUTTON] ?: false
                if (_powerMenuButtonEnabled.value != loadedPowerMenuButton) {
                    _powerMenuButtonEnabled.value = loadedPowerMenuButton
                }

                val loadedSectionOrder = parseSideScreenSectionOrder(prefs[KEY_SIDE_SCREEN_SECTION_ORDER])
                if (_sideScreenSectionOrder.value != loadedSectionOrder) {
                    _sideScreenSectionOrder.value = loadedSectionOrder
                }

                if (!devicePresetApplied) {
                    store.edit {
                        if (shouldApplyDevicePreset && !hasSavedHoneycombCols) {
                            it[KEY_HONEYCOMB_COLS] = 2
                        }
                        if (shouldApplyDevicePreset && !hasSavedLeftSafeInset) {
                            it[KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT] = 20
                        }
                        it[KEY_DEVICE_PRESET_APPLIED] = true
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

                val loadedWatchFaceFontPath = prefs[KEY_WATCHFACE_FONT_PATH]
                if (_builtInWatchFaceFontPath.value != loadedWatchFaceFontPath) {
                    _builtInWatchFaceFontPath.value = loadedWatchFaceFontPath
                }

                val loadedPhotoClockStyle = prefs[KEY_PHOTO_CLOCK_STYLE]
                    ?.let(::parseClockStyle)
                    ?: WatchFaceClockStyle.DIGITAL
                if (_builtInPhotoClockStyle.value != loadedPhotoClockStyle) {
                    _builtInPhotoClockStyle.value = loadedPhotoClockStyle
                }

                val loadedVideoClockStyle = prefs[KEY_VIDEO_CLOCK_STYLE]
                    ?.let(::parseClockStyle)
                    ?: WatchFaceClockStyle.DIGITAL
                if (_builtInVideoClockStyle.value != loadedVideoClockStyle) {
                    _builtInVideoClockStyle.value = loadedVideoClockStyle
                }

                val loadedPhotoMd3eShape = prefs[KEY_PHOTO_MD3E_SHAPE]
                    ?.let(::parseMd3eShape)
                    ?: WatchFaceMd3eShape.COOKIE
                if (_builtInPhotoMd3eShape.value != loadedPhotoMd3eShape) {
                    _builtInPhotoMd3eShape.value = loadedPhotoMd3eShape
                }

                val loadedVideoMd3eShape = prefs[KEY_VIDEO_MD3E_SHAPE]
                    ?.let(::parseMd3eShape)
                    ?: WatchFaceMd3eShape.COOKIE
                if (_builtInVideoMd3eShape.value != loadedVideoMd3eShape) {
                    _builtInVideoMd3eShape.value = loadedVideoMd3eShape
                }

                val loadedPhotoUseThemeTextColor = prefs[KEY_PHOTO_USE_THEME_TEXT_COLOR] ?: true
                if (_builtInPhotoUseThemeTextColor.value != loadedPhotoUseThemeTextColor) {
                    _builtInPhotoUseThemeTextColor.value = loadedPhotoUseThemeTextColor
                }

                val loadedVideoUseThemeTextColor = prefs[KEY_VIDEO_USE_THEME_TEXT_COLOR] ?: true
                if (_builtInVideoUseThemeTextColor.value != loadedVideoUseThemeTextColor) {
                    _builtInVideoUseThemeTextColor.value = loadedVideoUseThemeTextColor
                }

                val loadedPhotoTextColor = prefs[KEY_PHOTO_TEXT_COLOR] ?: 0xFFFFFFFF.toInt()
                if (_builtInPhotoTextColorArgb.value != loadedPhotoTextColor) {
                    _builtInPhotoTextColorArgb.value = loadedPhotoTextColor
                }

                val loadedVideoTextColor = prefs[KEY_VIDEO_TEXT_COLOR] ?: 0xFFFFFFFF.toInt()
                if (_builtInVideoTextColorArgb.value != loadedVideoTextColor) {
                    _builtInVideoTextColorArgb.value = loadedVideoTextColor
                }

                val loadedPhotoMd3eAutoColors = prefs[KEY_PHOTO_MD3E_AUTO_COLORS] ?: true
                if (_builtInPhotoMd3eAutoColors.value != loadedPhotoMd3eAutoColors) {
                    _builtInPhotoMd3eAutoColors.value = loadedPhotoMd3eAutoColors
                }

                val loadedVideoMd3eAutoColors = prefs[KEY_VIDEO_MD3E_AUTO_COLORS] ?: true
                if (_builtInVideoMd3eAutoColors.value != loadedVideoMd3eAutoColors) {
                    _builtInVideoMd3eAutoColors.value = loadedVideoMd3eAutoColors
                }

                val loadedPhotoMd3eTextColor = prefs[KEY_PHOTO_MD3E_TEXT_COLOR] ?: 0xFF202938.toInt()
                if (_builtInPhotoMd3eTextColorArgb.value != loadedPhotoMd3eTextColor) {
                    _builtInPhotoMd3eTextColorArgb.value = loadedPhotoMd3eTextColor
                }

                val loadedVideoMd3eTextColor = prefs[KEY_VIDEO_MD3E_TEXT_COLOR] ?: 0xFF202938.toInt()
                if (_builtInVideoMd3eTextColorArgb.value != loadedVideoMd3eTextColor) {
                    _builtInVideoMd3eTextColorArgb.value = loadedVideoMd3eTextColor
                }

                val loadedPhotoMd3eFaceColor = prefs[KEY_PHOTO_MD3E_FACE_COLOR] ?: 0xFFEAF1FF.toInt()
                if (_builtInPhotoMd3eFaceColorArgb.value != loadedPhotoMd3eFaceColor) {
                    _builtInPhotoMd3eFaceColorArgb.value = loadedPhotoMd3eFaceColor
                }

                val loadedVideoMd3eFaceColor = prefs[KEY_VIDEO_MD3E_FACE_COLOR] ?: 0xFFEAF1FF.toInt()
                if (_builtInVideoMd3eFaceColorArgb.value != loadedVideoMd3eFaceColor) {
                    _builtInVideoMd3eFaceColorArgb.value = loadedVideoMd3eFaceColor
                }

                val loadedPhotoMd3eHourColor = prefs[KEY_PHOTO_MD3E_HOUR_COLOR] ?: 0xFF334155.toInt()
                if (_builtInPhotoMd3eHourColorArgb.value != loadedPhotoMd3eHourColor) {
                    _builtInPhotoMd3eHourColorArgb.value = loadedPhotoMd3eHourColor
                }

                val loadedVideoMd3eHourColor = prefs[KEY_VIDEO_MD3E_HOUR_COLOR] ?: 0xFF334155.toInt()
                if (_builtInVideoMd3eHourColorArgb.value != loadedVideoMd3eHourColor) {
                    _builtInVideoMd3eHourColorArgb.value = loadedVideoMd3eHourColor
                }

                val loadedPhotoMd3eMinuteColor = prefs[KEY_PHOTO_MD3E_MINUTE_COLOR] ?: 0xFF5F84B6.toInt()
                if (_builtInPhotoMd3eMinuteColorArgb.value != loadedPhotoMd3eMinuteColor) {
                    _builtInPhotoMd3eMinuteColorArgb.value = loadedPhotoMd3eMinuteColor
                }

                val loadedVideoMd3eMinuteColor = prefs[KEY_VIDEO_MD3E_MINUTE_COLOR] ?: 0xFF5F84B6.toInt()
                if (_builtInVideoMd3eMinuteColorArgb.value != loadedVideoMd3eMinuteColor) {
                    _builtInVideoMd3eMinuteColorArgb.value = loadedVideoMd3eMinuteColor
                }

                val loadedPhotoMd3eSecondColor = prefs[KEY_PHOTO_MD3E_SECOND_COLOR] ?: 0xFF806EA4.toInt()
                if (_builtInPhotoMd3eSecondColorArgb.value != loadedPhotoMd3eSecondColor) {
                    _builtInPhotoMd3eSecondColorArgb.value = loadedPhotoMd3eSecondColor
                }

                val loadedVideoMd3eSecondColor = prefs[KEY_VIDEO_MD3E_SECOND_COLOR] ?: 0xFF806EA4.toInt()
                if (_builtInVideoMd3eSecondColorArgb.value != loadedVideoMd3eSecondColor) {
                    _builtInVideoMd3eSecondColorArgb.value = loadedVideoMd3eSecondColor
                }

                val loadedPhotoShowSeconds = prefs[KEY_PHOTO_SHOW_SECONDS] ?: false
                if (_builtInPhotoShowSeconds.value != loadedPhotoShowSeconds) {
                    _builtInPhotoShowSeconds.value = loadedPhotoShowSeconds
                }

                val loadedVideoShowSeconds = prefs[KEY_VIDEO_SHOW_SECONDS] ?: false
                if (_builtInVideoShowSeconds.value != loadedVideoShowSeconds) {
                    _builtInVideoShowSeconds.value = loadedVideoShowSeconds
                }

                val loadedPhotoCustomText = prefs[KEY_PHOTO_CUSTOM_TEXT].orEmpty()
                if (_builtInPhotoCustomText.value != loadedPhotoCustomText) {
                    _builtInPhotoCustomText.value = loadedPhotoCustomText
                }

                val loadedVideoCustomText = prefs[KEY_VIDEO_CUSTOM_TEXT].orEmpty()
                if (_builtInVideoCustomText.value != loadedVideoCustomText) {
                    _builtInVideoCustomText.value = loadedVideoCustomText
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
        _screenState.value = when {
            state == ScreenState.Notifications && !_sideScreenEnabled.value -> ScreenState.Face
            state == ScreenState.Notifications && !_showNotification.value -> ScreenState.Stack
            state == ScreenState.Widgets && !_showWidgetPage.value -> ScreenState.Face
            state == ScreenState.ControlCenter && !_showControlCenter.value -> ScreenState.Face
            else -> state
        }
        if (_screenState.value != ScreenState.Apps) {
            closeFolder()
        }
        _revealedNotificationTarget.value = null
    }

    fun setLauncherInteractive(interactive: Boolean) {
        _launcherInteractive.value = interactive
    }

    fun openApp(
        appInfo: AppInfo,
        origin: Offset = Offset(0.5f, 0.5f),
        launchDelayMs: Long = _splashDelay.value.toLong(),
        returnState: ScreenState = ScreenState.Apps
    ) {
        if (appInfo.isFolder) {
            openFolder(appInfo)
            return
        }
        if (!appInfo.isAppListShortcut && appInfo.packageName == getApplication<Application>().packageName) {
            Toast.makeText(getApplication(), "已阻止启动自身应用", Toast.LENGTH_SHORT).show()
            return
        }
        returnStateAfterExternalLaunch = returnState
        _launchSourceState.value = returnState
        _currentApp.value = appInfo
        _currentLaunchIcon.value = appInfo.iconForDisplay(useTwoTone = _twoToneIconsEnabled.value)
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
            ScreenState.Apps -> {
                if (_openFolder.value != null) {
                    closeFolder()
                } else {
                    _screenState.value = ScreenState.Face
                }
            }
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
            ScreenState.Widgets -> _screenState.value = ScreenState.Face
            ScreenState.ControlCenter -> _screenState.value = ScreenState.Face
        }
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        persist { store.edit { it[KEY_LAYOUT] = mode.name } }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        repositories.applicationScope.launch { store.edit { it[KEY_THEME_MODE] = mode.name } }
    }

    fun setUiStyle(style: UiStyle) {
        if (_uiStyle.value == style) return
        _uiStyle.value = style
        repositories.applicationScope.launch { store.edit { it[KEY_UI_STYLE] = style.name } }
    }

    fun setSideScreenEnabled(enabled: Boolean) {
        _sideScreenEnabled.value = enabled
        if (!enabled && (_screenState.value == ScreenState.Stack || _screenState.value == ScreenState.Notifications)) {
            _screenState.value = ScreenState.Face
        }
        persist { store.edit { it[KEY_SIDE_SCREEN_ENABLED] = enabled } }
    }

    fun setBlurEnabled(enabled: Boolean) {
        _blurEnabled.value = enabled
        persist {
            store.edit {
                it[KEY_BLUR] = enabled
            }
        }
    }

    fun setEdgeBlurEnabled(enabled: Boolean) {
        _edgeBlurEnabled.value = enabled
        persist { store.edit { it[KEY_EDGE_BLUR] = enabled } }
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

    fun openFolder(folder: AppInfo) {
        if (!folder.isFolder) return
        _openFolder.value = folder
        _openFolderItems.value = appRepository.folderItems(folder.componentKey)
    }

    fun closeFolder() {
        _openFolder.value = null
        _openFolderItems.value = emptyList()
    }

    fun createFolder(fromIndex: Int, toIndex: Int) {
        val current = apps.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val source = current[fromIndex]
        val target = current[toIndex]
        val folderKey = appRepository.createFolder(source.componentKey, target.componentKey) ?: return
        val insertIndex = minOf(fromIndex, toIndex)
        val nextOrder = current
            .filterIndexed { index, _ -> index != fromIndex && index != toIndex }
            .map { it.componentKey }
            .toMutableList()
        nextOrder.add(insertIndex.coerceIn(0, nextOrder.size), folderKey)
        setAppOrder(nextOrder)
    }

    fun removeAppListShortcut(shortcut: AppInfo) {
        if (!shortcut.isAppListShortcut) return
        if (appRepository.removeShortcut(shortcut.componentKey)) {
            setAppOrder(_appOrder.value.filterNot { it == shortcut.componentKey })
            closeFolder()
        }
    }

    fun renameFolder(folder: AppInfo, name: String) {
        if (!folder.isFolder) return
        if (appRepository.renameFolder(folder.componentKey, name)) {
            val renamed = folder.copy(label = name.ifBlank { "文件夹" })
            _openFolder.value = if (_openFolder.value?.componentKey == folder.componentKey) renamed else _openFolder.value
        }
    }

    fun reorderFolderItems(folder: AppInfo, orderedItems: List<AppInfo>) {
        if (!folder.isFolder) return
        val nextKeys = appRepository.reorderFolderItems(
            folder.componentKey,
            orderedItems.map { it.componentKey }
        )
        if (nextKeys.isNotEmpty()) {
            _openFolderItems.value = appRepository.itemsForKeys(nextKeys)
        }
    }

    fun dissolveFolder(folder: AppInfo) {
        if (!folder.isFolder) return
        val childKeys = appRepository.dissolveFolder(folder.componentKey)
        if (childKeys.isEmpty()) return
        val currentOrder = apps.value.map { it.componentKey }.toMutableList()
        val folderIndex = currentOrder.indexOf(folder.componentKey)
        currentOrder.remove(folder.componentKey)
        currentOrder.addAll(folderIndex.coerceIn(0, currentOrder.size), childKeys)
        setAppOrder(currentOrder)
        closeFolder()
    }

    fun moveItemOutOfFolder(folder: AppInfo, item: AppInfo) {
        if (!folder.isFolder) return
        val remainingKeys = appRepository.moveItemOutOfFolder(folder.componentKey, item.componentKey)
        val remainingItems = appRepository.itemsForKeys(remainingKeys)
        val dissolvesFolder = remainingKeys.size <= 1
        val currentOrder = apps.value.map { it.componentKey }.toMutableList()
        val folderIndex = currentOrder.indexOf(folder.componentKey).takeIf { it >= 0 } ?: currentOrder.size
        if (dissolvesFolder) {
            currentOrder.remove(folder.componentKey)
            val insertAt = folderIndex.coerceIn(0, currentOrder.size)
            remainingItems.forEachIndexed { offset, remaining ->
                if (remaining.componentKey !in currentOrder) {
                    currentOrder.add((insertAt + offset).coerceIn(0, currentOrder.size), remaining.componentKey)
                }
            }
        }
        val insertAt = folderIndex.coerceIn(0, currentOrder.size)
        if (item.componentKey !in currentOrder) {
            val offset = if (dissolvesFolder) remainingItems.size else 1
            currentOrder.add((insertAt + offset).coerceIn(0, currentOrder.size), item.componentKey)
        }
        setAppOrder(currentOrder)
        if (dissolvesFolder) {
            closeFolder()
        } else {
            _openFolderItems.value = remainingItems
        }
    }

    fun setHoneycombCols(cols: Int) {
        _honeycombCols.value = cols.coerceIn(1, 5)
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

    fun setHoneycombEdgeBlurRadius(value: Float) {
        val savedTenths = normalizeEdgeBlurTenths(value)
        _honeycombEdgeBlurRadius.value = savedTenths / 10f
        persistDebounced("key_honeycomb_edge_blur_tenths") {
            store.edit { it[KEY_HONEYCOMB_EDGE_BLUR_TENTHS] = savedTenths }
        }
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

    fun setAppListFisheyeEnabled(enabled: Boolean) {
        _appListFisheyeEnabled.value = enabled
        persist { store.edit { it[KEY_APP_LIST_FISHEYE] = enabled } }
    }

    fun setAppListFisheyeRangeRows(value: Int) {
        _appListFisheyeRangeRows.value = value.coerceIn(1, 8)
        val savedValue = _appListFisheyeRangeRows.value
        persistDebounced("key_app_list_fisheye_range_rows") {
            store.edit { it[KEY_APP_LIST_FISHEYE_RANGE_ROWS] = savedValue }
        }
    }

    fun setAppListFisheyeStrengthPercent(value: Int) {
        _appListFisheyeStrengthPercent.value = value.coerceIn(0, 200)
        val savedValue = _appListFisheyeStrengthPercent.value
        persistDebounced("key_app_list_fisheye_strength_percent") {
            store.edit { it[KEY_APP_LIST_FISHEYE_STRENGTH_PERCENT] = savedValue }
        }
    }

    fun setAppListEdgeSpacingCompressionEnabled(enabled: Boolean) {
        _appListEdgeSpacingCompressionEnabled.value = enabled
        persist { store.edit { it[KEY_APP_LIST_EDGE_SPACING_COMPRESSION] = enabled } }
    }

    fun setAppListLeftSafeInsetPercent(value: Int) {
        _appListLeftSafeInsetPercent.value = normalizeLeftSafeInsetPercent(value)
        val savedValue = _appListLeftSafeInsetPercent.value
        persistDebounced("key_app_list_left_safe_inset_percent") {
            store.edit { it[KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT] = savedValue }
        }
    }

    fun setAppListScalePercent(value: Int) {
        _appListScalePercent.value = normalizeAppListScalePercent(value)
        val savedValue = _appListScalePercent.value
        persistDebounced("key_app_list_scale_percent") {
            store.edit { it[KEY_APP_LIST_SCALE_PERCENT] = savedValue }
        }
    }

    fun setGlobalUiScalePercent(value: Int) {
        _globalUiScalePercent.value = value.coerceIn(50, 150)
        val savedValue = _globalUiScalePercent.value
        persistDebounced("key_global_ui_scale_percent") {
            store.edit { it[KEY_GLOBAL_UI_SCALE_PERCENT] = savedValue }
        }
    }

    fun setAppListWatchFaceColors(enabled: Boolean) {
        _appListWatchFaceColors.value = enabled
        persist { store.edit { it[KEY_APP_LIST_WATCHFACE_COLORS] = enabled } }
    }

    fun setAppListRowBorderEnabled(enabled: Boolean) {
        _appListRowBorderEnabled.value = enabled
        persist { store.edit { it[KEY_APP_LIST_ROW_BORDER] = enabled } }
    }

    fun setAppListFoldersEnabled(enabled: Boolean) {
        _appListFoldersEnabled.value = enabled
        persist { store.edit { it[KEY_APP_LIST_FOLDERS_ENABLED] = enabled } }
    }

    fun setFastFlowAnimationEnabled(enabled: Boolean) {
        _fastFlowAnimationEnabled.value = enabled
        persist { store.edit { it[KEY_FAST_FLOW_ANIMATION] = enabled } }
    }

    fun setMusicTextSwitchAnimation(id: String) {
        val normalized = MusicTextSwitchAnimations.normalizeId(id)
        _musicTextSwitchAnimation.value = normalized
        persist { store.edit { it[KEY_MUSIC_TEXT_SWITCH_ANIMATION] = normalized } }
    }

    fun setTwoToneIconsEnabled(enabled: Boolean) {
        if (_twoToneIconsEnabled.value == enabled) return
        _twoToneIconsEnabled.value = enabled
        appRepository.setTwoToneIconsEnabled(enabled)
        persist { store.edit { it[KEY_TWO_TONE_ICONS] = enabled } }
    }

    fun setIconShadowEnabled(enabled: Boolean) {
        _iconShadowEnabled.value = enabled
        persist { store.edit { it[KEY_ICON_SHADOW] = enabled } }
    }

    fun setClassicReturnAnimationEnabled(enabled: Boolean) {
        _classicReturnAnimationEnabled.value = enabled
        persist { store.edit { it[KEY_CLASSIC_RETURN_ANIMATION] = enabled } }
    }

    fun setShowStepCountEnabled(enabled: Boolean) {
        _showStepCount.value = enabled
        persist { store.edit { it[KEY_SHOW_STEP_COUNT] = enabled } }
    }

    fun setWatchFaceChargingPowerTextEnabled(enabled: Boolean) {
        _watchFaceChargingPowerText.value = enabled
        persist { store.edit { it[KEY_WATCHFACE_CHARGING_POWER_TEXT] = enabled } }
    }

    fun setWatchFaceStatusIndicatorsEnabled(enabled: Boolean) {
        _watchFaceStatusIndicators.value = enabled
        persist { store.edit { it[KEY_WATCHFACE_STATUS_INDICATORS] = enabled } }
    }

    fun setWatchFaceBottomFadeEnabled(enabled: Boolean) {
        _watchFaceBottomFadeEnabled.value = enabled
        persist { store.edit { it[KEY_WATCHFACE_BOTTOM_FADE] = enabled } }
    }

    fun setDingDingCatFillScreenEnabled(enabled: Boolean) {
        _dingDingCatFillScreen.value = enabled
        persist { store.edit { it[KEY_DINGDINGCAT_FILL_SCREEN] = enabled } }
    }

    fun setDingDingCatPlaybackSpeedPercent(value: Int) {
        val normalized = normalizeDingDingCatPlaybackSpeedPercent(value)
        _dingDingCatPlaybackSpeedPercent.value = normalized
        persist { store.edit { it[KEY_DINGDINGCAT_PLAYBACK_SPEED_PERCENT] = normalized } }
    }

    fun setDingDingCatImportUnlocked(unlocked: Boolean) {
        _dingDingCatImportUnlocked.value = unlocked
        persist { store.edit { it[KEY_DINGDINGCAT_IMPORT_UNLOCKED] = unlocked } }
    }

    fun setShowNotification(show: Boolean) {
        _showNotification.value = show
        if (!show && _screenState.value == ScreenState.Notifications) {
            _screenState.value = if (_sideScreenEnabled.value) ScreenState.Stack else ScreenState.Face
        }
        persist {
            store.edit {
                it[KEY_SHOW_NOTIFICATION] = show
                it[KEY_NOTIFICATION_SETTING_MIGRATED] = true
            }
        }
    }

    fun setShowOngoingNotifications(show: Boolean) {
        _showOngoingNotifications.value = show
        persist { store.edit { it[KEY_SHOW_ONGOING_NOTIFICATIONS] = show } }
    }

    fun setRotaryHapticsEnabled(enabled: Boolean) {
        _rotaryHapticsEnabled.value = enabled
        persist { store.edit { it[KEY_ROTARY_HAPTICS_ENABLED] = enabled } }
    }

    fun setShowWidgets(show: Boolean) {
        _showWidgets.value = show
        persist { store.edit { it[KEY_SHOW_WIDGETS] = show } }
    }

    fun setShowWidgetPage(show: Boolean) {
        _showWidgetPage.value = show
        if (!show && _screenState.value == ScreenState.Widgets) {
            _screenState.value = ScreenState.Face
        }
        persist { store.edit { it[KEY_SHOW_WIDGET_PAGE] = show } }
    }

    fun setShowControlCenter(show: Boolean) {
        _showControlCenter.value = show
        if (!show && _screenState.value == ScreenState.ControlCenter) {
            _screenState.value = ScreenState.Face
        }
        persist { store.edit { it[KEY_SHOW_CONTROL_CENTER] = show } }
    }

    fun setShowMusicControls(show: Boolean) {
        _showMusicControls.value = show
        persist { store.edit { it[KEY_SHOW_MUSIC_CONTROLS] = show } }
    }

    fun setDoubleTapLockScreenEnabled(enabled: Boolean) {
        _doubleTapLockScreenEnabled.value = enabled
        persist { store.edit { it[KEY_DOUBLE_TAP_LOCK_SCREEN] = enabled } }
    }

    fun setPowerMenuButtonEnabled(enabled: Boolean) {
        _powerMenuButtonEnabled.value = enabled
        persist { store.edit { it[KEY_POWER_MENU_BUTTON] = enabled } }
    }

    fun moveSideSection(section: SideScreenSection, direction: Int) {
        if (direction == 0) return
        val current = normalizeSideScreenSectionOrder(_sideScreenSectionOrder.value).toMutableList()
        val fromIndex = current.indexOf(section)
        if (fromIndex < 0) return
        val toIndex = (fromIndex + direction.coerceIn(-1, 1)).coerceIn(current.indices)
        if (fromIndex == toIndex) return
        current.removeAt(fromIndex)
        current.add(toIndex, section)
        val next = normalizeSideScreenSectionOrder(current)
        _sideScreenSectionOrder.value = next
        persist { store.edit { it[KEY_SIDE_SCREEN_SECTION_ORDER] = serializeSideScreenSectionOrder(next) } }
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
        val normalized = packageName?.takeIf { it.isNotBlank() }
        if (_selectedIconPackPackage.value == normalized) return
        _selectedIconPackPackage.value = normalized
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
        watchFaceRefreshJob?.cancel()
        watchFaceRefreshJob = viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                scanAllWatchFaces()
            }
            _availableWatchFaces.value = scanned
            LunchWatchFaceRegistry.update(scanned)
            watchFaceScanHydrated = true
            syncSelectedWatchFace(freshScanCompleted = true)
        }
    }

    suspend fun importDingDingCatWatchFace(uri: Uri): Result<LunchWatchFaceDescriptor> {
        return Result.failure(UnsupportedOperationException("公开版已移除旧版叮叮猫表盘导入"))
    }

    fun suggestedBackupFileName(): String = FlueBackupManager.suggestedFileName(getApplication())

    suspend fun exportBackup(uri: Uri, options: FlueBackupOptions): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                FlueBackupManager.exportBackup(getApplication(), uri, options)
            }
        }
    }

    suspend fun readBackupPreview(uri: Uri): Result<FlueBackupPreview> {
        return runCatching {
            withContext(Dispatchers.IO) {
                FlueBackupManager.readPreview(getApplication(), uri)
            }
        }
    }

    suspend fun importBackup(uri: Uri, options: FlueBackupOptions): Result<Unit> {
        return runCatching {
            val availableAppKeys = allApps.value.ifEmpty { apps.value }.map { it.componentKey }.toSet()
            withContext(Dispatchers.IO) {
                FlueBackupManager.importBackup(
                    context = getApplication(),
                    sourceUri = uri,
                    options = options,
                    availableAppKeys = availableAppKeys,
                    widgetRepository = repositories.widgetRepository
                )
            }
            refreshWatchFaces(force = true)
            refreshIconPacks()
            requestWatchFaceRefresh()
        }
    }

    suspend fun deleteDingDingCatWatchFace(id: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("公开版已移除旧版叮叮猫表盘"))
    }

    fun selectWatchFace(id: String) {
        _selectedWatchFaceId.value = id
        _watchFaceLastError.value = null
        syncSelectedWatchFace()
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        persist {
            store.edit {
                it[KEY_SELECTED_WATCHFACE_ID] = id
                it.remove(KEY_LAST_WATCHFACE_ERROR)
            }
        }
    }

    private fun scanAllWatchFaces(): List<LunchWatchFaceDescriptor> {
        val context = getApplication<Application>()
        return LunchWatchFaceScanner.builtInDescriptors() +
            LunchWatchFaceScanner.scanInstalled(context)
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
        val previousPath = _builtInPhotoPath.value
        _builtInPhotoPath.value = path
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        previousPath?.takeIf { it != path }?.let(WatchFacePhotoCache::remove)
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

    fun setBuiltInWatchFaceFontPath(path: String?) {
        _builtInWatchFaceFontPath.value = path?.takeIf { it.isNotBlank() }
        _watchFaceRefreshToken.value = _watchFaceRefreshToken.value + 1
        if (_builtInWatchFaceFontPath.value.isNullOrBlank()) {
            InternalWatchFaceStorage.clearFont(getApplication())
        }
        persist {
            store.edit {
                if (_builtInWatchFaceFontPath.value.isNullOrBlank()) {
                    it.remove(KEY_WATCHFACE_FONT_PATH)
                } else {
                    it[KEY_WATCHFACE_FONT_PATH] = _builtInWatchFaceFontPath.value!!
                }
            }
        }
    }

    fun setBuiltInPhotoClockStyle(style: WatchFaceClockStyle) {
        _builtInPhotoClockStyle.value = style
        persist { store.edit { it[KEY_PHOTO_CLOCK_STYLE] = style.name } }
    }

    fun setBuiltInVideoClockStyle(style: WatchFaceClockStyle) {
        _builtInVideoClockStyle.value = style
        persist { store.edit { it[KEY_VIDEO_CLOCK_STYLE] = style.name } }
    }

    fun setBuiltInPhotoMd3eShape(shape: WatchFaceMd3eShape) {
        _builtInPhotoMd3eShape.value = shape
        persist { store.edit { it[KEY_PHOTO_MD3E_SHAPE] = shape.name } }
    }

    fun setBuiltInVideoMd3eShape(shape: WatchFaceMd3eShape) {
        _builtInVideoMd3eShape.value = shape
        persist { store.edit { it[KEY_VIDEO_MD3E_SHAPE] = shape.name } }
    }

    fun setBuiltInPhotoUseThemeTextColor(enabled: Boolean) {
        _builtInPhotoUseThemeTextColor.value = enabled
        persist { store.edit { it[KEY_PHOTO_USE_THEME_TEXT_COLOR] = enabled } }
    }

    fun setBuiltInVideoUseThemeTextColor(enabled: Boolean) {
        _builtInVideoUseThemeTextColor.value = enabled
        persist { store.edit { it[KEY_VIDEO_USE_THEME_TEXT_COLOR] = enabled } }
    }

    fun setBuiltInPhotoTextColorArgb(argb: Int) {
        _builtInPhotoTextColorArgb.value = argb
        persistDebounced("photo_text_color") { store.edit { it[KEY_PHOTO_TEXT_COLOR] = argb } }
    }

    fun setBuiltInVideoTextColorArgb(argb: Int) {
        _builtInVideoTextColorArgb.value = argb
        persistDebounced("video_text_color") { store.edit { it[KEY_VIDEO_TEXT_COLOR] = argb } }
    }

    fun setBuiltInPhotoMd3eAutoColors(enabled: Boolean) {
        _builtInPhotoMd3eAutoColors.value = enabled
        persist { store.edit { it[KEY_PHOTO_MD3E_AUTO_COLORS] = enabled } }
    }

    fun setBuiltInVideoMd3eAutoColors(enabled: Boolean) {
        _builtInVideoMd3eAutoColors.value = enabled
        persist { store.edit { it[KEY_VIDEO_MD3E_AUTO_COLORS] = enabled } }
    }

    fun setBuiltInPhotoMd3eTextColorArgb(argb: Int) {
        _builtInPhotoMd3eTextColorArgb.value = argb
        persistDebounced("photo_md3e_text_color") { store.edit { it[KEY_PHOTO_MD3E_TEXT_COLOR] = argb } }
    }

    fun setBuiltInVideoMd3eTextColorArgb(argb: Int) {
        _builtInVideoMd3eTextColorArgb.value = argb
        persistDebounced("video_md3e_text_color") { store.edit { it[KEY_VIDEO_MD3E_TEXT_COLOR] = argb } }
    }

    fun setBuiltInPhotoMd3eFaceColorArgb(argb: Int) {
        _builtInPhotoMd3eFaceColorArgb.value = argb
        persistDebounced("photo_md3e_face_color") { store.edit { it[KEY_PHOTO_MD3E_FACE_COLOR] = argb } }
    }

    fun setBuiltInVideoMd3eFaceColorArgb(argb: Int) {
        _builtInVideoMd3eFaceColorArgb.value = argb
        persistDebounced("video_md3e_face_color") { store.edit { it[KEY_VIDEO_MD3E_FACE_COLOR] = argb } }
    }

    fun setBuiltInPhotoMd3eHourColorArgb(argb: Int) {
        _builtInPhotoMd3eHourColorArgb.value = argb
        persistDebounced("photo_md3e_hour_color") { store.edit { it[KEY_PHOTO_MD3E_HOUR_COLOR] = argb } }
    }

    fun setBuiltInVideoMd3eHourColorArgb(argb: Int) {
        _builtInVideoMd3eHourColorArgb.value = argb
        persistDebounced("video_md3e_hour_color") { store.edit { it[KEY_VIDEO_MD3E_HOUR_COLOR] = argb } }
    }

    fun setBuiltInPhotoMd3eMinuteColorArgb(argb: Int) {
        _builtInPhotoMd3eMinuteColorArgb.value = argb
        persistDebounced("photo_md3e_minute_color") { store.edit { it[KEY_PHOTO_MD3E_MINUTE_COLOR] = argb } }
    }

    fun setBuiltInVideoMd3eMinuteColorArgb(argb: Int) {
        _builtInVideoMd3eMinuteColorArgb.value = argb
        persistDebounced("video_md3e_minute_color") { store.edit { it[KEY_VIDEO_MD3E_MINUTE_COLOR] = argb } }
    }

    fun setBuiltInPhotoMd3eSecondColorArgb(argb: Int) {
        _builtInPhotoMd3eSecondColorArgb.value = argb
        persistDebounced("photo_md3e_second_color") { store.edit { it[KEY_PHOTO_MD3E_SECOND_COLOR] = argb } }
    }

    fun setBuiltInVideoMd3eSecondColorArgb(argb: Int) {
        _builtInVideoMd3eSecondColorArgb.value = argb
        persistDebounced("video_md3e_second_color") { store.edit { it[KEY_VIDEO_MD3E_SECOND_COLOR] = argb } }
    }

    fun setBuiltInPhotoShowSeconds(enabled: Boolean) {
        _builtInPhotoShowSeconds.value = enabled
        persist { store.edit { it[KEY_PHOTO_SHOW_SECONDS] = enabled } }
    }

    fun setBuiltInVideoShowSeconds(enabled: Boolean) {
        _builtInVideoShowSeconds.value = enabled
        persist { store.edit { it[KEY_VIDEO_SHOW_SECONDS] = enabled } }
    }

    fun setBuiltInPhotoCustomText(text: String) {
        val normalized = text.take(32)
        _builtInPhotoCustomText.value = normalized
        persistDebounced("photo_custom_text") {
            store.edit {
                if (normalized.isBlank()) it.remove(KEY_PHOTO_CUSTOM_TEXT)
                else it[KEY_PHOTO_CUSTOM_TEXT] = normalized
            }
        }
    }

    fun setBuiltInVideoCustomText(text: String) {
        val normalized = text.take(32)
        _builtInVideoCustomText.value = normalized
        persistDebounced("video_custom_text") {
            store.edit {
                if (normalized.isBlank()) it.remove(KEY_VIDEO_CUSTOM_TEXT)
                else it[KEY_VIDEO_CUSTOM_TEXT] = normalized
            }
        }
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
        if (slotIndex !in 0 until SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) return
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

    fun swapSideScreenShortcuts(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in 0 until SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY || toIndex !in 0 until SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) return
        val next = _sideScreenShortcuts.value.toMutableList()
        val item = next[fromIndex]
        next[fromIndex] = next[toIndex]
        next[toIndex] = item
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

    fun dismissAllNotifications() {
        val keys = _notificationGroups.value
            .flatMap(NotificationGroupUi::entries)
            .filter(NotificationEntryUi::isClearable)
            .map(NotificationEntryUi::key)
            .distinct()
        if (keys.isEmpty()) return
        markNotificationsDismissed(keys)
        WLauncherNotificationListener.dismissNotifications(keys)
        _expandedNotificationGroups.value = emptySet()
        _revealedNotificationTarget.value = null
    }

    fun runNotificationAction(actionKey: String): Boolean {
        if (actionKey.isBlank()) return false
        _revealedNotificationTarget.value = null
        return WLauncherNotificationListener.runNotificationAction(actionKey)
    }

    fun setRevealedNotificationTarget(target: NotificationRevealTarget?) {
        _revealedNotificationTarget.value = target
    }

    fun refreshNotificationAccess() {
        val application = getApplication<Application>()
        val component = ComponentName(application, WLauncherNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(
            application.contentResolver,
            "enabled_notification_listeners"
        )
        val granted = enabledListeners
            ?.split(':')
            ?.mapNotNull { ComponentName.unflattenFromString(it) }
            ?.any { it.packageName == component.packageName && it.className == component.className }
            ?: false
        _notificationAccessGranted.value = granted || WLauncherNotificationListener.isConnected()
        if (granted) {
            WLauncherNotificationListener.requestRebindIfNeeded(application)
        }
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
        _sideScreenShortcutRows.value = 2
        _sideScreenShortcutCols.value = 3
        _sideScreenShortcuts.value = List(SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) { null }
        _sideScreenWidgetSlots.value = emptyList()
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
        _honeycombEdgeBlurRadius.value = 4f
        _honeycombTopFade.value = 30
        _honeycombBottomFade.value = 30
        _honeycombFastScrollOptimization.value = true
        _appListFisheyeEnabled.value = true
        _appListFisheyeRangeRows.value = 4
        _appListFisheyeStrengthPercent.value = 100
        _appListEdgeSpacingCompressionEnabled.value = true
        _appListLeftSafeInsetPercent.value = 0
        _appListScalePercent.value = 100
        _globalUiScalePercent.value = 100
        _appListWatchFaceColors.value = true
        _appListRowBorderEnabled.value = false
        _appListFoldersEnabled.value = true
        _fastFlowAnimationEnabled.value = false
        _musicTextSwitchAnimation.value = MusicTextSwitchAnimations.DEFAULT_ID
        _twoToneIconsEnabled.value = false
        _iconShadowEnabled.value = true
        _classicReturnAnimationEnabled.value = false
        _showStepCount.value = true
        _showNotification.value = true
        _showOngoingNotifications.value = false
        _rotaryHapticsEnabled.value = true
        _showWidgets.value = true
        _showWidgetPage.value = true
        _showControlCenter.value = true
        _showMusicControls.value = true
        _doubleTapLockScreenEnabled.value = false
        _powerMenuButtonEnabled.value = false
        _watchFaceChargingPowerText.value = true
        _watchFaceStatusIndicators.value = true
        _watchFaceBottomFadeEnabled.value = true
        _dingDingCatFillScreen.value = false
        _dingDingCatPlaybackSpeedPercent.value = 100
        _dingDingCatImportUnlocked.value = false
        _sideScreenSectionOrder.value = defaultSideScreenSectionOrder()
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
        _builtInWatchFaceFontPath.value = null
        _builtInPhotoClockStyle.value = WatchFaceClockStyle.DIGITAL
        _builtInVideoClockStyle.value = WatchFaceClockStyle.DIGITAL
        _builtInPhotoMd3eShape.value = WatchFaceMd3eShape.COOKIE
        _builtInVideoMd3eShape.value = WatchFaceMd3eShape.COOKIE
        _builtInPhotoUseThemeTextColor.value = true
        _builtInVideoUseThemeTextColor.value = true
        _builtInPhotoTextColorArgb.value = 0xFFFFFFFF.toInt()
        _builtInVideoTextColorArgb.value = 0xFFFFFFFF.toInt()
        _builtInPhotoMd3eAutoColors.value = true
        _builtInVideoMd3eAutoColors.value = true
        _builtInPhotoMd3eTextColorArgb.value = 0xFF202938.toInt()
        _builtInVideoMd3eTextColorArgb.value = 0xFF202938.toInt()
        _builtInPhotoMd3eFaceColorArgb.value = 0xFFEAF1FF.toInt()
        _builtInVideoMd3eFaceColorArgb.value = 0xFFEAF1FF.toInt()
        _builtInPhotoMd3eHourColorArgb.value = 0xFF334155.toInt()
        _builtInVideoMd3eHourColorArgb.value = 0xFF334155.toInt()
        _builtInPhotoMd3eMinuteColorArgb.value = 0xFF5F84B6.toInt()
        _builtInVideoMd3eMinuteColorArgb.value = 0xFF5F84B6.toInt()
        _builtInPhotoMd3eSecondColorArgb.value = 0xFF806EA4.toInt()
        _builtInVideoMd3eSecondColorArgb.value = 0xFF806EA4.toInt()
        _builtInPhotoShowSeconds.value = false
        _builtInVideoShowSeconds.value = false
        _builtInPhotoCustomText.value = ""
        _builtInVideoCustomText.value = ""
        _builtInManagerThumbnails.value = true
        _hideFromRecents.value = true
        InternalWatchFaceStorage.clearFont(getApplication())
        appRepository.setHiddenComponents(emptySet())
        appRepository.setIconRenderingOptions(
            packageName = null,
            legacyCircular = true,
            twoTone = false,
            iconSize = 128
        )
        LunchWatchFaceRegistry.setCurrentSelectedId(BUILT_IN_WATCHFACE_ID)
        persist {
            store.edit {
                it[KEY_LAYOUT] = LayoutMode.Honeycomb.name
                it[KEY_SIDE_SCREEN_ENABLED] = true
                it[KEY_SIDE_SCREEN_SHORTCUT_ROWS] = 2
                it[KEY_SIDE_SCREEN_SHORTCUT_COLS] = 3
                it[KEY_BLUR] = defaultBlurEnabled
                it[KEY_EDGE_BLUR] = false
                it[KEY_LOW_RES] = false
                it[KEY_ANIMATION_OVERRIDE] = true
                it[KEY_SPLASH_ICON] = true
                it[KEY_SPLASH_DELAY] = 500
                it[KEY_HONEYCOMB_COLS] = 3
                it[KEY_HONEYCOMB_TOP_BLUR] = 4
                it[KEY_HONEYCOMB_BOTTOM_BLUR] = 4
                it[KEY_HONEYCOMB_EDGE_BLUR_TENTHS] = 40
                it[KEY_HONEYCOMB_TOP_FADE] = 30
                it[KEY_HONEYCOMB_BOTTOM_FADE] = 30
                it[KEY_HONEYCOMB_FAST_SCROLL_OPTIMIZATION] = true
                it[KEY_APP_LIST_FISHEYE] = true
                it[KEY_APP_LIST_FISHEYE_RANGE_ROWS] = 4
                it[KEY_APP_LIST_FISHEYE_STRENGTH_PERCENT] = 100
                it[KEY_APP_LIST_EDGE_SPACING_COMPRESSION] = true
                it[KEY_APP_LIST_LEFT_SAFE_INSET_PERCENT] = 0
                it[KEY_APP_LIST_SCALE_PERCENT] = 100
                it[KEY_GLOBAL_UI_SCALE_PERCENT] = 100
                it[KEY_APP_LIST_WATCHFACE_COLORS] = true
                it[KEY_APP_LIST_ROW_BORDER] = false
                it[KEY_APP_LIST_FOLDERS_ENABLED] = true
                it[KEY_FAST_FLOW_ANIMATION] = false
                it[KEY_MUSIC_TEXT_SWITCH_ANIMATION] = MusicTextSwitchAnimations.DEFAULT_ID
                it[KEY_TWO_TONE_ICONS] = false
                it[KEY_ICON_SHADOW] = true
                it[KEY_CLASSIC_RETURN_ANIMATION] = false
                it[KEY_SHOW_STEP_COUNT] = true
                it[KEY_LEGACY_CIRCULAR_ICONS] = true
                it[KEY_SHOW_NOTIFICATION] = true
                it[KEY_SHOW_ONGOING_NOTIFICATIONS] = false
                it[KEY_ROTARY_HAPTICS_ENABLED] = true
                it[KEY_SHOW_WIDGETS] = true
                it[KEY_SHOW_WIDGET_PAGE] = true
                it[KEY_SHOW_CONTROL_CENTER] = true
                it[KEY_SHOW_MUSIC_CONTROLS] = true
                it[KEY_DOUBLE_TAP_LOCK_SCREEN] = false
                it[KEY_POWER_MENU_BUTTON] = false
                it[KEY_WATCHFACE_CHARGING_POWER_TEXT] = true
                it[KEY_WATCHFACE_STATUS_INDICATORS] = true
                it[KEY_WATCHFACE_BOTTOM_FADE] = true
                it[KEY_DINGDINGCAT_FILL_SCREEN] = false
                it[KEY_DINGDINGCAT_PLAYBACK_SPEED_PERCENT] = 100
                it[KEY_DINGDINGCAT_IMPORT_UNLOCKED] = false
                it[KEY_SIDE_SCREEN_SECTION_ORDER] = serializeSideScreenSectionOrder(defaultSideScreenSectionOrder())
                it[KEY_NOTIFICATION_SETTING_MIGRATED] = true
                it.remove(KEY_SIDE_SCREEN_SHORTCUTS)
                it.remove(KEY_SIDE_SCREEN_WIDGETS)
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
                it.remove(KEY_WATCHFACE_FONT_PATH)
                it[KEY_PHOTO_CLOCK_STYLE] = WatchFaceClockStyle.DIGITAL.name
                it[KEY_VIDEO_CLOCK_STYLE] = WatchFaceClockStyle.DIGITAL.name
                it[KEY_PHOTO_MD3E_SHAPE] = WatchFaceMd3eShape.COOKIE.name
                it[KEY_VIDEO_MD3E_SHAPE] = WatchFaceMd3eShape.COOKIE.name
                it[KEY_PHOTO_USE_THEME_TEXT_COLOR] = true
                it[KEY_VIDEO_USE_THEME_TEXT_COLOR] = true
                it[KEY_PHOTO_TEXT_COLOR] = 0xFFFFFFFF.toInt()
                it[KEY_VIDEO_TEXT_COLOR] = 0xFFFFFFFF.toInt()
                it[KEY_PHOTO_MD3E_AUTO_COLORS] = true
                it[KEY_VIDEO_MD3E_AUTO_COLORS] = true
                it[KEY_PHOTO_MD3E_TEXT_COLOR] = 0xFF202938.toInt()
                it[KEY_VIDEO_MD3E_TEXT_COLOR] = 0xFF202938.toInt()
                it[KEY_PHOTO_MD3E_FACE_COLOR] = 0xFFEAF1FF.toInt()
                it[KEY_VIDEO_MD3E_FACE_COLOR] = 0xFFEAF1FF.toInt()
                it[KEY_PHOTO_MD3E_HOUR_COLOR] = 0xFF334155.toInt()
                it[KEY_VIDEO_MD3E_HOUR_COLOR] = 0xFF334155.toInt()
                it[KEY_PHOTO_MD3E_MINUTE_COLOR] = 0xFF5F84B6.toInt()
                it[KEY_VIDEO_MD3E_MINUTE_COLOR] = 0xFF5F84B6.toInt()
                it[KEY_PHOTO_MD3E_SECOND_COLOR] = 0xFF806EA4.toInt()
                it[KEY_VIDEO_MD3E_SECOND_COLOR] = 0xFF806EA4.toInt()
                it[KEY_PHOTO_SHOW_SECONDS] = false
                it[KEY_VIDEO_SHOW_SECONDS] = false
                it.remove(KEY_PHOTO_CUSTOM_TEXT)
                it.remove(KEY_VIDEO_CUSTOM_TEXT)
                it[KEY_BUILTIN_MANAGER_THUMBNAILS] = true
                it[KEY_HIDE_FROM_RECENTS] = true
                it.remove(KEY_LAST_WATCHFACE_ERROR)
            }
        }
    }

    private fun parseClockColorMode(raw: String): WatchClockColorMode {
        return runCatching { WatchClockColorMode.valueOf(raw) }.getOrDefault(WatchClockColorMode.AUTO)
    }

    private fun parseClockStyle(raw: String): WatchFaceClockStyle {
        return runCatching { WatchFaceClockStyle.valueOf(raw) }.getOrDefault(WatchFaceClockStyle.DIGITAL)
    }

    private fun parseMd3eShape(raw: String): WatchFaceMd3eShape {
        return runCatching { WatchFaceMd3eShape.valueOf(raw) }.getOrDefault(WatchFaceMd3eShape.COOKIE)
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

    private fun syncSelectedWatchFace(freshScanCompleted: Boolean = false) {
        val requestedId = _selectedWatchFaceId.value.ifBlank { BUILT_IN_WATCHFACE_ID }
        val available = _availableWatchFaces.value
        val match = available.firstOrNull { it.id == requestedId }
        var selectionResolved = true

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

        _watchFaceSelectionReady.value = watchFacePrefsHydrated && watchFaceScanHydrated && selectionResolved
    }

    private fun refreshIcons() {
        refreshIconsJob?.cancel()
        refreshIconsJob = viewModelScope.launch(Dispatchers.IO) {
            appRepository.requestRefresh(if (_lowResIcons.value) 64 else 128)
        }
    }

    private fun parseSideScreenShortcuts(raw: String?): List<String?> {
        val parts = raw
            ?.split('|')
            ?.map { it.ifBlank { null } }
            .orEmpty()
        return List(SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) { index -> parts.getOrNull(index) }
    }

    private fun serializeSideScreenShortcuts(shortcuts: List<String?>): String {
        val normalized = List(SIDE_SCREEN_SHORTCUT_SLOT_CAPACITY) { index ->
            shortcuts.getOrNull(index)?.takeIf { it.isNotBlank() } ?: ""
        }
        return if (normalized.all(String::isEmpty)) "" else normalized.joinToString("|")
    }

    private fun parseSideScreenWidgets(raw: String?): List<String?> {
        return raw
            ?.split(if (raw.contains('|')) '|' else ',')
            ?.mapNotNull { it.takeIf { value -> value.isNotBlank() } }
            .orEmpty()
    }

    private fun serializeSideScreenWidgets(widgets: List<String?>): String {
        return widgets
            .mapNotNull { value -> value?.takeIf { it.isNotBlank() } }
            .joinToString("|")
    }

    private fun defaultSideScreenSectionOrder(): List<SideScreenSection> {
        return listOf(SideScreenSection.Widgets, SideScreenSection.Notifications)
    }

    private fun parseSideScreenSectionOrder(raw: String?): List<SideScreenSection> {
        val parsed = raw
            ?.split(',')
            ?.mapNotNull { token ->
                SideScreenSection.entries.firstOrNull { section ->
                    section.prefValue.equals(token.trim(), ignoreCase = true) ||
                        section.name.equals(token.trim(), ignoreCase = true)
                }
            }
            .orEmpty()
        return normalizeSideScreenSectionOrder(parsed)
    }

    private fun normalizeSideScreenSectionOrder(order: List<SideScreenSection>): List<SideScreenSection> {
        val normalized = order.distinct().toMutableList()
        defaultSideScreenSectionOrder().forEach { section ->
            if (section !in normalized) normalized.add(section)
        }
        return normalized
    }

    private fun serializeSideScreenSectionOrder(order: List<SideScreenSection>): String {
        return normalizeSideScreenSectionOrder(order).joinToString(",") { it.prefValue }
    }

    private fun isXiaomi17ProSeriesDevice(): Boolean {
        val maker = listOf(Build.MANUFACTURER, Build.BRAND)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        if ("xiaomi" !in maker) return false
        return listOf(Build.MODEL, Build.DEVICE, Build.PRODUCT)
            .map { value ->
                value.lowercase(Locale.ROOT).replace(Regex("[\\s_\\-]+"), "")
            }
            .any { normalized ->
                normalized.contains("25098pn5ac") ||
                    normalized.contains("2509fpn0bc") ||
                    normalized.contains("xiaomi17pro") ||
                    normalized.contains("xiaomi17promax")
            }
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
                            isForegroundService = item.isForegroundService,
                            actions = item.actions.map { action ->
                                NotificationActionUi(
                                    key = action.key,
                                    title = action.title
                                )
                            }
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

    private fun shouldShowNotification(item: NotifData, showOngoing: Boolean): Boolean {
        if (item.isSystemHidden) return false
        if (item.isNoisyOngoing) return false
        return showOngoing || (!item.isOngoing && !item.isForegroundService)
    }

    override fun onCleared() {
        refreshIconsJob?.cancel()
        pendingNotificationClearJob?.cancel()
        pendingWriteJobs.values.forEach(Job::cancel)
        pendingWriteJobs.clear()
        super.onCleared()
    }
}
