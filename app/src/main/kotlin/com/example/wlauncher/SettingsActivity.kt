package com.flue.launcher

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dudu.wearlauncher.utils.ILog
import com.flue.launcher.backup.FlueBackupOptions
import com.flue.launcher.backup.FlueBackupPreview
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.service.FlueAccessibilityService
import com.flue.launcher.ui.common.bottomFisheyeScale
import com.flue.launcher.ui.controlcenter.MusicTextSwitchAnimations
import com.flue.launcher.ui.drawer.vibrateHaptic
import com.flue.launcher.ui.input.flueRotaryScrollable
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import com.flue.launcher.ui.navigation.LayoutMode
import com.flue.launcher.ui.settings.WatchFaceSettingCard
import com.flue.launcher.ui.theme.LauncherTheme
import com.flue.launcher.ui.theme.ProvideGlobalUiScale
import com.flue.launcher.ui.theme.ThemeMode
import com.flue.launcher.ui.theme.UiStyle
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.ui.theme.WatchLauncherTheme
import com.flue.launcher.util.applyFadeCloseTransition
import com.flue.launcher.util.applyFadeOpenTransition
import com.flue.launcher.util.RecentsVisibility
import com.flue.launcher.viewmodel.LauncherViewModel
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_VIDEO_WATCHFACE_ID
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.InternalWatchFaceStorage
import com.flue.launcher.watchface.LunchWatchFaceDescriptor
import com.flue.launcher.watchface.LunchWatchFaceRuntime
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val ABOUT_VERSION = "beta1.5"
private const val COMMUNITY_GROUP_NUMBER = "1097162313"
private const val AUTHOR_QQ_NUMBER = "3513903055"
private const val AUTHOR_BILIBILI_URL = "https://m.bilibili.com/space/1609437970"
private const val DIAGNOSTIC_LOG_DURATION_MS = 5 * 60 * 1000L
private const val COMMUNITY_GROUP_URL = "https://qun.qq.com/universal-share/share?ac=1&authKey=5CJC0tNLWsy3YWzGlPbqt%2F5kv%2BYZuJ8y8IVj%2B1UnIeLyR2DWR6QjWtM%2B4HXxH%2BKJ&busi_data=eyJncm91cENvZGUiOiIxMDk3MTYyMzEzIiwidG9rZW4iOiJlMGFBc1VpRXRROE1mS3J5SHgxRjFudmxXY0FuSnRQd2hOWWl6WllFMmlxNTErNWdadXJ5U1ozMmdzUSszaGNYIiwidWluIjoiMzUxMzkwMzA1NSJ9&data=YK52b80xUVwmcLWmSneKQMdVZodE4vTpsqmSb60ykXudbVCfa6AskMHxvqjhAjervNeE4exll-kQw5w-EifYOg&svctype=4&tempid=h5_group_info"
const val EXTRA_SETTINGS_DESTINATION = "settings_destination"
const val EXTRA_SETTINGS_RETURN_TO_FACE = "settings_return_to_face"
const val SETTINGS_DESTINATION_WATCH_FACES = "watch_faces"
private val LocalSettingsLeftSafeInsetPercent = compositionLocalOf { 0 }

enum class SettingsDestination {
    ROOT,
    WATCH_FACES,
    WATCH_FACE_MORE,
    HIDDEN_APPS,
    ICON_PACKS,
    APPEARANCE,
    DISPLAY_SIZE,
    FEATURE_TOGGLES,
    PERFORMANCE,
    MUSIC_TEXT_ANIMATION,
    TOOLS,
    BACKUP,
    BACKUP_EXPORT,
    BACKUP_IMPORT,
    ABOUT,
    DONATE
}

private data class SavedScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

private data class SettingsLaunchConfig(
    val initialDestination: SettingsDestination = SettingsDestination.ROOT,
    val returnToFaceOnBack: Boolean = false,
    val requestId: Long = 0L
)

private data class BlockingProgressState(
    val title: String,
    val detail: String,
    val completed: Boolean = false,
    val completionAction: BlockingProgressCompletionAction = BlockingProgressCompletionAction.None,
    val shareUri: Uri? = null
)

private enum class BlockingProgressCompletionAction {
    None,
    ExportBackup,
    ImportBackup,
    DingDingCatImport
}

class SettingsActivity : ComponentActivity() {
    private var launchRequestId = 0L
    private var launchConfig by mutableStateOf(SettingsLaunchConfig())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        launchConfig = intent.readSettingsLaunchConfig()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val uiStyle by viewModel.uiStyle.collectAsStateWithLifecycle()
            val globalUiScalePercent by viewModel.globalUiScalePercent.collectAsStateWithLifecycle()
            WatchLauncherTheme(themeMode = themeMode, uiStyle = uiStyle) {
                ProvideGlobalUiScale(globalUiScalePercent) {
                    SettingsRootScreen(
                        onFinish = { finish() },
                        launchConfig = launchConfig
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchConfig = intent.readSettingsLaunchConfig()
    }

    override fun finish() {
        super.finish()
        applyFadeCloseTransition()
    }

    private fun Intent.readSettingsLaunchConfig(): SettingsLaunchConfig {
        launchRequestId += 1
        val initialDestination = when (getStringExtra(EXTRA_SETTINGS_DESTINATION)) {
            SETTINGS_DESTINATION_WATCH_FACES -> SettingsDestination.WATCH_FACES
            else -> SettingsDestination.ROOT
        }
        return SettingsLaunchConfig(
            initialDestination = initialDestination,
            returnToFaceOnBack = getBooleanExtra(EXTRA_SETTINGS_RETURN_TO_FACE, false),
            requestId = launchRequestId
        )
    }
}

private fun initialSettingsBackStack(
    initialDestination: SettingsDestination,
    returnToFaceOnBack: Boolean
): List<SettingsDestination> {
    return if (initialDestination != SettingsDestination.ROOT && !returnToFaceOnBack) {
        listOf(SettingsDestination.ROOT)
    } else {
        emptyList()
    }
}

@Composable
private fun SettingsRootScreen(
    onFinish: () -> Unit,
    launchConfig: SettingsLaunchConfig = SettingsLaunchConfig()
) {
    val context = LocalContext.current
    val settingsScope = rememberCoroutineScope()
    val vm: LauncherViewModel = viewModel()
    val watchFaces by vm.availableWatchFaces.collectAsStateWithLifecycle()
    val selectedWatchFaceId by vm.selectedWatchFaceId.collectAsStateWithLifecycle()
    val selectedWatchFace by vm.selectedWatchFace.collectAsStateWithLifecycle()
    val allApps by vm.allApps.collectAsStateWithLifecycle()
    val hiddenApps by vm.hiddenApps.collectAsStateWithLifecycle()
    val availableIconPacks by vm.availableIconPacks.collectAsStateWithLifecycle()
    val selectedIconPackPackage by vm.selectedIconPackPackage.collectAsStateWithLifecycle()
    val watchFaceLastError by vm.watchFaceLastError.collectAsStateWithLifecycle()
    val layoutMode by vm.layoutMode.collectAsStateWithLifecycle()
    val sideScreenEnabled by vm.sideScreenEnabled.collectAsStateWithLifecycle()
    val blurEnabled by vm.blurEnabled.collectAsStateWithLifecycle()
    val edgeBlurEnabled by vm.edgeBlurEnabled.collectAsStateWithLifecycle()
    val lowResIcons by vm.lowResIcons.collectAsStateWithLifecycle()
    val animationOverrideEnabled by vm.animationOverrideEnabled.collectAsStateWithLifecycle()
    val splashIcon by vm.splashIcon.collectAsStateWithLifecycle()
    val splashDelay by vm.splashDelay.collectAsStateWithLifecycle()
    val honeycombCols by vm.honeycombCols.collectAsStateWithLifecycle()
    val honeycombEdgeBlurRadius by vm.honeycombEdgeBlurRadius.collectAsStateWithLifecycle()
    val honeycombTopFade by vm.honeycombTopFade.collectAsStateWithLifecycle()
    val honeycombBottomFade by vm.honeycombBottomFade.collectAsStateWithLifecycle()
    val honeycombFastScrollOptimization by vm.honeycombFastScrollOptimization.collectAsStateWithLifecycle()
    val appListFisheyeEnabled by vm.appListFisheyeEnabled.collectAsStateWithLifecycle()
    val appListFisheyeRangeRows by vm.appListFisheyeRangeRows.collectAsStateWithLifecycle()
    val appListFisheyeStrengthPercent by vm.appListFisheyeStrengthPercent.collectAsStateWithLifecycle()
    val appListEdgeSpacingCompressionEnabled by vm.appListEdgeSpacingCompressionEnabled.collectAsStateWithLifecycle()
    val appListLeftSafeInsetPercent by vm.appListLeftSafeInsetPercent.collectAsStateWithLifecycle()
    val appListScalePercent by vm.appListScalePercent.collectAsStateWithLifecycle()
    val globalUiScalePercent by vm.globalUiScalePercent.collectAsStateWithLifecycle()
    val appListWatchFaceColors by vm.appListWatchFaceColors.collectAsStateWithLifecycle()
    val appListRowBorderEnabled by vm.appListRowBorderEnabled.collectAsStateWithLifecycle()
    val appListFoldersEnabled by vm.appListFoldersEnabled.collectAsStateWithLifecycle()
    val fastFlowAnimationEnabled by vm.fastFlowAnimationEnabled.collectAsStateWithLifecycle()
    val musicTextSwitchAnimation by vm.musicTextSwitchAnimation.collectAsStateWithLifecycle()
    val twoToneIconsEnabled by vm.twoToneIconsEnabled.collectAsStateWithLifecycle()
    val iconShadowEnabled by vm.iconShadowEnabled.collectAsStateWithLifecycle()
    val classicReturnAnimationEnabled by vm.classicReturnAnimationEnabled.collectAsStateWithLifecycle()
    val showStepCount by vm.showStepCount.collectAsStateWithLifecycle()
    val builtInPhotoPath by vm.builtInPhotoPath.collectAsStateWithLifecycle()
    val builtInVideoPath by vm.builtInVideoPath.collectAsStateWithLifecycle()
    val builtInPhotoClockPosition by vm.builtInPhotoClockPosition.collectAsStateWithLifecycle()
    val builtInVideoClockPosition by vm.builtInVideoClockPosition.collectAsStateWithLifecycle()
    val builtInPhotoClockSize by vm.builtInPhotoClockSize.collectAsStateWithLifecycle()
    val builtInVideoClockSize by vm.builtInVideoClockSize.collectAsStateWithLifecycle()
    val builtInPhotoClockBold by vm.builtInPhotoClockBold.collectAsStateWithLifecycle()
    val builtInVideoClockBold by vm.builtInVideoClockBold.collectAsStateWithLifecycle()
    val builtInVideoFillScreen by vm.builtInVideoFillScreen.collectAsStateWithLifecycle()
    val builtInVideoClockColorMode by vm.builtInVideoClockColorMode.collectAsStateWithLifecycle()
    val builtInWatchFaceFontPath by vm.builtInWatchFaceFontPath.collectAsStateWithLifecycle()
    val builtInPhotoClockStyle by vm.builtInPhotoClockStyle.collectAsStateWithLifecycle()
    val builtInVideoClockStyle by vm.builtInVideoClockStyle.collectAsStateWithLifecycle()
    val builtInPhotoMd3eShape by vm.builtInPhotoMd3eShape.collectAsStateWithLifecycle()
    val builtInVideoMd3eShape by vm.builtInVideoMd3eShape.collectAsStateWithLifecycle()
    val builtInPhotoUseThemeTextColor by vm.builtInPhotoUseThemeTextColor.collectAsStateWithLifecycle()
    val builtInVideoUseThemeTextColor by vm.builtInVideoUseThemeTextColor.collectAsStateWithLifecycle()
    val builtInPhotoTextColorArgb by vm.builtInPhotoTextColorArgb.collectAsStateWithLifecycle()
    val builtInVideoTextColorArgb by vm.builtInVideoTextColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoMd3eAutoColors by vm.builtInPhotoMd3eAutoColors.collectAsStateWithLifecycle()
    val builtInVideoMd3eAutoColors by vm.builtInVideoMd3eAutoColors.collectAsStateWithLifecycle()
    val builtInPhotoMd3eTextColorArgb by vm.builtInPhotoMd3eTextColorArgb.collectAsStateWithLifecycle()
    val builtInVideoMd3eTextColorArgb by vm.builtInVideoMd3eTextColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoMd3eFaceColorArgb by vm.builtInPhotoMd3eFaceColorArgb.collectAsStateWithLifecycle()
    val builtInVideoMd3eFaceColorArgb by vm.builtInVideoMd3eFaceColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoMd3eHourColorArgb by vm.builtInPhotoMd3eHourColorArgb.collectAsStateWithLifecycle()
    val builtInVideoMd3eHourColorArgb by vm.builtInVideoMd3eHourColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoMd3eMinuteColorArgb by vm.builtInPhotoMd3eMinuteColorArgb.collectAsStateWithLifecycle()
    val builtInVideoMd3eMinuteColorArgb by vm.builtInVideoMd3eMinuteColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoMd3eSecondColorArgb by vm.builtInPhotoMd3eSecondColorArgb.collectAsStateWithLifecycle()
    val builtInVideoMd3eSecondColorArgb by vm.builtInVideoMd3eSecondColorArgb.collectAsStateWithLifecycle()
    val builtInPhotoShowSeconds by vm.builtInPhotoShowSeconds.collectAsStateWithLifecycle()
    val builtInVideoShowSeconds by vm.builtInVideoShowSeconds.collectAsStateWithLifecycle()
    val builtInPhotoCustomText by vm.builtInPhotoCustomText.collectAsStateWithLifecycle()
    val builtInVideoCustomText by vm.builtInVideoCustomText.collectAsStateWithLifecycle()
    val builtInManagerThumbnails by vm.builtInManagerThumbnails.collectAsStateWithLifecycle()
    val hideFromRecents by vm.hideFromRecents.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val uiStyle by vm.uiStyle.collectAsStateWithLifecycle()
    val launcherStyle = LauncherTheme.style
    val showNotification by vm.showNotification.collectAsStateWithLifecycle()
    val showOngoingNotifications by vm.showOngoingNotifications.collectAsStateWithLifecycle()
    val rotaryHapticsEnabled by vm.rotaryHapticsEnabled.collectAsStateWithLifecycle()
    val showWidgetPage by vm.showWidgetPage.collectAsStateWithLifecycle()
    val showControlCenter by vm.showControlCenter.collectAsStateWithLifecycle()
    val showMusicControls by vm.showMusicControls.collectAsStateWithLifecycle()
    val doubleTapLockScreenEnabled by vm.doubleTapLockScreenEnabled.collectAsStateWithLifecycle()
    val powerMenuButtonEnabled by vm.powerMenuButtonEnabled.collectAsStateWithLifecycle()
    val watchFaceChargingPowerText by vm.watchFaceChargingPowerText.collectAsStateWithLifecycle()
    val watchFaceStatusIndicators by vm.watchFaceStatusIndicators.collectAsStateWithLifecycle()
    val watchFaceBottomFadeEnabled by vm.watchFaceBottomFadeEnabled.collectAsStateWithLifecycle()
    val dingDingCatFillScreen by vm.dingDingCatFillScreen.collectAsStateWithLifecycle()
    val dingDingCatPlaybackSpeedPercent by vm.dingDingCatPlaybackSpeedPercent.collectAsStateWithLifecycle()
    val dingDingCatImportUnlocked by vm.dingDingCatImportUnlocked.collectAsStateWithLifecycle()
    val accessibilityServiceConnected by FlueAccessibilityService.connected.collectAsStateWithLifecycle()
    val headerTime = rememberSettingsHeaderTime()
    val isZh = remember(context.resources.configuration) {
        context.resources.configuration.locales[0]?.language?.startsWith("zh") == true
    }
    var backupExportOptions by remember { mutableStateOf(FlueBackupOptions()) }
    var pendingBackupExportOptions by remember { mutableStateOf<FlueBackupOptions?>(null) }
    var backupImportUri by remember { mutableStateOf<Uri?>(null) }
    var backupImportPreview by remember { mutableStateOf<FlueBackupPreview?>(null) }
    var backupImportOptions by remember { mutableStateOf(FlueBackupOptions()) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf<BlockingProgressState?>(null) }
    val dingDingCatBackupAvailability = remember {
        FlueBackupOptions(dingDingCatWatchFaces = false)
    }
    val effectiveBackupExportOptions = remember(backupExportOptions, dingDingCatBackupAvailability) {
        backupExportOptions.constrainedBy(dingDingCatBackupAvailability)
    }
    val effectiveBackupImportPreview = remember(backupImportPreview, dingDingCatBackupAvailability) {
        backupImportPreview?.let { preview ->
            preview.copy(availableOptions = preview.availableOptions.constrainedBy(dingDingCatBackupAvailability))
        }
    }
    val effectiveBackupImportOptions = remember(backupImportOptions, effectiveBackupImportPreview) {
        val available = effectiveBackupImportPreview?.availableOptions ?: FlueBackupOptions(dingDingCatWatchFaces = false)
        backupImportOptions.constrainedBy(available)
    }
    val fontPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            val savedPath = InternalWatchFaceStorage.copyFont(context, uri)
            if (savedPath.isNullOrBlank()) {
                Toast.makeText(context, "保存字体失败", Toast.LENGTH_SHORT).show()
            } else {
                vm.setBuiltInWatchFaceFontPath(savedPath)
                Toast.makeText(context, "表盘字体已更新", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val dingDingCatZipPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            settingsScope.launch {
                backupBusy = true
                backupProgress = BlockingProgressState(
                    title = "导入叮叮猫表盘",
                    detail = "正在解包和解析 ZIP"
                )
                val result = vm.importDingDingCatWatchFace(uri)
                result
                    .onSuccess { descriptor ->
                        backupProgress = BlockingProgressState(
                            title = "导入完成",
                            detail = descriptor.displayName,
                            completed = true,
                            completionAction = BlockingProgressCompletionAction.DingDingCatImport
                        )
                    }
                    .onFailure { error ->
                        backupProgress = null
                        Toast.makeText(
                            context,
                            "导入失败：${error.message ?: error.javaClass.simpleName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                backupBusy = false
            }
        }
    }
    val backupCreatePicker = rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
        val options = pendingBackupExportOptions
        pendingBackupExportOptions = null
        if (uri != null && options != null) {
            settingsScope.launch {
                backupBusy = true
                backupProgress = BlockingProgressState(
                    title = "导出配置",
                    detail = "正在写入 Flue 备份 ZIP"
                )
                val result = vm.exportBackup(uri, options)
                result
                    .onSuccess {
                        backupProgress = BlockingProgressState(
                            title = "导出完成",
                            detail = uri.backupDisplayLabel(),
                            completed = true,
                            completionAction = BlockingProgressCompletionAction.ExportBackup,
                            shareUri = uri
                        )
                    }
                    .onFailure { error ->
                        backupProgress = null
                        Toast.makeText(
                            context,
                            "导出失败：${error.message ?: error.javaClass.simpleName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                backupBusy = false
            }
        }
    }
    val backupOpenPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            settingsScope.launch {
                backupBusy = true
                backupProgress = BlockingProgressState(
                    title = "读取配置",
                    detail = "正在检查备份 ZIP"
                )
                val result = vm.readBackupPreview(uri)
                result
                    .onSuccess { preview ->
                        backupImportUri = uri
                        backupImportPreview = preview
                        backupImportOptions = preview.availableOptions.constrainedBy(dingDingCatBackupAvailability)
                    }
                    .onFailure { error ->
                        Toast.makeText(
                            context,
                            "读取失败：${error.message ?: error.javaClass.simpleName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                backupBusy = false
                backupProgress = null
            }
        }
    }
    val openAccessibilitySettings = remember(context) {
        {
            Toast.makeText(context, "请在无障碍中授权 Flue 辅助控制", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
            Unit
        }
    }

    var destination by remember { mutableStateOf(launchConfig.initialDestination) }
    var backStack by remember {
        mutableStateOf(initialSettingsBackStack(launchConfig.initialDestination, launchConfig.returnToFaceOnBack))
    }
    var hiddenAppsDraft by remember { mutableStateOf(hiddenApps) }
    var hiddenAppsDirty by remember { mutableStateOf(false) }
    var donatePreviewResId by remember { mutableStateOf<Int?>(null) }
    var diagnosticLoggingEnabled by remember { mutableStateOf(ILog.isDiagnosticLoggingEnabled()) }
    var dingDingCatVersionTapCount by remember { mutableStateOf(0) }
    var dingDingCatLastVersionTapAt by remember { mutableStateOf(0L) }
    var dingDingCatDeleteTarget by remember { mutableStateOf<LunchWatchFaceDescriptor?>(null) }
    val pageScrollPositions = remember { mutableStateMapOf<SettingsDestination, SavedScrollPosition>() }
    val pageScrollResetVersions = remember { mutableStateMapOf<SettingsDestination, Int>() }

    fun resetPageScroll(target: SettingsDestination) {
        pageScrollPositions.remove(target)
        pageScrollResetVersions[target] = (pageScrollResetVersions[target] ?: 0) + 1
    }

    fun closeBlockingProgressPage() {
        val state = backupProgress
        backupProgress = null
        backupBusy = false
        if (state?.completionAction == BlockingProgressCompletionAction.DingDingCatImport) {
            pageScrollPositions.remove(SettingsDestination.WATCH_FACES)
            destination = SettingsDestination.WATCH_FACES
            if (backStack.isEmpty() && !launchConfig.returnToFaceOnBack) {
                backStack = listOf(SettingsDestination.ROOT)
            }
            settingsScope.launch {
                vm.refreshWatchFaces(force = true)
            }
        }
    }

    dingDingCatDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { dingDingCatDeleteTarget = null },
            title = { Text("删除表盘") },
            text = { Text("删除 ${target.displayName}？这个叮叮猫 ZIP 表盘会从 Flue 私有目录移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dingDingCatDeleteTarget = null
                        settingsScope.launch {
                            val result = vm.deleteDingDingCatWatchFace(target.id)
                            result
                                .onSuccess {
                                    Toast.makeText(context, "已删除 ${target.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        "删除失败：${error.message ?: error.javaClass.simpleName}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { dingDingCatDeleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }

    backupProgress?.let { state ->
        BlockingProgressDialog(
            state = state,
            onDismiss = { closeBlockingProgressPage() },
            onShare = { state.shareUri?.let { shareBackupUri(context, it) } }
        )
    }

    LaunchedEffect(hiddenApps) {
        if (!hiddenAppsDirty) {
            hiddenAppsDraft = hiddenApps
        }
    }

    LaunchedEffect(Unit) {
        vm.refreshWatchFaces()
    }

    LaunchedEffect(hideFromRecents, context) {
        (context as? Activity)?.let { RecentsVisibility.apply(it, hideFromRecents) }
    }

    val commitHiddenAppsDraft = {
        if (hiddenAppsDirty) {
            vm.setHiddenApps(hiddenAppsDraft)
            hiddenAppsDirty = false
        }
    }

    LaunchedEffect(launchConfig.requestId) {
        if (destination == SettingsDestination.HIDDEN_APPS) {
            commitHiddenAppsDraft()
        }
        destination = launchConfig.initialDestination
        backStack = initialSettingsBackStack(launchConfig.initialDestination, launchConfig.returnToFaceOnBack)
        donatePreviewResId = null
    }

    fun leaveDestination(next: SettingsDestination) {
        if (destination == SettingsDestination.HIDDEN_APPS && next != SettingsDestination.HIDDEN_APPS) {
            commitHiddenAppsDraft()
        }
    }

    val navigateTo: (SettingsDestination) -> Unit = { next ->
        if (next != destination) {
            if (next.resetsScrollOnEnter || next.depth > destination.depth) {
                resetPageScroll(next)
            }
            leaveDestination(next)
            backStack = if (backStack.lastOrNull() == next) {
                backStack.dropLast(1)
            } else {
                backStack + destination
            }
            destination = next
        }
    }

    val handleBack: () -> Unit = handleBack@{
        if (backupProgress?.completed == true) {
            closeBlockingProgressPage()
            return@handleBack
        }
        if (backupBusy) {
            return@handleBack
        }
        if (donatePreviewResId != null) {
            donatePreviewResId = null
            return@handleBack
        }
        val previous = backStack.lastOrNull()
        if (previous != null) {
            leaveDestination(previous)
            backStack = backStack.dropLast(1)
            destination = previous
        } else {
            if (destination == SettingsDestination.HIDDEN_APPS) {
                commitHiddenAppsDraft()
            }
            onFinish()
        }
    }

    BackHandler(enabled = true) { handleBack() }

    val selectedIconPackLabel = availableIconPacks.firstOrNull { it.packageName == selectedIconPackPackage }?.label
    val scrollFor: (SettingsDestination) -> SavedScrollPosition = { target ->
        pageScrollPositions[target] ?: SavedScrollPosition()
    }
    val updateScroll: (SettingsDestination, Int, Int) -> Unit = { target, index, offset ->
        if (!target.resetsScrollOnEnter) {
            pageScrollPositions[target] = SavedScrollPosition(index = index, offset = offset)
        }
    }

    CompositionLocalProvider(LocalSettingsLeftSafeInsetPercent provides appListLeftSafeInsetPercent) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                (fadeIn(animationSpec = launcherStyle.pageMotionSpec) +
                    scaleIn(
                        initialScale = if (uiStyle == UiStyle.MATERIAL_3) 0.98f else 0.985f,
                        animationSpec = launcherStyle.pageMotionSpec
                    )) togetherWith
                    (fadeOut(animationSpec = tween(140)) +
                        scaleOut(
                            targetScale = if (uiStyle == UiStyle.MATERIAL_3) 0.995f else 0.985f,
                            animationSpec = tween(140)
                        ))
            },
            label = "settings_destination"
        ) { currentDestination ->
        key(currentDestination, pageScrollResetVersions[currentDestination] ?: 0) {
        when (currentDestination) {
            SettingsDestination.ROOT -> SettingsPageScaffold(
                title = "Flue设置",
                onBack = {
                    commitHiddenAppsDraft()
                    onFinish()
                },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.ROOT).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.ROOT).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.ROOT, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("watchfaces") {
                    SettingsCategoryCard(
                        title = "表盘",
                        subtitle = if (uiStyle == UiStyle.MATERIAL_3) "MD3 主页与表盘图形已联动 · ${selectedWatchFace.displayName}" else selectedWatchFace.displayName,
                        onClick = { navigateTo(SettingsDestination.WATCH_FACES) },
                        scale = itemFisheye(listState, "watchfaces", screenCenterY, screenHeightPx)
                    )
                }
                item("appearance") {
                    SettingsCategoryCard(
                        title = "显示与外观",
                        subtitle = if (uiStyle == UiStyle.MATERIAL_3) "布局、材质层次与 MD3 动效" else "布局、模糊与启动图标",
                        onClick = { navigateTo(SettingsDestination.APPEARANCE) },
                        scale = itemFisheye(listState, "appearance", screenCenterY, screenHeightPx)
                    )
                }
                item("hidden_apps") {
                    SettingsCategoryCard(
                        title = "隐藏应用",
                        subtitle = "已隐藏 ${hiddenAppsDraft.size} 个应用",
                        onClick = { navigateTo(SettingsDestination.HIDDEN_APPS) },
                        scale = itemFisheye(listState, "hidden_apps", screenCenterY, screenHeightPx)
                    )
                }
                item("icon_packs") {
                    SettingsCategoryCard(
                        title = "图标包",
                        subtitle = selectedIconPackLabel ?: "系统默认图标",
                        onClick = { navigateTo(SettingsDestination.ICON_PACKS) },
                        scale = itemFisheye(listState, "icon_packs", screenCenterY, screenHeightPx)
                    )
                }
                item("performance") {
                    SettingsCategoryCard(
                        title = "性能与动画",
                        subtitle = "图标质量与动画控制",
                        onClick = { navigateTo(SettingsDestination.PERFORMANCE) },
                        scale = itemFisheye(listState, "performance", screenCenterY, screenHeightPx)
                    )
                }
                item("tools") {
                    SettingsCategoryCard(
                        title = "工具",
                        subtitle = "配置备份、导出日志与恢复默认",
                        onClick = { navigateTo(SettingsDestination.TOOLS) },
                        scale = itemFisheye(listState, "tools", screenCenterY, screenHeightPx)
                    )
                }
                item("about") {
                    SettingsCategoryCard(
                        title = "关于",
                        subtitle = "Flue  $ABOUT_VERSION",
                        onClick = { navigateTo(SettingsDestination.ABOUT) },
                        scale = itemFisheye(listState, "about", screenCenterY, screenHeightPx)
                    )
                }
            }

            SettingsDestination.HIDDEN_APPS -> SettingsPageScaffold(
                title = "隐藏应用",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.HIDDEN_APPS).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.HIDDEN_APPS).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.HIDDEN_APPS, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, visibleItemKeys ->
                item("hidden_summary") {
                    MessageCard(
                        text = "已隐藏 ${hiddenAppsDraft.size} 个应用",
                        background = WatchColors.SurfaceGlass,
                        onClick = {}
                    )
                }
                items(allApps.filterNot(AppInfo::isBuiltInSettingsEntry), key = { "app_${it.componentKey}" }) { app ->
                    SettingsSwitchRow(
                        title = app.label,
                        subtitle = app.packageName,
                        checked = hiddenAppsDraft.contains(app.componentKey) || hiddenAppsDraft.contains(app.packageName),
                        onToggle = {
                            hiddenAppsDraft = hiddenAppsDraft.toMutableSet().apply {
                                if (it) add(app.componentKey) else remove(app.componentKey)
                            }
                            hiddenAppsDirty = true
                        },
                        scale = itemFisheye(listState, "app_${app.componentKey}", screenCenterY, screenHeightPx),
                        leadingIcon = app.cachedIcon.takeIf { visibleItemKeys.contains("app_${app.componentKey}") },
                        reserveLeadingIconSpace = true
                    )
                }
            }

            SettingsDestination.ICON_PACKS -> SettingsPageScaffold(
                title = "图标包",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.ICON_PACKS).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.ICON_PACKS).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.ICON_PACKS, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("icon_pack_default") {
                    SettingsChoiceRow(
                        title = "系统默认",
                        subtitle = "使用当前系统默认图标包",
                        selected = selectedIconPackPackage.isNullOrBlank(),
                        onClick = { vm.setIconPackPackage(null) },
                        scale = itemFisheye(listState, "icon_pack_default", screenCenterY, screenHeightPx)
                    )
                }
                items(availableIconPacks, key = { "iconpack_${it.packageName}" }) { pack ->
                    SettingsChoiceRow(
                        title = pack.label,
                        subtitle = "ADW 图标包",
                        selected = pack.packageName == selectedIconPackPackage,
                        onClick = { vm.setIconPackPackage(pack.packageName) },
                        scale = itemFisheye(listState, "iconpack_${pack.packageName}", screenCenterY, screenHeightPx)
                    )
                }
                item("icon_pack_refresh") {
                    ActionCard(
                        title = "刷新图标包",
                        subtitle = "重新扫描已安装的 ADW 图标包",
                        icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = WatchColors.ActiveCyan) },
                        onClick = { vm.refreshIconPacks() },
                        scale = itemFisheye(listState, "icon_pack_refresh", screenCenterY, screenHeightPx)
                    )
                }
            }

            SettingsDestination.WATCH_FACES -> SettingsPageScaffold(
            title = "表盘",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.WATCH_FACES).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.WATCH_FACES).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.WATCH_FACES, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            if (!watchFaceLastError.isNullOrBlank()) {
                item("watchface_error") {
                    MessageCard(
                        text = watchFaceLastError!!,
                        background = Color(0x33FF6B6B),
                        onClick = { vm.clearWatchFaceError() }
                    )
                }
            }
            items(watchFaces, key = { it.id }) { descriptor ->
                WatchFaceSettingCard(
                    descriptor = descriptor,
                    selected = descriptor.id == selectedWatchFaceId,
                    uiStyle = uiStyle,
                    builtInPhotoPath = builtInPhotoPath,
                    builtInVideoPath = builtInVideoPath,
                    photoOptions = BuiltInWatchFaceOptions(
                        clockPosition = builtInPhotoClockPosition,
                        clockSizeSp = builtInPhotoClockSize,
                        boldClock = builtInPhotoClockBold,
                        clockStyle = builtInPhotoClockStyle,
                        showSeconds = builtInPhotoShowSeconds,
                        customText = builtInPhotoCustomText,
                        fontPath = builtInWatchFaceFontPath,
                        md3eShape = builtInPhotoMd3eShape,
                        useThemeTextColor = builtInPhotoUseThemeTextColor,
                        textColorArgb = builtInPhotoTextColorArgb,
                        md3eAutoColors = builtInPhotoMd3eAutoColors,
                        md3eTextColorArgb = builtInPhotoMd3eTextColorArgb,
                        md3eFaceColorArgb = builtInPhotoMd3eFaceColorArgb,
                        md3eHourHandColorArgb = builtInPhotoMd3eHourColorArgb,
                        md3eMinuteHandColorArgb = builtInPhotoMd3eMinuteColorArgb,
                        md3eSecondHandColorArgb = builtInPhotoMd3eSecondColorArgb
                    ),
                    videoOptions = BuiltInWatchFaceOptions(
                        clockPosition = builtInVideoClockPosition,
                        clockSizeSp = builtInVideoClockSize,
                        boldClock = builtInVideoClockBold,
                        cropToFill = builtInVideoFillScreen,
                        clockColorMode = builtInVideoClockColorMode,
                        clockStyle = builtInVideoClockStyle,
                        showSeconds = builtInVideoShowSeconds,
                        customText = builtInVideoCustomText,
                        fontPath = builtInWatchFaceFontPath,
                        md3eShape = builtInVideoMd3eShape,
                        useThemeTextColor = builtInVideoUseThemeTextColor,
                        textColorArgb = builtInVideoTextColorArgb,
                        md3eAutoColors = builtInVideoMd3eAutoColors,
                        md3eTextColorArgb = builtInVideoMd3eTextColorArgb,
                        md3eFaceColorArgb = builtInVideoMd3eFaceColorArgb,
                        md3eHourHandColorArgb = builtInVideoMd3eHourColorArgb,
                        md3eMinuteHandColorArgb = builtInVideoMd3eMinuteColorArgb,
                        md3eSecondHandColorArgb = builtInVideoMd3eSecondColorArgb
                    ),
                    onSelect = { vm.selectWatchFace(descriptor.id) },
                    onOpenSettings = if (descriptor.supportsSettings) {
                        {
                            if (descriptor.isBuiltin && descriptor.id in setOf(BUILT_IN_PHOTO_WATCHFACE_ID, BUILT_IN_VIDEO_WATCHFACE_ID)) {
                                context.startActivity(
                                    Intent(context, InternalWatchFaceConfigActivity::class.java)
                                        .putExtra(EXTRA_INTERNAL_WATCHFACE_ID, descriptor.id)
                                )
                                (context as? Activity)?.applyFadeOpenTransition()
                            } else if (!LunchWatchFaceRuntime.openSettings(context, descriptor)) {
                                Toast.makeText(context, "没有可用的表盘设置", Toast.LENGTH_SHORT).show()
                            } else {
                                (context as? Activity)?.applyFadeOpenTransition()
                            }
                        }
                    } else {
                        null
                    },
                    onDelete = null,
                    scale = itemFisheye(listState, descriptor.id, screenCenterY, screenHeightPx)
                )
            }
            item("watchface_refresh") {
                ActionCard(
                    title = "重新扫描表盘",
                    subtitle = "刷新已安装的 Lunch 兼容表盘",
                    scale = itemFisheye(listState, "watchface_refresh", screenCenterY, screenHeightPx),
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = WatchColors.ActiveCyan) },
                    onClick = { vm.refreshWatchFaces(force = true) },
                    onLongPress = {},
                    longPressDurationMs = 2_000L
                )
            }
            item("watchface_more") {
                SettingsCategoryCard(
                    title = "更多表盘选项",
                    subtitle = "字体和底部渐隐等表盘选项",
                    onClick = { navigateTo(SettingsDestination.WATCH_FACE_MORE) },
                    scale = itemFisheye(listState, "watchface_more", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.WATCH_FACE_MORE -> SettingsPageScaffold(
            title = "更多表盘选项",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.WATCH_FACE_MORE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.WATCH_FACE_MORE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.WATCH_FACE_MORE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            if (selectedWatchFace.isBuiltin) {
                item("watchface_font") {
                    ActionCard(
                        title = "表盘字体",
                        subtitle = builtInWatchFaceFontPath?.let { File(it).name } ?: "系统默认字体",
                        scale = itemFisheye(listState, "watchface_font", screenCenterY, screenHeightPx),
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = WatchColors.ActiveCyan) },
                        onClick = {
                            fontPicker.launch(
                                arrayOf(
                                    "font/*",
                                    "application/x-font-ttf",
                                    "application/x-font-otf",
                                    "application/vnd.ms-opentype",
                                    "application/octet-stream"
                                )
                            )
                        }
                    )
                }
                if (!builtInWatchFaceFontPath.isNullOrBlank()) {
                    item("watchface_font_clear") {
                        ActionCard(
                            title = "恢复默认字体",
                            subtitle = "清除已保存的表盘字体",
                            scale = itemFisheye(listState, "watchface_font_clear", screenCenterY, screenHeightPx),
                            icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = WatchColors.ActiveCyan) },
                            onClick = { vm.setBuiltInWatchFaceFontPath(null) }
                        )
                    }
                }
            }
            item("watchface_bottom_fade") {
                SettingsSwitchRow(
                    title = if (isZh) "表盘底部渐隐遮罩" else "Watch Face Bottom Fade",
                    subtitle = if (watchFaceBottomFadeEnabled) {
                        if (isZh) "AW 和 MD3 表盘底部保留渐隐过渡" else "Keep the bottom fade on AW and MD3 watch faces"
                    } else {
                        if (isZh) "关闭表盘底部渐隐过渡" else "Disable the bottom watch face fade"
                    },
                    checked = watchFaceBottomFadeEnabled,
                    onToggle = vm::setWatchFaceBottomFadeEnabled,
                    scale = itemFisheye(listState, "watchface_bottom_fade", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.APPEARANCE -> SettingsPageScaffold(
            title = "显示与外观",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.APPEARANCE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.APPEARANCE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.APPEARANCE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("layout_header") { SectionTitle("布局", itemFisheye(listState, "layout_header", screenCenterY, screenHeightPx)) }
            item("layout_honeycomb") {
                SettingsChoiceRow(
                    title = "蜂窝布局",
                    subtitle = "Apple Watch 风格",
                    selected = layoutMode == LayoutMode.Honeycomb,
                    onClick = { vm.setLayoutMode(LayoutMode.Honeycomb) },
                    scale = itemFisheye(listState, "layout_honeycomb", screenCenterY, screenHeightPx)
                )
            }
            item("layout_list") {
                SettingsChoiceRow(
                    title = "列表布局",
                    subtitle = "经典纵向列表",
                    selected = layoutMode == LayoutMode.List,
                    onClick = { vm.setLayoutMode(LayoutMode.List) },
                    scale = itemFisheye(listState, "layout_list", screenCenterY, screenHeightPx)
                )
            }
            item("display_header") {
                SectionTitle(if (isZh) "显示设置" else "Display", itemFisheye(listState, "display_header", screenCenterY, screenHeightPx))
            }
            item("display_size_menu") {
                SettingsCategoryCard(
                    title = if (isZh) "显示适配" else "Display Fit",
                    subtitle = if (isZh) "全局 UI、应用列表大小和安全区" else "Global UI, app list size, and safe area",
                    onClick = { navigateTo(SettingsDestination.DISPLAY_SIZE) },
                    scale = itemFisheye(listState, "display_size_menu", screenCenterY, screenHeightPx)
                )
            }
            item("feature_toggles_menu") {
                SettingsCategoryCard(
                    title = if (isZh) "功能开关" else "Feature Toggles",
                    subtitle = if (isZh) "副一屏、通知、控制中心和表盘提示" else "Side screen, notifications, Control Center, and watch face indicators",
                    onClick = { navigateTo(SettingsDestination.FEATURE_TOGGLES) },
                    scale = itemFisheye(listState, "feature_toggles_menu", screenCenterY, screenHeightPx)
                )
            }
            item("theme_mode_system") {
                SettingsChoiceRow(
                    title = if (isZh) "跟随系统" else "System",
                    subtitle = if (isZh) "跟随系统主题设置" else "Follow system theme setting",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { vm.setThemeMode(ThemeMode.SYSTEM) },
                    scale = itemFisheye(listState, "theme_mode_system", screenCenterY, screenHeightPx)
                )
            }
            item("theme_mode_dark") {
                SettingsChoiceRow(
                    title = if (isZh) "深色模式" else "Dark",
                    subtitle = if (isZh) "强制使用深色主题" else "Force dark theme",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { vm.setThemeMode(ThemeMode.DARK) },
                    scale = itemFisheye(listState, "theme_mode_dark", screenCenterY, screenHeightPx)
                )
            }
            item("theme_mode_light") {
                SettingsChoiceRow(
                    title = if (isZh) "浅色模式" else "Light",
                    subtitle = if (isZh) "强制使用浅色主题" else "Force light theme",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { vm.setThemeMode(ThemeMode.LIGHT) },
                    scale = itemFisheye(listState, "theme_mode_light", screenCenterY, screenHeightPx)
                )
            }
            item("ui_style_header") {
                SectionTitle(if (isZh) "界面风格" else "Interface Style", itemFisheye(listState, "ui_style_header", screenCenterY, screenHeightPx))
            }
            item("ui_style_apple_watch") {
                SettingsChoiceRow(
                    title = if (isZh) "Apple Watch 风格" else "Apple Watch",
                    subtitle = if (isZh) "玻璃质感、紧凑卡片、圆形承载与更轻巧的缩放动效" else "Glass layers, compact cards, circular carriers, and tighter motion",
                    selected = uiStyle == UiStyle.APPLE_WATCH,
                    onClick = { vm.setUiStyle(UiStyle.APPLE_WATCH) },
                    scale = itemFisheye(listState, "ui_style_apple_watch", screenCenterY, screenHeightPx)
                )
            }
            item("ui_style_material_3") {
                SettingsChoiceRow(
                    title = if (isZh) "Material 3 风格" else "Material 3",
                    subtitle = if (isZh) "大色块平铺、MD3E 图形、分层更明确的首页与列表" else "Bigger blocks, MD3E geometry, and stronger layered surfaces",
                    selected = uiStyle == UiStyle.MATERIAL_3,
                    onClick = { vm.setUiStyle(UiStyle.MATERIAL_3) },
                    scale = itemFisheye(listState, "ui_style_material_3", screenCenterY, screenHeightPx)
                )
            }
            item("visual_effects_header") {
                SectionTitle(if (isZh) "视觉效果" else "Visual Effects", itemFisheye(listState, "visual_effects_header", screenCenterY, screenHeightPx))
            }
            item("blur_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "高级材质" else "Blur",
                    subtitle = if (isZh) "建议 Android 12+ 设备开启" else "Enable blur on supported Android versions",
                    checked = blurEnabled,
                    onToggle = vm::setBlurEnabled,
                    scale = itemFisheye(listState, "blur_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("edge_blur_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "边缘模糊（实验性）" else "Edge Blur (Experimental)",
                    subtitle = if (isZh) "在顶部和底部边缘增加模糊，可能带来少量问题" else "Apply extra blur near the top and bottom edges",
                    checked = edgeBlurEnabled,
                    onToggle = vm::setEdgeBlurEnabled,
                    scale = itemFisheye(listState, "edge_blur_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("edge_blur_radius") {
                SettingsSliderRow(
                    title = if (isZh) "边缘模糊半径" else "Edge Blur Radius",
                    value = honeycombEdgeBlurRadius,
                    valueText = "%.1f dp".format(java.util.Locale.US, honeycombEdgeBlurRadius),
                    range = 0.5f..5f,
                    steps = 8,
                    onValueChange = vm::setHoneycombEdgeBlurRadius,
                    enabled = blurEnabled && edgeBlurEnabled,
                    scale = itemFisheye(listState, "edge_blur_radius", screenCenterY, screenHeightPx)
                )
            }
            item("top_fade") {
                SettingsSliderRow(
                    title = "顶部渐隐范围",
                    value = honeycombTopFade.toFloat(),
                    valueText = "$honeycombTopFade dp",
                    range = 0f..160f,
                    steps = 15,
                    onValueChange = { vm.setHoneycombTopFade(it.toInt()) },
                    scale = itemFisheye(listState, "top_fade", screenCenterY, screenHeightPx)
                )
            }
            item("bottom_fade") {
                SettingsSliderRow(
                    title = "底部渐隐范围",
                    value = honeycombBottomFade.toFloat(),
                    valueText = "$honeycombBottomFade dp",
                    range = 0f..160f,
                    steps = 15,
                    onValueChange = { vm.setHoneycombBottomFade(it.toInt()) },
                    scale = itemFisheye(listState, "bottom_fade", screenCenterY, screenHeightPx)
                )
            }
            item("launch_header") { SectionTitle("启动", itemFisheye(listState, "launch_header", screenCenterY, screenHeightPx)) }
            item("splash_toggle") {
                SettingsSwitchRow(
                    title = "启动遮罩",
                    subtitle = "打开应用时显示图标过渡",
                    checked = splashIcon,
                    onToggle = { vm.setSplashIcon(it) },
                    scale = itemFisheye(listState, "splash_toggle", screenCenterY, screenHeightPx)
                )
            }
            if (splashIcon) {
                item("splash_delay") {
                    SettingsSliderRow(
                        title = "遮罩时长",
                        value = splashDelay.toFloat(),
                        valueText = "${splashDelay} ms",
                        range = 300f..1500f,
                        steps = 11,
                        onValueChange = { vm.setSplashDelay(it.toInt()) },
                        scale = itemFisheye(listState, "splash_delay", screenCenterY, screenHeightPx)
                    )
                }
            }
        }

            SettingsDestination.DISPLAY_SIZE -> SettingsPageScaffold(
            title = if (isZh) "显示适配" else "Display Fit",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.DISPLAY_SIZE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.DISPLAY_SIZE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.DISPLAY_SIZE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("global_ui_scale") {
                SettingsSliderRow(
                    title = if (isZh) "全局 UI 大小" else "Global UI Size",
                    value = globalUiScalePercent.toFloat(),
                    valueText = "${globalUiScalePercent}%",
                    localValueText = { "${it.roundToInt()}%" },
                    range = 50f..150f,
                    steps = 9,
                    onValueChange = {},
                    onValueChangeFinished = { vm.setGlobalUiScalePercent(it.roundToInt()) },
                    scale = itemFisheye(listState, "global_ui_scale", screenCenterY, screenHeightPx)
                )
            }
            item("honeycomb_cols") {
                SettingsSliderRow(
                    title = if (isZh) "蜂窝列数" else "Honeycomb Columns",
                    value = honeycombCols.toFloat(),
                    valueText = if (isZh) "$honeycombCols 列" else "$honeycombCols columns",
                    range = 1f..5f,
                    steps = 3,
                    onValueChange = { vm.setHoneycombCols(it.roundToInt()) },
                    scale = itemFisheye(listState, "honeycomb_cols", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_scale") {
                SettingsSliderRow(
                    title = if (isZh) "应用列表整体大小" else "App List Size",
                    value = appListScalePercent.toFloat(),
                    valueText = "${appListScalePercent}%",
                    range = 50f..200f,
                    steps = 14,
                    onValueChange = { vm.setAppListScalePercent(it.toInt()) },
                    scale = itemFisheye(listState, "app_list_scale", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_fisheye") {
                SettingsSwitchRow(
                    title = if (isZh) "鱼眼效果" else "Fisheye Effect",
                    subtitle = if (appListFisheyeEnabled) {
                        if (isZh) "开启后可调节范围、强度和边缘间距压缩" else "Enable range, strength, and edge spacing controls"
                    } else {
                        if (isZh) "关闭后应用列表不再缩放边缘图标" else "Disable edge scaling for the app list"
                    },
                    checked = appListFisheyeEnabled,
                    onToggle = vm::setAppListFisheyeEnabled,
                    scale = itemFisheye(listState, "app_list_fisheye", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_fisheye_range_rows") {
                SettingsSliderRow(
                    title = if (isZh) "鱼眼生效范围" else "Fisheye Range",
                    value = appListFisheyeRangeRows.toFloat(),
                    valueText = if (isZh) "$appListFisheyeRangeRows 行" else "$appListFisheyeRangeRows rows",
                    localValueText = {
                        val rows = it.roundToInt()
                        if (isZh) "$rows 行" else "$rows rows"
                    },
                    range = 1f..8f,
                    steps = 6,
                    onValueChange = { vm.setAppListFisheyeRangeRows(it.roundToInt()) },
                    enabled = appListFisheyeEnabled,
                    scale = itemFisheye(listState, "app_list_fisheye_range_rows", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_fisheye_strength") {
                SettingsSliderRow(
                    title = if (isZh) "鱼眼效果强度" else "Fisheye Strength",
                    value = appListFisheyeStrengthPercent.toFloat(),
                    valueText = "${appListFisheyeStrengthPercent}%",
                    localValueText = { "${it.roundToInt()}%" },
                    range = 0f..200f,
                    steps = 19,
                    onValueChange = { vm.setAppListFisheyeStrengthPercent(it.roundToInt()) },
                    enabled = appListFisheyeEnabled,
                    scale = itemFisheye(listState, "app_list_fisheye_strength", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_edge_spacing_compression") {
                SettingsSwitchRow(
                    title = if (isZh) "边缘图标间距压缩" else "Edge Icon Spacing",
                    subtitle = if (isZh) "越靠近屏幕边界，图标之间的视觉距离越小" else "Compress visual spacing as icons approach the screen edge",
                    checked = appListEdgeSpacingCompressionEnabled,
                    enabled = appListFisheyeEnabled,
                    onToggle = vm::setAppListEdgeSpacingCompressionEnabled,
                    scale = itemFisheye(listState, "app_list_edge_spacing_compression", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_left_safe_inset") {
                SettingsSliderRow(
                    title = if (isZh) "应用列表左侧安全区" else "App List Left Safe Area",
                    value = appListLeftSafeInsetPercent.toFloat(),
                    valueText = "${appListLeftSafeInsetPercent}%",
                    localValueText = { "${((it / 5f).roundToInt() * 5).coerceIn(0, 50)}%" },
                    range = 0f..50f,
                    steps = 9,
                    onValueChange = { vm.setAppListLeftSafeInsetPercent((it / 5f).roundToInt() * 5) },
                    scale = itemFisheye(listState, "app_list_left_safe_inset", screenCenterY, screenHeightPx)
                )
            }
            item("rotary_haptics") {
                SettingsSwitchRow(
                    title = if (isZh) "表冠滚动震动" else "Rotary Haptics",
                    subtitle = if (rotaryHapticsEnabled) {
                        if (isZh) "表冠滚动时保留轻微震动反馈" else "Keep a light pulse while scrolling with the crown"
                    } else {
                        if (isZh) "关闭表冠滚动震动反馈" else "Disable crown scroll haptics"
                    },
                    checked = rotaryHapticsEnabled,
                    onToggle = vm::setRotaryHapticsEnabled,
                    scale = itemFisheye(listState, "rotary_haptics", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.FEATURE_TOGGLES -> SettingsPageScaffold(
            title = if (isZh) "功能开关" else "Feature Toggles",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.FEATURE_TOGGLES).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.FEATURE_TOGGLES).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.FEATURE_TOGGLES, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("side_screen_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "副一屏" else "Side Screen",
                    subtitle = if (isZh) "从表盘右滑进入快捷启动和通知预览" else "Swipe right from the watch face to open quick launch and notification previews",
                    checked = sideScreenEnabled,
                    onToggle = vm::setSideScreenEnabled,
                    scale = itemFisheye(listState, "side_screen_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("notification_center_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "显示通知" else "Show Notifications",
                    subtitle = if (showNotification) {
                        if (isZh) "通知预览会显示在副一屏快捷启动下方" else "Show notification previews below quick launch on the side screen"
                    } else {
                        if (isZh) "隐藏副一屏里的通知列表" else "Hide notifications from the side screen"
                    },
                    checked = showNotification,
                    enabled = sideScreenEnabled,
                    onToggle = vm::setShowNotification,
                    scale = itemFisheye(listState, "notification_center_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("ongoing_notification_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "显示常驻通知" else "Show Ongoing Notifications",
                    subtitle = if (showOngoingNotifications) {
                        if (isZh) "显示有内容价值的常驻通知，过滤系统运行和悬浮窗提示" else "Show useful ongoing notifications while filtering system noise"
                    } else {
                        if (isZh) "隐藏常驻通知" else "Hide ongoing notifications"
                    },
                    checked = showOngoingNotifications,
                    enabled = sideScreenEnabled && showNotification,
                    onToggle = vm::setShowOngoingNotifications,
                    scale = itemFisheye(listState, "ongoing_notification_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("widget_center_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "显示小组件页" else "Show Widget Page",
                    subtitle = if (showWidgetPage) {
                        if (isZh) "从表盘左滑进入独立小组件页面" else "Swipe left from the watch face to open the widget page"
                    } else {
                        if (isZh) "关闭左滑小组件页面入口" else "Disable the swipe-left widget page"
                    },
                    checked = showWidgetPage,
                    onToggle = vm::setShowWidgetPage,
                    scale = itemFisheye(listState, "widget_center_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("control_center_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "显示控制中心" else "Show Control Center",
                    subtitle = if (showControlCenter) {
                        if (isZh) "从表盘下拉进入亮度、音量和快捷开关" else "Pull down from the watch face for brightness, volume, and quick toggles"
                    } else {
                        if (isZh) "关闭表盘下拉控制中心入口" else "Disable the pull-down control center"
                    },
                    checked = showControlCenter,
                    onToggle = vm::setShowControlCenter,
                    scale = itemFisheye(listState, "control_center_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("music_controls_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "音乐控制卡片" else "Music Controls",
                    subtitle = if (showMusicControls) {
                        if (isZh) "控制中心显示当前媒体播放卡片" else "Show the media card in Control Center"
                    } else {
                        if (isZh) "隐藏控制中心里的音乐控制卡片" else "Hide the media card from Control Center"
                    },
                    checked = showMusicControls,
                    enabled = showControlCenter,
                    onToggle = vm::setShowMusicControls,
                    scale = itemFisheye(listState, "music_controls_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("double_tap_lock_screen") {
                SettingsSwitchRow(
                    title = if (isZh) "双击表盘息屏" else "Double Tap to Lock",
                    subtitle = if (accessibilityServiceConnected) {
                        if (isZh) "已授权无障碍，双击表盘会立即息屏" else "Accessibility is enabled; double tap the watch face to lock the screen"
                    } else {
                        if (isZh) "开启后跳转无障碍授权，仅用于双击息屏" else "Requires Accessibility; used only for double-tap screen lock"
                    },
                    checked = doubleTapLockScreenEnabled,
                    onToggle = { enabled ->
                        vm.setDoubleTapLockScreenEnabled(enabled)
                        if (enabled && !accessibilityServiceConnected) {
                            openAccessibilitySettings()
                        }
                    },
                    scale = itemFisheye(listState, "double_tap_lock_screen", screenCenterY, screenHeightPx)
                )
            }
            item("power_menu_button") {
                SettingsSwitchRow(
                    title = if (isZh) "控制中心电源按钮" else "Control Center Power Button",
                    subtitle = if (accessibilityServiceConnected) {
                        if (isZh) "已授权无障碍，点击控制中心电源图标会打开电源选项" else "Accessibility is enabled; the power icon opens power options"
                    } else {
                        if (isZh) "开启后跳转无障碍授权，仅用于打开电源选项" else "Requires Accessibility; used only to open power options"
                    },
                    checked = powerMenuButtonEnabled,
                    enabled = true,
                    onToggle = { enabled ->
                        vm.setPowerMenuButtonEnabled(enabled)
                        if (enabled && !accessibilityServiceConnected) {
                            openAccessibilitySettings()
                        }
                    },
                    scale = itemFisheye(listState, "power_menu_button", screenCenterY, screenHeightPx)
                )
            }
            item("watchface_status_indicators") {
                SettingsSwitchRow(
                    title = if (isZh) "表盘状态提示" else "Watch Face Status Indicators",
                    subtitle = if (isZh) "表盘顶部显示充电闪电和通知红点" else "Show charging and notification indicators at the top of the watch face",
                    checked = watchFaceStatusIndicators,
                    onToggle = vm::setWatchFaceStatusIndicatorsEnabled,
                    scale = itemFisheye(listState, "watchface_status_indicators", screenCenterY, screenHeightPx)
                )
            }
            item("watchface_charging_power_text") {
                SettingsSwitchRow(
                    title = if (isZh) "充电功率文字" else "Charging Power Text",
                    subtitle = if (isZh) "充电时在表盘里显示功率或充电文字" else "Show charging wattage or charging text on the watch face",
                    checked = watchFaceChargingPowerText,
                    onToggle = vm::setWatchFaceChargingPowerTextEnabled,
                    scale = itemFisheye(listState, "watchface_charging_power_text", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_watchface_colors") {
                SettingsSwitchRow(
                    title = if (isZh) "应用列表表盘取色" else "Watch Face App List Colors",
                    subtitle = if (appListWatchFaceColors) {
                        if (isZh) "应用列表背景继续跟随表盘颜色" else "Keep the app list tinted by the watch face"
                    } else {
                        if (isZh) "应用列表背景固定为黑白，图标遮罩保留系统取色" else "Use black or white background while keeping system-tinted icon masks"
                    },
                    checked = appListWatchFaceColors,
                    onToggle = vm::setAppListWatchFaceColors,
                    scale = itemFisheye(listState, "app_list_watchface_colors", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_row_border") {
                SettingsSwitchRow(
                    title = if (isZh) "列表模式底色遮罩" else "List Row Background",
                    subtitle = if (appListRowBorderEnabled) {
                        if (isZh) "列表布局中每排应用显示底色遮罩" else "Show the tinted background behind each list row"
                    } else {
                        if (isZh) "列表布局中不显示每排应用底色遮罩" else "Hide the list row background tint"
                    },
                    checked = appListRowBorderEnabled,
                    enabled = layoutMode == LayoutMode.List,
                    onToggle = vm::setAppListRowBorderEnabled,
                    scale = itemFisheye(listState, "app_list_row_border", screenCenterY, screenHeightPx)
                )
            }
            item("app_list_folders_enabled") {
                SettingsSwitchRow(
                    title = if (isZh) "允许创建文件夹（实验性）" else "Allow Folders (Experimental)",
                    subtitle = if (appListFoldersEnabled) {
                        if (isZh) "长按拖动到另一个图标中心并停留 700ms 后合成文件夹" else "Hold a dragged icon over another icon center for 700ms to create a folder"
                    } else {
                        if (isZh) "关闭拖动合成文件夹，只保留排序手势" else "Disable drag-to-folder creation and keep reordering only"
                    },
                    checked = appListFoldersEnabled,
                    onToggle = vm::setAppListFoldersEnabled,
                    scale = itemFisheye(listState, "app_list_folders_enabled", screenCenterY, screenHeightPx)
                )
            }
            item("show_step_count") {
                SettingsSwitchRow(
                    title = if (isZh) "显示步数" else "Show Step Count",
                    subtitle = if (isZh) "在侧屏底部电池右侧显示步数" else "Show steps to the right of battery on side screen",
                    checked = showStepCount,
                    onToggle = vm::setShowStepCountEnabled,
                    scale = itemFisheye(listState, "show_step_count", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.PERFORMANCE -> SettingsPageScaffold(
            title = "性能与动画",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.PERFORMANCE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.PERFORMANCE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.PERFORMANCE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("low_res") {
                SettingsSwitchRow(
                    title = "低分辨率图标",
                    subtitle = "降低图标开销以提升流畅度",
                    checked = lowResIcons,
                    onToggle = { vm.setLowResIcons(it) },
                    scale = itemFisheye(listState, "low_res", screenCenterY, screenHeightPx)
                )
            }
            item("honeycomb_fast_scroll_opt") {
                SettingsSwitchRow(
                    title = "滑动流畅度优化",
                    subtitle = "开启后大幅改善滑动流畅度",
                    checked = honeycombFastScrollOptimization,
                    onToggle = { vm.setHoneycombFastScrollOptimization(it) },
                    scale = itemFisheye(listState, "honeycomb_fast_scroll_opt", screenCenterY, screenHeightPx)
                )
            }
            item("fast_flow_animation") {
                SettingsSwitchRow(
                    title = if (isZh) "快速流逝动画" else "Fast Flow Animation",
                    subtitle = if (isZh) "仅作用于蜂窝模式，越远离屏幕中心滚动越快" else "Honeycomb only; icons farther from center move faster while scrolling",
                    checked = fastFlowAnimationEnabled,
                    onToggle = vm::setFastFlowAnimationEnabled,
                    scale = itemFisheye(listState, "fast_flow_animation", screenCenterY, screenHeightPx)
                )
            }
            item("music_text_animation") {
                val selectedPreset = MusicTextSwitchAnimations.byId(musicTextSwitchAnimation)
                SettingsCategoryCard(
                    title = "音乐文字切换动画",
                    subtitle = selectedPreset.label,
                    onClick = { navigateTo(SettingsDestination.MUSIC_TEXT_ANIMATION) },
                    scale = itemFisheye(listState, "music_text_animation", screenCenterY, screenHeightPx)
                )
            }
            item("two_tone_icons") {
                SettingsSwitchRow(
                    title = if (isZh) "双色图标" else "Two-Tone Icons",
                    subtitle = if (isZh) "AdaptiveIcon 使用单色层，其他图标转灰度后填色" else "Use adaptive monochrome when available, otherwise grayscale + tint",
                    checked = twoToneIconsEnabled,
                    onToggle = vm::setTwoToneIconsEnabled,
                    scale = itemFisheye(listState, "two_tone_icons", screenCenterY, screenHeightPx)
                )
            }
            item("icon_shadow") {
                SettingsSwitchRow(
                    title = if (isZh) "图标阴影" else "Icon Shadow",
                    subtitle = if (isZh) "关闭后减少蜂窝与列表拖动时的阴影绘制" else "Reduce shadow drawing for app icons and drag previews",
                    checked = iconShadowEnabled,
                    onToggle = vm::setIconShadowEnabled,
                    scale = itemFisheye(listState, "icon_shadow", screenCenterY, screenHeightPx)
                )
            }
            item("anim_override") {
                SettingsSwitchRow(
                    title = "桌面返回动画",
                    subtitle = "启用渐隐过渡",
                    checked = animationOverrideEnabled,
                    onToggle = { vm.setAnimationOverrideEnabled(it) },
                    scale = itemFisheye(listState, "anim_override", screenCenterY, screenHeightPx)
                )
            }
            item("classic_return_animation") {
                SettingsSwitchRow(
                    title = if (isZh) "经典返回动画" else "Classic Return Animation",
                    subtitle = if (classicReturnAnimationEnabled) {
                        if (isZh) "返回应用列表时从屏幕中间缩放" else "Return to the app list from the screen center"
                    } else {
                        if (isZh) "返回应用列表时跟随应用图标位置缩放" else "Return to the app list from the launched app icon"
                    },
                    checked = classicReturnAnimationEnabled,
                    onToggle = vm::setClassicReturnAnimationEnabled,
                    scale = itemFisheye(listState, "classic_return_animation", screenCenterY, screenHeightPx)
                )
            }
            item("hide_from_recents") {
                SettingsSwitchRow(
                    title = if (isZh) "隐藏后台卡片" else "Hide Recents Card",
                    subtitle = if (isZh) "从最近任务中隐藏 Flue 的后台卡片" else "Hide Flue from the system recents screen",
                    checked = hideFromRecents,
                    onToggle = vm::setHideFromRecents,
                    scale = itemFisheye(listState, "hide_from_recents", screenCenterY, screenHeightPx)
                )
            }
            item("builtin_manager_thumbnails") {
                SettingsSwitchRow(
                    title = "内置管理器缩略图",
                    subtitle = "在图片/视频列表左侧显示预览图",
                    checked = builtInManagerThumbnails,
                    onToggle = { vm.setBuiltInManagerThumbnails(it) },
                    scale = itemFisheye(listState, "builtin_manager_thumbnails", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.MUSIC_TEXT_ANIMATION -> SettingsPageScaffold(
            title = "音乐文字切换动画",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.MUSIC_TEXT_ANIMATION).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.MUSIC_TEXT_ANIMATION).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.MUSIC_TEXT_ANIMATION, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            items(MusicTextSwitchAnimations.presets, key = { "music_text_anim_${it.id}" }) { preset ->
                SettingsChoiceRow(
                    title = preset.label,
                    subtitle = preset.subtitle,
                    selected = MusicTextSwitchAnimations.normalizeId(musicTextSwitchAnimation) == preset.id,
                    onClick = { vm.setMusicTextSwitchAnimation(preset.id) },
                    scale = itemFisheye(listState, "music_text_anim_${preset.id}", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.TOOLS -> SettingsPageScaffold(
            title = "工具",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.TOOLS).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.TOOLS).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.TOOLS, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("backup_menu") {
                SettingsCategoryCard(
                    title = "导入/导出配置",
                    subtitle = "选择备份项目、导入项目和表盘资源",
                    onClick = { navigateTo(SettingsDestination.BACKUP) },
                    scale = itemFisheye(listState, "backup_menu", screenCenterY, screenHeightPx)
                )
            }
            item("export_log") {
                ActionCard(
                    title = "导出日志",
                    subtitle = "导出 Flue 摘要、崩溃文件和最近 500 行系统日志",
                    onClick = { settingsScope.launch { exportLog(context) } },
                    scale = itemFisheye(listState, "export_log", screenCenterY, screenHeightPx)
                )
            }
            item("diagnostic_logging") {
                SettingsSwitchRow(
                    title = "诊断日志模式",
                    subtitle = if (diagnosticLoggingEnabled) "已开启，5 分钟后自动关闭" else "默认关闭，开启后临时允许 release 输出诊断日志",
                    checked = diagnosticLoggingEnabled,
                    onToggle = { enabled ->
                        diagnosticLoggingEnabled = enabled
                        ILog.setDiagnosticLoggingEnabled(context, enabled, DIAGNOSTIC_LOG_DURATION_MS)
                    },
                    scale = itemFisheye(listState, "diagnostic_logging", screenCenterY, screenHeightPx)
                )
            }
            item("reset_defaults") {
                ActionCard(
                    title = "恢复默认设置",
                    subtitle = "重置桌面外观与性能选项",
                    onClick = { vm.resetSettings() },
                    scale = itemFisheye(listState, "reset_defaults", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.BACKUP -> SettingsPageScaffold(
            title = "导入/导出配置",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.BACKUP).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.BACKUP).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.BACKUP, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("backup_export_page") {
                SettingsCategoryCard(
                    title = "导出配置",
                    subtitle = "导出设置、排序和表盘资源为 ZIP",
                    onClick = { navigateTo(SettingsDestination.BACKUP_EXPORT) },
                    scale = itemFisheye(listState, "backup_export_page", screenCenterY, screenHeightPx)
                )
            }
            item("backup_import_page") {
                SettingsCategoryCard(
                    title = "导入配置",
                    subtitle = "选择 Flue ZIP 备份并按项目恢复",
                    onClick = { navigateTo(SettingsDestination.BACKUP_IMPORT) },
                    scale = itemFisheye(listState, "backup_import_page", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.BACKUP_EXPORT -> SettingsPageScaffold(
            title = "导出配置",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.BACKUP_EXPORT).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.BACKUP_EXPORT).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.BACKUP_EXPORT, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            backupOptionRows(
                listState = listState,
                screenCenterY = screenCenterY,
                screenHeightPx = screenHeightPx,
                options = effectiveBackupExportOptions,
                availableOptions = dingDingCatBackupAvailability,
                onOptionsChange = { backupExportOptions = it.constrainedBy(dingDingCatBackupAvailability) }
            )
            item("export_backup_start") {
                ActionCard(
                    title = "开始导出",
                    subtitle = "选择保存位置后写入 ZIP 备份",
                    onClick = {
                        if (!effectiveBackupExportOptions.hasAny) {
                            Toast.makeText(context, "至少选择一项", Toast.LENGTH_SHORT).show()
                        } else if (!backupBusy) {
                            pendingBackupExportOptions = effectiveBackupExportOptions
                            backupCreatePicker.launch(vm.suggestedBackupFileName())
                        }
                    },
                    scale = itemFisheye(listState, "export_backup_start", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.BACKUP_IMPORT -> SettingsPageScaffold(
            title = "导入配置",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.BACKUP_IMPORT).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.BACKUP_IMPORT).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.BACKUP_IMPORT, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("import_backup_pick") {
                ActionCard(
                    title = "选择备份文件",
                    subtitle = effectiveBackupImportPreview?.let { preview ->
                        "备份版本 ${preview.appVersion} · ${preview.createdAt.ifBlank { "未知时间" }}"
                    } ?: "选择 Flue ZIP 备份文件",
                    onClick = {
                        if (!backupBusy) {
                            backupOpenPicker.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                    },
                    scale = itemFisheye(listState, "import_backup_pick", screenCenterY, screenHeightPx)
                )
            }
            effectiveBackupImportPreview?.let { preview ->
                backupOptionRows(
                    listState = listState,
                    screenCenterY = screenCenterY,
                    screenHeightPx = screenHeightPx,
                    options = effectiveBackupImportOptions,
                    availableOptions = preview.availableOptions,
                    onOptionsChange = { backupImportOptions = it.constrainedBy(preview.availableOptions) }
                )
                item("import_backup_start") {
                    ActionCard(
                        title = "开始导入",
                        subtitle = "恢复所选项目",
                        onClick = {
                            val uri = backupImportUri
                            if (uri == null) {
                                Toast.makeText(context, "先选择备份文件", Toast.LENGTH_SHORT).show()
                            } else if (!effectiveBackupImportOptions.hasAny) {
                                Toast.makeText(context, "至少选择一项", Toast.LENGTH_SHORT).show()
                            } else if (!backupBusy) {
                                val options = effectiveBackupImportOptions
                                settingsScope.launch {
                                    backupBusy = true
                                    backupProgress = BlockingProgressState(
                                        title = "导入配置",
                                        detail = "正在恢复所选项目"
                                    )
                                    val result = vm.importBackup(uri, options)
                                    result
                                        .onSuccess {
                                            backupProgress = BlockingProgressState(
                                                title = "导入完成",
                                                detail = options.backupSummaryText(),
                                                completed = true,
                                                completionAction = BlockingProgressCompletionAction.ImportBackup
                                            )
                                            backupImportPreview = null
                                            backupImportUri = null
                                        }
                                        .onFailure { error ->
                                            backupProgress = null
                                            Toast.makeText(
                                                context,
                                                "导入失败：${error.message ?: error.javaClass.simpleName}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    backupBusy = false
                                }
                            }
                        },
                        scale = itemFisheye(listState, "import_backup_start", screenCenterY, screenHeightPx)
                    )
                }
            }
        }

            SettingsDestination.ABOUT -> SettingsPageScaffold(
                title = "关于",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.ABOUT).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.ABOUT).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.ABOUT, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("about_card") {
                    AboutCard(
                        onDonateClick = { navigateTo(SettingsDestination.DONATE) },
                        onVersionClick = {
                            dingDingCatVersionTapCount = 0
                            dingDingCatLastVersionTapAt = 0L
                        },
                        scale = itemFisheye(listState, "about_card", screenCenterY, screenHeightPx)
                    )
                }
            }
            SettingsDestination.DONATE -> SettingsPageScaffold(
                title = "捐赠支持",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.DONATE).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.DONATE).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.DONATE, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("donate_tip") {
                    MessageCard(
                        text = "真的很感谢你能支持 Flue 开发！点开下方二维码可全屏查看",
                        background = Color(0xFF1A2233),
                        onClick = {}
                    )
                }
                item("donate_wechat") {
                    DonateMethodCard(
                        title = "寰俊",
                        subtitle = "点击预览图全屏查看",
                        resId = R.drawable.donate_wechat,
                        scale = itemFisheye(listState, "donate_wechat", screenCenterY, screenHeightPx),
                        onPreviewClick = { donatePreviewResId = R.drawable.donate_wechat }
                    )
                }
                item("donate_alipay") {
                    DonateMethodCard(
                        title = "支付宝",
                        subtitle = "点击预览图全屏查看",
                        resId = R.drawable.donate_alipay,
                        scale = itemFisheye(listState, "donate_alipay", screenCenterY, screenHeightPx),
                        onPreviewClick = { donatePreviewResId = R.drawable.donate_alipay }
                    )
                }
            }
        }
        }
        }
    }

    donatePreviewResId?.let { resId ->
        DonatePreviewDialog(resId = resId, onDismiss = { donatePreviewResId = null })
    }
}

@Composable
private fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    headerTime: String,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> },
    content: LazyListScope.(LazyListState, Float, Float, Set<Any>) -> Unit
) {
    val launcherStyle = LauncherTheme.style
    val context = LocalContext.current
    val leftSafeInsetPercent = LocalSettingsLeftSafeInsetPercent.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset
    )
    val focusRequester = remember { FocusRequester() }
    val overscroll = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val visibleItemKeys by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key }.toSet()
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source != NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val atBottom = lastVisible != null &&
                    lastVisible.index >= listState.layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <= listState.layoutInfo.viewportEndOffset
                if (available.y > 0f && atTop) {
                    scope.launch { overscroll.snapTo((overscroll.value + available.y * 0.35f).coerceAtMost(140f)) }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (available.y < 0f && atBottom) {
                    scope.launch { overscroll.snapTo((overscroll.value + available.y * 0.35f).coerceAtLeast(-140f)) }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (overscroll.value > 0f && available.y < 0f) {
                    scope.launch { overscroll.snapTo((overscroll.value + available.y).coerceAtLeast(0f)) }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (overscroll.value < 0f && available.y > 0f) {
                    scope.launch { overscroll.snapTo((overscroll.value + available.y).coerceAtMost(0f)) }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscroll.value != 0f) {
                    overscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> onScrollChanged(index, offset) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFirstFrame()
    }

    val rotaryScrollMultiplier = 1.18f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(launcherStyle.screenBackground)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenCenterY = screenHeightPx / 2f
        val leftSafeInset = maxWidth * (leftSafeInsetPercent.coerceIn(0, 50) / 100f)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .flueRotaryScrollable(
                    focusRequester,
                    rotaryScrollMultiplier,
                    onRotaryScroll = { vibrateHaptic(context) }
                ) { rotaryDelta ->
                    scope.launch {
                        listState.scrollBy(-rotaryDelta)
                    }
                }
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = overscroll.value }
                .padding(start = 16.dp + leftSafeInset, top = 18.dp, end = 16.dp, bottom = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeaderBackButton(onClick = onBack)
                    Text(
                        text = headerTime,
                        color = launcherStyle.topBarTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            item {
                Text(
                    text = title,
                    color = launcherStyle.accentColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content(listState, screenCenterY, screenHeightPx, visibleItemKeys)
            item("tail_spacer") {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

@Composable
private fun HeaderBackButton(onClick: () -> Unit) {
    val launcherStyle = LauncherTheme.style
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "header_back_scale")
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(launcherStyle.topBarChipColor)
            .instantPressGesture(pressedState, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = launcherStyle.topBarTextColor)
    }
}

@Composable
private fun SettingsCategoryCard(title: String, subtitle: String, onClick: () -> Unit, scale: Float) {
    val launcherStyle = LauncherTheme.style
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.958f else 1f,
        animationSpec = launcherStyle.pressScaleSpec,
        label = "settings_category_scale"
    )
    val background by animateColorAsState(
        if (pressed) launcherStyle.pressedCardColor else launcherStyle.cardColor,
        label = "settings_category_bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressedScale
                scaleY = scale * pressedScale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(launcherStyle.cardShape)
            .background(background)
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.cardShape)
            .instantPressGesture(pressedState, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .padding(end = 12.dp)
            ) {
                Text(title, color = launcherStyle.titleColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = launcherStyle.secondaryTextColor, fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = launcherStyle.accentColor)
        }
    }
}

@Composable
private fun SectionTitle(text: String, scale: Float) {
    val launcherStyle = LauncherTheme.style
    Text(
        text = text,
        color = launcherStyle.secondaryTextColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 4.dp, start = 4.dp, bottom = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0.55f, 1f)
            }
    )
}

@Composable
private fun rememberPressedState(): MutableState<Boolean> {
    val state = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { state.value = false }
    }
    return state
}

private fun Modifier.instantPressGesture(
    pressedState: MutableState<Boolean>,
    enabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    longPressDurationMs: Long? = null,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    if (onLongPress != null && longPressDurationMs != null) {
        return pointerInput(onClick, onLongPress, enabled, longPressDurationMs) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressedState.value = true
                var completedBeforeTimeout = false
                val up = withTimeoutOrNull(longPressDurationMs) {
                    waitForUpOrCancellation().also { completedBeforeTimeout = true }
                }
                if (completedBeforeTimeout) {
                    pressedState.value = false
                    if (up != null) onClick()
                } else {
                    pressedState.value = false
                    onLongPress()
                    waitForUpOrCancellation()
                }
            }
        }
    }
    return pointerInput(onClick, onLongPress, enabled) {
        var longPressConsumed = false
        detectTapGestures(
            onLongPress = onLongPress?.let { handler ->
                {
                    longPressConsumed = true
                    pressedState.value = false
                    handler()
                }
            },
            onPress = {
                longPressConsumed = false
                pressedState.value = true
                val released = tryAwaitRelease()
                pressedState.value = false
                if (released && !longPressConsumed) onClick()
            }
        )
    }
}

private val SettingsDestination.depth: Int
    get() = when (this) {
        SettingsDestination.ROOT -> 0
        SettingsDestination.WATCH_FACE_MORE,
        SettingsDestination.BACKUP,
        SettingsDestination.DONATE -> 2
        SettingsDestination.MUSIC_TEXT_ANIMATION,
        SettingsDestination.BACKUP_EXPORT,
        SettingsDestination.BACKUP_IMPORT -> 3
        else -> 1
    }

private val SettingsDestination.resetsScrollOnEnter: Boolean
    get() = when (this) {
        SettingsDestination.DISPLAY_SIZE,
        SettingsDestination.FEATURE_TOGGLES -> true
        else -> depth >= 3
    }

@Composable
private fun MessageCard(text: String, background: Color, onClick: () -> Unit) {
    val launcherStyle = LauncherTheme.style
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.964f else 1f,
        animationSpec = launcherStyle.pressScaleSpec,
        label = "message_card_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clip(launcherStyle.compactShape)
            .background(background)
            .instantPressGesture(pressedState, onClick = onClick)
            .padding(14.dp)
    ) {
        Text(text = text, color = launcherStyle.bodyColor, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    scale: Float
) {
    val launcherStyle = LauncherTheme.style
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        if (pressed) 0.964f else 1f,
        animationSpec = launcherStyle.pressScaleSpec,
        label = "choice_row_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressedScale
                scaleY = scale * pressedScale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(launcherStyle.compactShape)
            .background(
                when {
                    pressed && selected -> launcherStyle.selectedCardColor.copy(alpha = 0.80f)
                    pressed -> launcherStyle.pressedCardColor
                    selected -> launcherStyle.selectedCardColor
                    else -> launcherStyle.cardColor
                }
            )
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.compactShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .padding(end = 12.dp)
            ) {
                Text(title, color = launcherStyle.titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = launcherStyle.secondaryTextColor, fontSize = 12.sp)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = launcherStyle.accentColor)
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
    scale: Float,
    leadingIcon: ImageBitmap? = null,
    reserveLeadingIconSpace: Boolean = false
) {
    val launcherStyle = LauncherTheme.style
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        if (pressed) 0.958f else 1f,
        animationSpec = launcherStyle.pressScaleSpec,
        label = "switch_row_scale"
    )
    val trackColor by animateColorAsState(
        when {
            !enabled -> Color(0xFF2A2A2A)
            checked -> launcherStyle.positiveColor
            else -> launcherStyle.secondaryTextColor.copy(alpha = 0.45f)
        },
        label = "switch_track_color"
    )
    val knobOffset by animateDpAsState(
        if (checked) launcherStyle.switchThumbTravel.dp else 2.dp,
        animationSpec = spring(stiffness = 760f, dampingRatio = 0.82f),
        label = "switch_knob_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressedScale
                scaleY = scale * pressedScale
                alpha = if (enabled) scale.coerceIn(0.55f, 1f) else 0.5f
            }
            .clip(launcherStyle.compactShape)
            .background(if (pressed) launcherStyle.pressedCardColor else launcherStyle.cardColor)
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.compactShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onToggle(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null || reserveLeadingIconSpace) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (leadingIcon != null) 0f else 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (leadingIcon != null) {
                            Image(
                                bitmap = leadingIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(title, color = launcherStyle.titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(subtitle, color = launcherStyle.secondaryTextColor, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = knobOffset, top = 3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: Float,
    valueText: String,
    localValueText: ((Float) -> String)? = null,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    scale: Float,
    enabled: Boolean = true
) {
    val launcherStyle = LauncherTheme.style
    var localValue by remember(title) { mutableFloatStateOf(value) }
    LaunchedEffect(value) { localValue = value }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) scale.coerceIn(0.55f, 1f) else 0.5f
            }
            .clip(launcherStyle.compactShape)
            .background(launcherStyle.cardColor)
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.compactShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(title, color = launcherStyle.titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(localValueText?.invoke(localValue) ?: valueText, color = launcherStyle.secondaryTextColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = localValue,
                onValueChange = {
                    localValue = it
                    if (onValueChangeFinished == null) {
                        onValueChange(it)
                    }
                },
                onValueChangeFinished = {
                    onValueChangeFinished?.invoke(localValue)
                },
                valueRange = range,
                steps = steps,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = launcherStyle.accentColor,
                    activeTrackColor = launcherStyle.accentColor
                )
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    longPressDurationMs: Long? = null,
    scale: Float
) {
    val launcherStyle = LauncherTheme.style
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        animationSpec = launcherStyle.pressScaleSpec,
        label = "action_card_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressedScale
                scaleY = scale * pressedScale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(launcherStyle.compactShape)
            .background(if (pressed) launcherStyle.pressedCardColor else launcherStyle.cardColor)
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.compactShape)
            .instantPressGesture(
                pressedState,
                onLongPress = onLongPress,
                longPressDurationMs = longPressDurationMs,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(end = 12.dp)
            ) {
                Text(title, color = launcherStyle.accentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(subtitle, color = launcherStyle.secondaryTextColor, fontSize = 12.sp)
                }
            }
            icon?.invoke()
        }
    }
}

@Composable
private fun AboutCard(
    scale: Float,
    onDonateClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    val launcherStyle = LauncherTheme.style
    val context = LocalContext.current
    fun openAuthorProfile() {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse(AUTHOR_BILIBILI_URL)
            )
        )
    }
    fun copyAuthorQq() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("QQ", AUTHOR_QQ_NUMBER))
        Toast.makeText(context, "已复制 QQ：$AUTHOR_QQ_NUMBER", Toast.LENGTH_SHORT).show()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(launcherStyle.cardShape)
            .background(launcherStyle.cardColor)
            .border(1.dp, launcherStyle.outlineColor, launcherStyle.cardShape)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Flue", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                ABOUT_VERSION,
                color = WatchColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVersionClick
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.author_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = ::openAuthorProfile)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "柚子柚子皮",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = ::openAuthorProfile)
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "QQ：$AUTHOR_QQ_NUMBER",
                        color = WatchColors.TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = ::copyAuthorQq)
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("cjryrx", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("QQ：124018406", color = WatchColors.TextTertiary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            ActionCard(
                title = "加入交流群获取更新",
                subtitle = "群号 $COMMUNITY_GROUP_NUMBER",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(COMMUNITY_GROUP_URL)
                        )
                    )
                },
                scale = 1f
            )
            Spacer(modifier = Modifier.height(10.dp))
            ActionCard(
                title = "捐赠支持",
                subtitle = "微信 / 支付宝",
                onClick = onDonateClick,
                scale = 1f
            )
            Spacer(modifier = Modifier.height(10.dp))
            ActionCard(
                title = "感谢以下开源项目",
                subtitle = "dudu-Dev0/Lunch",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/dudu-Dev0/Lunch")
                        )
                    )
                },
                scale = 1f
            )
            Spacer(modifier = Modifier.height(10.dp))
            ActionCard(
                title = "Lyricon",
                subtitle = "tomakino/lyricon",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/tomakino/lyricon")
                        )
                    )
                },
                scale = 1f
            )
        }
    }
}

@Composable
private fun DonateMethodCard(
    title: String,
    subtitle: String,
    resId: Int,
    scale: Float,
    onPreviewClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val launcherStyle = LauncherTheme.style
    val pressedScale by animateFloatAsState(
        if (pressed) 0.962f else 1f,
        animationSpec = spring(stiffness = 820f, dampingRatio = 0.74f),
        label = "donate_card_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressedScale
                scaleY = scale * pressedScale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(launcherStyle.cardColor)
            .border(1.dp, launcherStyle.outlineColor, RoundedCornerShape(24.dp))
            .instantPressGesture(pressedState, onClick = onPreviewClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = launcherStyle.titleColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = launcherStyle.secondaryTextColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = resId),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(launcherStyle.pressedCardColor)
            )
        }
    }
}

@Composable
private fun DonatePreviewDialog(resId: Int, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        offset = if (newScale <= 1.02f) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.98f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(transformState)
                    .pointerInput(scale) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.2f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .pointerInput(scale) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                if (scale > 1.02f) {
                                    change.consume()
                                    offset += Offset(dragAmount.x, dragAmount.y)
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun itemFisheye(
    listState: LazyListState,
    key: String,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    return bottomFisheyeScale(listState, key, screenCenterY, screenHeight)
}

@Composable
private fun BackupOptionsDialog(
    title: String,
    subtitle: String,
    options: FlueBackupOptions,
    availableOptions: FlueBackupOptions,
    confirmText: String,
    onOptionsChange: (FlueBackupOptions) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle, color = LauncherTheme.style.secondaryTextColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableOptions.settings) {
                        item("backup_settings") {
                            BackupOptionSwitch(
                                title = "设置",
                                subtitle = "桌面外观、性能、开关等通用设置",
                                checked = options.settings,
                                onToggle = { onOptionsChange(options.copy(settings = it)) }
                            )
                        }
                    }
                    if (availableOptions.appOrder) {
                        item("backup_app_order") {
                            BackupOptionSwitch(
                                title = "应用排序",
                                subtitle = "应用列表的排列顺序",
                                checked = options.appOrder,
                                onToggle = { onOptionsChange(options.copy(appOrder = it)) }
                            )
                        }
                    }
                    if (availableOptions.widgetOrder) {
                        item("backup_widget_order") {
                            BackupOptionSwitch(
                                title = "小组件排序",
                                subtitle = "小组件列表和槽位顺序",
                                checked = options.widgetOrder,
                                onToggle = { onOptionsChange(options.copy(widgetOrder = it)) }
                            )
                        }
                    }
                    if (availableOptions.sideScreenOrder) {
                        item("backup_side_screen_order") {
                            BackupOptionSwitch(
                                title = "负一屏排序",
                                subtitle = "负一屏分区顺序和快捷方式槽位",
                                checked = options.sideScreenOrder,
                                onToggle = { onOptionsChange(options.copy(sideScreenOrder = it)) }
                            )
                        }
                    }
                    if (availableOptions.photoWatchFace) {
                        item("backup_photo_watchface") {
                            BackupOptionSwitch(
                                title = "图片表盘",
                                subtitle = "图片表盘配置和当前图片",
                                checked = options.photoWatchFace,
                                onToggle = { onOptionsChange(options.copy(photoWatchFace = it)) }
                            )
                        }
                    }
                    if (availableOptions.videoWatchFace) {
                        item("backup_video_watchface") {
                            BackupOptionSwitch(
                                title = "视频表盘",
                                subtitle = "视频表盘配置和当前视频",
                                checked = options.videoWatchFace,
                                onToggle = { onOptionsChange(options.copy(videoWatchFace = it)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun BackupOptionSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    scale: Float = 1f
) {
    SettingsSwitchRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onToggle = onToggle,
        scale = scale
    )
}

private fun LazyListScope.backupOptionRows(
    listState: LazyListState,
    screenCenterY: Float,
    screenHeightPx: Float,
    options: FlueBackupOptions,
    availableOptions: FlueBackupOptions,
    onOptionsChange: (FlueBackupOptions) -> Unit
) {
    if (availableOptions.settings) {
        item("backup_settings") {
            BackupOptionSwitch(
                title = "设置",
                subtitle = "桌面外观、性能、开关等通用设置",
                checked = options.settings,
                onToggle = { onOptionsChange(options.copy(settings = it)) },
                scale = itemFisheye(listState, "backup_settings", screenCenterY, screenHeightPx)
            )
        }
    }
    if (availableOptions.appOrder) {
        item("backup_app_order") {
            BackupOptionSwitch(
                title = "应用排序",
                subtitle = "应用列表的排列顺序",
                checked = options.appOrder,
                onToggle = { onOptionsChange(options.copy(appOrder = it)) },
                scale = itemFisheye(listState, "backup_app_order", screenCenterY, screenHeightPx)
            )
        }
    }
    if (availableOptions.widgetOrder) {
        item("backup_widget_order") {
            BackupOptionSwitch(
                title = "小组件排序",
                subtitle = "小组件列表和槽位顺序",
                checked = options.widgetOrder,
                onToggle = { onOptionsChange(options.copy(widgetOrder = it)) },
                scale = itemFisheye(listState, "backup_widget_order", screenCenterY, screenHeightPx)
            )
        }
    }
    if (availableOptions.sideScreenOrder) {
        item("backup_side_screen_order") {
            BackupOptionSwitch(
                title = "负一屏排序",
                subtitle = "负一屏分区顺序和快捷方式槽位",
                checked = options.sideScreenOrder,
                onToggle = { onOptionsChange(options.copy(sideScreenOrder = it)) },
                scale = itemFisheye(listState, "backup_side_screen_order", screenCenterY, screenHeightPx)
            )
        }
    }
    if (availableOptions.photoWatchFace) {
        item("backup_photo_watchface") {
            BackupOptionSwitch(
                title = "图片表盘",
                subtitle = "图片表盘配置和当前图片",
                checked = options.photoWatchFace,
                onToggle = { onOptionsChange(options.copy(photoWatchFace = it)) },
                scale = itemFisheye(listState, "backup_photo_watchface", screenCenterY, screenHeightPx)
            )
        }
    }
    if (availableOptions.videoWatchFace) {
        item("backup_video_watchface") {
            BackupOptionSwitch(
                title = "视频表盘",
                subtitle = "视频表盘配置和当前视频",
                checked = options.videoWatchFace,
                onToggle = { onOptionsChange(options.copy(videoWatchFace = it)) },
                scale = itemFisheye(listState, "backup_video_watchface", screenCenterY, screenHeightPx)
            )
        }
    }
}

private fun FlueBackupOptions.backupSummaryText(): String {
    val labels = buildList {
        if (settings) add("设置")
        if (appOrder) add("应用排序")
        if (widgetOrder) add("小组件排序")
        if (sideScreenOrder) add("负一屏排序")
        if (photoWatchFace) add("图片表盘")
        if (videoWatchFace) add("视频表盘")
        if (dingDingCatWatchFaces) add("叮叮猫表盘")
    }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "无"
}

private fun Uri.backupDisplayLabel(): String {
    return runCatching {
        val path = lastPathSegment?.substringAfterLast(':').orEmpty().ifBlank { toString() }
        path.substringAfterLast('/').ifBlank { "未知位置" }
    }.getOrElse { toString() }
}

@Composable
private fun BlockingProgressDialog(
    state: BlockingProgressState,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (state.completed) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = state.completed,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.completed) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = WatchColors.ActiveCyan,
                        modifier = Modifier.size(52.dp)
                    )
                } else {
                    CircularProgressIndicator(color = WatchColors.ActiveCyan)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(state.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(state.detail, color = WatchColors.TextTertiary, fontSize = 12.sp)
                if (state.completed) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.shareUri != null) {
                            TextButton(onClick = onShare) {
                                Text("分享", color = WatchColors.ActiveCyan)
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text("完成", color = WatchColors.ActiveCyan)
                        }
                    }
                }
            }
        }
    }
}

private fun shareBackupUri(context: android.content.Context, uri: Uri) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "分享配置备份"
            )
        )
    }.onFailure {
        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun exportLog(context: android.content.Context) {
    try {
        val file = withContext(Dispatchers.IO) {
            val rawLog = runCatching {
                Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "500"))
                    .inputStream
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrElse { error -> "logcat export failed: ${error.message.orEmpty()}" }
            val crashSummary = listOf("crash_brief.txt", "crash_info.txt", "crash_app_info.txt")
                .mapNotNull { name ->
                    val crashFile = File(context.cacheDir, name)
                    if (!crashFile.exists()) return@mapNotNull null
                    "===== $name =====\n${crashFile.readText()}"
                }
                .joinToString("\n\n")
                .ifBlank { "No private crash files found." }
            File(context.cacheDir, "wlauncher_log.txt").apply {
                writeText(
                    buildString {
                        appendLine("===== Flue Diagnostics =====")
                        appendLine("diagnosticLoggingEnabled=${ILog.isDiagnosticLoggingEnabled()}")
                        appendLine()
                        appendLine("===== Private Crash Files =====")
                        appendLine(crashSummary)
                        appendLine()
                        appendLine("===== Raw logcat tail =====")
                        append(rawLog)
                    }
                )
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "导出日志"
            )
        )
    } catch (_: Exception) {
        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun rememberSettingsHeaderTime(): String {
    var time by remember { mutableStateOf("--:--") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            time = formatter.format(Date())
            delay(30_000)
        }
    }
    return time
}
