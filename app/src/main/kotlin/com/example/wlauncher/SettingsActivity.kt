package com.flue.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import com.flue.launcher.ui.common.bottomFisheyeScale
import com.flue.launcher.ui.input.flueRotaryScrollable
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import com.flue.launcher.ui.navigation.LayoutMode
import com.flue.launcher.ui.settings.WatchFaceSettingCard
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.ui.theme.WatchLauncherTheme
import com.flue.launcher.util.RecentsVisibility
import com.flue.launcher.viewmodel.LauncherViewModel
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_VIDEO_WATCHFACE_ID
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.LunchWatchFaceRuntime
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.Locale

private const val ABOUT_VERSION = "beta1.2"
private const val COMMUNITY_GROUP_NUMBER = "1097162313"
private const val COMMUNITY_GROUP_URL = "https://qun.qq.com/universal-share/share?ac=1&authKey=5CJC0tNLWsy3YWzGlPbqt%2F5kv%2BYZuJ8y8IVj%2B1UnIeLyR2DWR6QjWtM%2B4HXxH%2BKJ&busi_data=eyJncm91cENvZGUiOiIxMDk3MTYyMzEzIiwidG9rZW4iOiJlMGFBc1VpRXRROE1mS3J5SHgxRjFudmxXY0FuSnRQd2hOWWl6WllFMmlxNTErNWdadXJ5U1ozMmdzUSszaGNYIiwidWluIjoiMzUxMzkwMzA1NSJ9&data=YK52b80xUVwmcLWmSneKQMdVZodE4vTpsqmSb60ykXudbVCfa6AskMHxvqjhAjervNeE4exll-kQw5w-EifYOg&svctype=4&tempid=h5_group_info"
const val EXTRA_SETTINGS_DESTINATION = "settings_destination"
const val EXTRA_SETTINGS_RETURN_TO_FACE = "settings_return_to_face"
const val SETTINGS_DESTINATION_WATCH_FACES = "watch_faces"

enum class SettingsDestination {
    ROOT,
    WATCH_FACES,
    HIDDEN_APPS,
    ICON_PACKS,
    APPEARANCE,
    PERFORMANCE,
    TOOLS,
    ABOUT,
    DONATE
}

private data class SavedScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        val initialDestination = when (intent.getStringExtra(EXTRA_SETTINGS_DESTINATION)) {
            SETTINGS_DESTINATION_WATCH_FACES -> SettingsDestination.WATCH_FACES
            else -> SettingsDestination.ROOT
        }
        val returnToFaceOnBack = intent.getBooleanExtra(EXTRA_SETTINGS_RETURN_TO_FACE, false)
        setContent {
            WatchLauncherTheme {
                SettingsRootScreen(
                    onFinish = { finish() },
                    initialDestination = initialDestination,
                    returnToFaceOnBack = returnToFaceOnBack
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
private fun SettingsRootScreen(
    onFinish: () -> Unit,
    initialDestination: SettingsDestination = SettingsDestination.ROOT,
    returnToFaceOnBack: Boolean = false
) {
    val context = LocalContext.current
    val vm: LauncherViewModel = viewModel()
    val watchFaces by vm.availableWatchFaces.collectAsState()
    val selectedWatchFaceId by vm.selectedWatchFaceId.collectAsState()
    val selectedWatchFace by vm.selectedWatchFace.collectAsState()
    val allApps by vm.allApps.collectAsState()
    val hiddenApps by vm.hiddenApps.collectAsState()
    val availableIconPacks by vm.availableIconPacks.collectAsState()
    val selectedIconPackPackage by vm.selectedIconPackPackage.collectAsState()
    val watchFaceLastError by vm.watchFaceLastError.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val sideScreenEnabled by vm.sideScreenEnabled.collectAsState()
    val blurEnabled by vm.blurEnabled.collectAsState()
    val edgeBlurEnabled by vm.edgeBlurEnabled.collectAsState()
    val lowResIcons by vm.lowResIcons.collectAsState()
    val animationOverrideEnabled by vm.animationOverrideEnabled.collectAsState()
    val splashIcon by vm.splashIcon.collectAsState()
    val splashDelay by vm.splashDelay.collectAsState()
    val honeycombCols by vm.honeycombCols.collectAsState()
    val honeycombTopBlur by vm.honeycombTopBlur.collectAsState()
    val honeycombBottomBlur by vm.honeycombBottomBlur.collectAsState()
    val honeycombTopFade by vm.honeycombTopFade.collectAsState()
    val honeycombBottomFade by vm.honeycombBottomFade.collectAsState()
    val honeycombFastScrollOptimization by vm.honeycombFastScrollOptimization.collectAsState()
    val builtInPhotoPath by vm.builtInPhotoPath.collectAsState()
    val builtInVideoPath by vm.builtInVideoPath.collectAsState()
    val builtInPhotoClockPosition by vm.builtInPhotoClockPosition.collectAsState()
    val builtInVideoClockPosition by vm.builtInVideoClockPosition.collectAsState()
    val builtInPhotoClockSize by vm.builtInPhotoClockSize.collectAsState()
    val builtInVideoClockSize by vm.builtInVideoClockSize.collectAsState()
    val builtInPhotoClockBold by vm.builtInPhotoClockBold.collectAsState()
    val builtInVideoClockBold by vm.builtInVideoClockBold.collectAsState()
    val builtInVideoFillScreen by vm.builtInVideoFillScreen.collectAsState()
    val builtInVideoClockColorMode by vm.builtInVideoClockColorMode.collectAsState()
    val builtInManagerThumbnails by vm.builtInManagerThumbnails.collectAsState()
    val hideFromRecents by vm.hideFromRecents.collectAsState()
    val showNotification by vm.showNotification.collectAsState()
    val headerTime = rememberSettingsHeaderTime()
    val isZh = remember(context.resources.configuration) {
        context.resources.configuration.locales[0]?.language?.startsWith("zh") == true
    }

    var destination by remember(initialDestination) { mutableStateOf(initialDestination) }
    var hiddenAppsDraft by remember { mutableStateOf(hiddenApps) }
    var hiddenAppsDirty by remember { mutableStateOf(false) }
    var donatePreviewResId by remember { mutableStateOf<Int?>(null) }
    val pageScrollPositions = remember { mutableStateMapOf<SettingsDestination, SavedScrollPosition>() }

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

    val navigateTo: (SettingsDestination) -> Unit = { next ->
        if (destination == SettingsDestination.HIDDEN_APPS && next != SettingsDestination.HIDDEN_APPS) {
            commitHiddenAppsDraft()
        }
        destination = next
    }

    val handleBack: () -> Unit = {
        when (destination) {
            SettingsDestination.HIDDEN_APPS -> {
                commitHiddenAppsDraft()
                if (returnToFaceOnBack) onFinish() else navigateTo(SettingsDestination.ROOT)
            }
            SettingsDestination.DONATE -> navigateTo(SettingsDestination.ABOUT)
            SettingsDestination.ROOT -> onFinish()
            else -> if (returnToFaceOnBack) onFinish() else navigateTo(SettingsDestination.ROOT)
        }
    }

    BackHandler(enabled = destination != SettingsDestination.ROOT || returnToFaceOnBack) {
        handleBack()
    }

    val selectedIconPackLabel = availableIconPacks.firstOrNull { it.packageName == selectedIconPackPackage }?.label
    val scrollFor: (SettingsDestination) -> SavedScrollPosition = { target ->
        if (target == SettingsDestination.ROOT) {
            pageScrollPositions[target] ?: SavedScrollPosition()
        } else {
            SavedScrollPosition()
        }
    }
    val updateScroll: (SettingsDestination, Int, Int) -> Unit = { target, index, offset ->
        if (target == SettingsDestination.ROOT) {
            pageScrollPositions[target] = SavedScrollPosition(index = index, offset = offset)
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                (fadeOut(animationSpec = tween(140)) + scaleOut(targetScale = 0.985f, animationSpec = tween(140)))
        },
        label = "settings_destination"
    ) { currentDestination ->
        when (currentDestination) {
            SettingsDestination.ROOT -> SettingsPageScaffold(
                title = "\u684c\u9762\u8bbe\u7f6e",
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
                        title = "\u8868\u76d8",
                        subtitle = selectedWatchFace.displayName,
                        onClick = { navigateTo(SettingsDestination.WATCH_FACES) },
                        scale = itemFisheye(listState, "watchfaces", screenCenterY, screenHeightPx)
                    )
                }
                item("appearance") {
                    SettingsCategoryCard(
                        title = "\u663e\u793a\u4e0e\u5916\u89c2",
                        subtitle = "\u5e03\u5c40\u3001\u6a21\u7cca\u4e0e\u542f\u52a8\u56fe\u6807",
                        onClick = { navigateTo(SettingsDestination.APPEARANCE) },
                        scale = itemFisheye(listState, "appearance", screenCenterY, screenHeightPx)
                    )
                }
                item("hidden_apps") {
                    SettingsCategoryCard(
                        title = "\u9690\u85cf\u5e94\u7528",
                        subtitle = "\u5df2\u9690\u85cf ${hiddenAppsDraft.size} \u4e2a\u5e94\u7528",
                        onClick = { navigateTo(SettingsDestination.HIDDEN_APPS) },
                        scale = itemFisheye(listState, "hidden_apps", screenCenterY, screenHeightPx)
                    )
                }
                item("icon_packs") {
                    SettingsCategoryCard(
                        title = "\u56fe\u6807\u5305",
                        subtitle = selectedIconPackLabel ?: "\u7cfb\u7edf\u9ed8\u8ba4\u56fe\u6807",
                        onClick = { navigateTo(SettingsDestination.ICON_PACKS) },
                        scale = itemFisheye(listState, "icon_packs", screenCenterY, screenHeightPx)
                    )
                }
                item("performance") {
                    SettingsCategoryCard(
                        title = "\u6027\u80fd\u4e0e\u52a8\u753b",
                        subtitle = "\u56fe\u6807\u8d28\u91cf\u4e0e\u52a8\u753b\u63a7\u5236",
                        onClick = { navigateTo(SettingsDestination.PERFORMANCE) },
                        scale = itemFisheye(listState, "performance", screenCenterY, screenHeightPx)
                    )
                }
                item("tools") {
                    SettingsCategoryCard(
                        title = "\u5de5\u5177",
                        subtitle = "\u5bfc\u51fa\u65e5\u5fd7\u4e0e\u6062\u590d\u9ed8\u8ba4",
                        onClick = { navigateTo(SettingsDestination.TOOLS) },
                        scale = itemFisheye(listState, "tools", screenCenterY, screenHeightPx)
                    )
                }
                item("about") {
                    SettingsCategoryCard(
                        title = "\u5173\u4e8e",
                        subtitle = "Flue  $ABOUT_VERSION",
                        onClick = { navigateTo(SettingsDestination.ABOUT) },
                        scale = itemFisheye(listState, "about", screenCenterY, screenHeightPx)
                    )
                }
            }

            SettingsDestination.HIDDEN_APPS -> SettingsPageScaffold(
                title = "\u9690\u85cf\u5e94\u7528",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.HIDDEN_APPS).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.HIDDEN_APPS).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.HIDDEN_APPS, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, visibleItemKeys ->
                item("hidden_summary") {
                    MessageCard(
                        text = "\u5df2\u9690\u85cf ${hiddenAppsDraft.size} \u4e2a\u5e94\u7528",
                        background = WatchColors.SurfaceGlass,
                        onClick = {}
                    )
                }
                items(allApps, key = { "app_${it.componentKey}" }) { app ->
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
                title = "\u56fe\u6807\u5305",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.ICON_PACKS).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.ICON_PACKS).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.ICON_PACKS, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("icon_pack_default") {
                    SettingsChoiceRow(
                        title = "\u7cfb\u7edf\u9ed8\u8ba4",
                        subtitle = "\u4f7f\u7528 Flue \u5f53\u524d\u5e94\u7528\u56fe\u6807",
                        selected = selectedIconPackPackage.isNullOrBlank(),
                        onClick = { vm.setIconPackPackage(null) },
                        scale = itemFisheye(listState, "icon_pack_default", screenCenterY, screenHeightPx)
                    )
                }
                items(availableIconPacks, key = { "iconpack_${it.packageName}" }) { pack ->
                    SettingsChoiceRow(
                        title = pack.label,
                        subtitle = "ADW Icon Pack Standard",
                        selected = pack.packageName == selectedIconPackPackage,
                        onClick = { vm.setIconPackPackage(pack.packageName) },
                        scale = itemFisheye(listState, "iconpack_${pack.packageName}", screenCenterY, screenHeightPx)
                    )
                }
                item("icon_pack_refresh") {
                    ActionCard(
                        title = "\u5237\u65b0\u56fe\u6807\u5305",
                        subtitle = "\u91cd\u65b0\u626b\u63cf\u5df2\u5b89\u88c5\u7684 ADW \u56fe\u6807\u5305",
                        icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = WatchColors.ActiveCyan) },
                        onClick = { vm.refreshIconPacks() },
                        scale = itemFisheye(listState, "icon_pack_refresh", screenCenterY, screenHeightPx)
                    )
                }
            }

            SettingsDestination.WATCH_FACES -> SettingsPageScaffold(
            title = "\u8868\u76d8",
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
                    builtInPhotoPath = builtInPhotoPath,
                    builtInVideoPath = builtInVideoPath,
                    photoOptions = BuiltInWatchFaceOptions(
                        clockPosition = builtInPhotoClockPosition,
                        clockSizeSp = builtInPhotoClockSize,
                        boldClock = builtInPhotoClockBold
                    ),
                    videoOptions = BuiltInWatchFaceOptions(
                        clockPosition = builtInVideoClockPosition,
                        clockSizeSp = builtInVideoClockSize,
                        boldClock = builtInVideoClockBold,
                        cropToFill = builtInVideoFillScreen,
                        clockColorMode = builtInVideoClockColorMode
                    ),
                    onSelect = { vm.selectWatchFace(descriptor.id) },
                    onOpenSettings = if (descriptor.supportsSettings) {
                        {
                            if (descriptor.isBuiltin && descriptor.id in setOf(BUILT_IN_PHOTO_WATCHFACE_ID, BUILT_IN_VIDEO_WATCHFACE_ID)) {
                                context.startActivity(
                                    Intent(context, InternalWatchFaceConfigActivity::class.java)
                                        .putExtra(EXTRA_INTERNAL_WATCHFACE_ID, descriptor.id)
                                )
                                (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            } else if (!LunchWatchFaceRuntime.openSettings(context, descriptor)) {
                                Toast.makeText(context, "\u6CA1\u6709\u53EF\u7528\u7684\u8868\u76D8\u8BBE\u7F6E", Toast.LENGTH_SHORT).show()
                            } else {
                                (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                        }
                    } else {
                        null
                    },
                    scale = itemFisheye(listState, descriptor.id, screenCenterY, screenHeightPx)
                )
            }
            item("watchface_refresh") {
                ActionCard(
                    title = "\u91cd\u65b0\u626b\u63cf\u8868\u76d8",
                    subtitle = "\u5237\u65b0\u5df2\u5b89\u88c5\u7684 Lunch \u517c\u5bb9\u8868\u76d8",
                    scale = itemFisheye(listState, "watchface_refresh", screenCenterY, screenHeightPx),
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = WatchColors.ActiveCyan) },
                    onClick = { vm.refreshWatchFaces(force = true) }
                )
            }
        }

            SettingsDestination.APPEARANCE -> SettingsPageScaffold(
            title = "\u663e\u793a\u4e0e\u5916\u89c2",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.APPEARANCE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.APPEARANCE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.APPEARANCE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("layout_header") { SectionTitle("\u5e03\u5c40", itemFisheye(listState, "layout_header", screenCenterY, screenHeightPx)) }
            item("layout_honeycomb") {
                SettingsChoiceRow(
                    title = "\u8702\u7a9d\u5e03\u5c40",
                    subtitle = "Apple Watch \u98ce\u683c",
                    selected = layoutMode == LayoutMode.Honeycomb,
                    onClick = { vm.setLayoutMode(LayoutMode.Honeycomb) },
                    scale = itemFisheye(listState, "layout_honeycomb", screenCenterY, screenHeightPx)
                )
            }
            item("layout_list") {
                SettingsChoiceRow(
                    title = "\u5217\u8868\u5e03\u5c40",
                    subtitle = "\u7ecf\u5178\u7eb5\u5411\u5217\u8868",
                    selected = layoutMode == LayoutMode.List,
                    onClick = { vm.setLayoutMode(LayoutMode.List) },
                    scale = itemFisheye(listState, "layout_list", screenCenterY, screenHeightPx)
                )
            }
            item("display_header") {
                SectionTitle(if (isZh) "\u663e\u793a\u8bbe\u7f6e" else "Display", itemFisheye(listState, "display_header", screenCenterY, screenHeightPx))
            }
            item("side_screen_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "\u526f\u4e00\u5c4f" else "Side Screen",
                    subtitle = if (isZh) "\u4ece\u8868\u76d8\u53f3\u6ed1\u8fdb\u5165\u5feb\u6377\u542f\u52a8\u4e0e\u901a\u77e5\u9875" else "Swipe right from the watch face to open the side screen",
                    checked = sideScreenEnabled,
                    onToggle = vm::setSideScreenEnabled,
                    scale = itemFisheye(listState, "side_screen_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("notification_center_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "\u901a\u77e5\u4e2d\u5fc3" else "Notification Center",
                    subtitle = if (showNotification) {
                        if (isZh) "\u663e\u793a\u901a\u77e5\u6a2a\u5e45\uff0c\u5e76\u5141\u8bb8\u4ece\u526f\u4e00\u5c4f\u4e0a\u6ed1\u8fdb\u5165\u901a\u77e5\u4e2d\u5fc3" else "Show notification banners and allow swipe-up from the side screen into the notification center"
                    } else {
                        if (isZh) "\u9690\u85cf\u901a\u77e5\u6a2a\u5e45\u4e0e\u901a\u77e5\u4e2d\u5fc3\uff0c\u5e76\u628a\u5feb\u6377\u542f\u52a8\u6269\u5c55\u4e3a 3x3" else "Hide notification banners and the notification center, and expand quick launch to 3x3"
                    },
                    checked = showNotification,
                    enabled = sideScreenEnabled,
                    onToggle = vm::setShowNotification,
                    scale = itemFisheye(listState, "notification_center_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("blur_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "\u6a21\u7cca" else "Blur",
                    subtitle = if (isZh) "\u5728\u652f\u6301\u7684 Android \u7248\u672c\u4e0a\u542f\u7528\u6a21\u7cca" else "Enable blur on supported Android versions",
                    checked = blurEnabled,
                    onToggle = vm::setBlurEnabled,
                    scale = itemFisheye(listState, "blur_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("edge_blur_toggle") {
                SettingsSwitchRow(
                    title = if (isZh) "\u8fb9\u7f18\u6a21\u7cca\uff08\u5b9e\u9a8c\uff09" else "Edge Blur (Experimental)",
                    subtitle = if (blurEnabled) {
                        if (isZh) "\u5728\u9876\u90e8\u548c\u5e95\u90e8\u8fb9\u7f18\u589e\u52a0\u6a21\u7cca" else "Apply extra blur near the top and bottom edges"
                    } else {
                        if (isZh) "\u8bf7\u5148\u5f00\u542f\u6a21\u7cca" else "Enable Blur first"
                    },
                    checked = edgeBlurEnabled,
                    enabled = blurEnabled,
                    onToggle = vm::setEdgeBlurEnabled,
                    scale = itemFisheye(listState, "edge_blur_toggle", screenCenterY, screenHeightPx)
                )
            }
            item("honeycomb_cols") {
                SettingsSliderRow(
                    title = "\u8702\u7a9d\u5217\u6570",
                    value = honeycombCols.toFloat(),
                    valueText = "$honeycombCols \u5217",
                    range = 3f..6f,
                    steps = 2,
                    onValueChange = { vm.setHoneycombCols(it.toInt()) },
                    scale = itemFisheye(listState, "honeycomb_cols", screenCenterY, screenHeightPx)
                )
            }
            item("top_blur") {
                SettingsSliderRow(
                    title = "\u9876\u90e8\u6a21\u7cca\u534a\u5f84",
                    value = honeycombTopBlur.toFloat(),
                    valueText = "$honeycombTopBlur dp",
                    range = 0f..48f,
                    steps = 11,
                    onValueChange = { vm.setHoneycombTopBlur(it.toInt()) },
                    enabled = blurEnabled,
                    scale = itemFisheye(listState, "top_blur", screenCenterY, screenHeightPx)
                )
            }
            item("bottom_blur") {
                SettingsSliderRow(
                    title = "\u5e95\u90e8\u6a21\u7cca\u534a\u5f84",
                    value = honeycombBottomBlur.toFloat(),
                    valueText = "$honeycombBottomBlur dp",
                    range = 0f..48f,
                    steps = 11,
                    onValueChange = { vm.setHoneycombBottomBlur(it.toInt()) },
                    enabled = blurEnabled,
                    scale = itemFisheye(listState, "bottom_blur", screenCenterY, screenHeightPx)
                )
            }
            item("top_fade") {
                SettingsSliderRow(
                    title = "\u9876\u90e8\u6e10\u9690\u8303\u56f4",
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
                    title = "\u5e95\u90e8\u6e10\u9690\u8303\u56f4",
                    value = honeycombBottomFade.toFloat(),
                    valueText = "$honeycombBottomFade dp",
                    range = 0f..160f,
                    steps = 15,
                    onValueChange = { vm.setHoneycombBottomFade(it.toInt()) },
                    scale = itemFisheye(listState, "bottom_fade", screenCenterY, screenHeightPx)
                )
            }
            item("launch_header") { SectionTitle("\u542f\u52a8", itemFisheye(listState, "launch_header", screenCenterY, screenHeightPx)) }
            item("splash_toggle") {
                SettingsSwitchRow(
                    title = "\u542f\u52a8\u906e\u7f69",
                    subtitle = "\u6253\u5f00\u5e94\u7528\u65f6\u663e\u793a\u56fe\u6807\u8fc7\u6e21",
                    checked = splashIcon,
                    onToggle = { vm.setSplashIcon(it) },
                    scale = itemFisheye(listState, "splash_toggle", screenCenterY, screenHeightPx)
                )
            }
            if (splashIcon) {
                item("splash_delay") {
                    SettingsSliderRow(
                        title = "\u906e\u7f69\u65f6\u957f",
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

            SettingsDestination.PERFORMANCE -> SettingsPageScaffold(
            title = "\u6027\u80fd\u4e0e\u52a8\u753b",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.PERFORMANCE).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.PERFORMANCE).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.PERFORMANCE, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("low_res") {
                SettingsSwitchRow(
                    title = "\u4f4e\u5206\u8fa8\u7387\u56fe\u6807",
                    subtitle = "\u964d\u4f4e\u56fe\u6807\u5f00\u9500\u4ee5\u63d0\u5347\u6d41\u7545\u5ea6",
                    checked = lowResIcons,
                    onToggle = { vm.setLowResIcons(it) },
                    scale = itemFisheye(listState, "low_res", screenCenterY, screenHeightPx)
                )
            }
            item("honeycomb_fast_scroll_opt") {
                SettingsSwitchRow(
                    title = "\u8702\u7a9d\u9ad8\u901f\u6ed1\u52a8\u4f18\u5316",
                    subtitle = "\u9ad8\u901f\u6ed1\u52a8\u65f6\u4e34\u65f6\u964d\u4f4e\u666e\u901a\u56fe\u6807\u7684\u6a21\u7cca\u4e0e\u9634\u5f71\u8d1f\u8f7d\uff0c\u505c\u4e0b\u540e\u6062\u590d\u5b8c\u6574\u6548\u679c",
                    checked = honeycombFastScrollOptimization,
                    onToggle = { vm.setHoneycombFastScrollOptimization(it) },
                    scale = itemFisheye(listState, "honeycomb_fast_scroll_opt", screenCenterY, screenHeightPx)
                )
            }
            item("anim_override") {
                SettingsSwitchRow(
                    title = "\u684c\u9762\u8fd4\u56de\u52a8\u753b",
                    subtitle = "\u542f\u7528\u7c7b watchOS \u7684\u8fd4\u56de\u8fc7\u6e21",
                    checked = animationOverrideEnabled,
                    onToggle = { vm.setAnimationOverrideEnabled(it) },
                    scale = itemFisheye(listState, "anim_override", screenCenterY, screenHeightPx)
                )
            }
            item("hide_from_recents") {
                SettingsSwitchRow(
                    title = if (isZh) "\u9690\u85cf\u540e\u53f0\u5361\u7247" else "Hide Recents Card",
                    subtitle = if (isZh) "\u4ece\u6700\u8fd1\u4efb\u52a1\u4e2d\u9690\u85cf Flue \u7684\u540e\u53f0\u5361\u7247" else "Hide Flue from the system recents screen",
                    checked = hideFromRecents,
                    onToggle = vm::setHideFromRecents,
                    scale = itemFisheye(listState, "hide_from_recents", screenCenterY, screenHeightPx)
                )
            }
            item("builtin_manager_thumbnails") {
                SettingsSwitchRow(
                    title = "\u5185\u7f6e\u7ba1\u7406\u5668\u7f29\u7565\u56fe",
                    subtitle = "\u5728\u56fe\u7247/\u89c6\u9891\u5217\u8868\u5de6\u4fa7\u663e\u793a\u9884\u89c8\u56fe",
                    checked = builtInManagerThumbnails,
                    onToggle = { vm.setBuiltInManagerThumbnails(it) },
                    scale = itemFisheye(listState, "builtin_manager_thumbnails", screenCenterY, screenHeightPx)
                )
            }
        }

            SettingsDestination.TOOLS -> SettingsPageScaffold(
            title = "\u5de5\u5177",
            onBack = { handleBack() },
            headerTime = headerTime,
            initialFirstVisibleItemIndex = scrollFor(SettingsDestination.TOOLS).index,
            initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.TOOLS).offset,
            onScrollChanged = { index, offset -> updateScroll(SettingsDestination.TOOLS, index, offset) }
        ) { listState, screenCenterY, screenHeightPx, _ ->
            item("export_log") {
                ActionCard(
                    title = "\u5bfc\u51fa\u65e5\u5fd7",
                    subtitle = "\u5bfc\u51fa\u6700\u8fd1 500 \u884c\u7cfb\u7edf\u65e5\u5fd7",
                    onClick = { exportLog(context) },
                    scale = itemFisheye(listState, "export_log", screenCenterY, screenHeightPx)
                )
            }
            item("reset_defaults") {
                ActionCard(
                    title = "\u6062\u590d\u9ed8\u8ba4\u8bbe\u7f6e",
                    subtitle = "\u91cd\u7f6e\u684c\u9762\u5916\u89c2\u4e0e\u6027\u80fd\u9009\u9879",
                    onClick = { vm.resetSettings() },
                    scale = itemFisheye(listState, "reset_defaults", screenCenterY, screenHeightPx)
                )
            }
            }

            SettingsDestination.ABOUT -> SettingsPageScaffold(
                title = "\u5173\u4e8e",
                onBack = { handleBack() },
                headerTime = headerTime,
                initialFirstVisibleItemIndex = scrollFor(SettingsDestination.ABOUT).index,
                initialFirstVisibleItemScrollOffset = scrollFor(SettingsDestination.ABOUT).offset,
                onScrollChanged = { index, offset -> updateScroll(SettingsDestination.ABOUT, index, offset) }
            ) { listState, screenCenterY, screenHeightPx, _ ->
                item("about_card") {
                    AboutCard(
                        onDonateClick = { navigateTo(SettingsDestination.DONATE) },
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
                        text = "感谢支持 Flue。点开下方二维码可全屏查看并扫码捐赠。",
                        background = Color(0xFF1A2233),
                        onClick = {}
                    )
                }
                item("donate_wechat") {
                    DonateMethodCard(
                        title = "微信赞助码",
                        subtitle = "点击预览图全屏查看",
                        resId = R.drawable.donate_wechat,
                        scale = itemFisheye(listState, "donate_wechat", screenCenterY, screenHeightPx),
                        onPreviewClick = { donatePreviewResId = R.drawable.donate_wechat }
                    )
                }
                item("donate_alipay") {
                    DonateMethodCard(
                        title = "支付宝收款码",
                        subtitle = "点击预览图全屏查看",
                        resId = R.drawable.donate_alipay,
                        scale = itemFisheye(listState, "donate_alipay", screenCenterY, screenHeightPx),
                        onPreviewClick = { donatePreviewResId = R.drawable.donate_alipay }
                    )
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
                if (source != NestedScrollSource.Drag) return androidx.compose.ui.geometry.Offset.Zero
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
            .background(Color.Black)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenCenterY = screenHeightPx / 2f

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .flueRotaryScrollable(focusRequester, rotaryScrollMultiplier) { rotaryDelta ->
                    scope.launch {
                        listState.scrollBy(-rotaryDelta)
                    }
                }
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = overscroll.value }
                .padding(horizontal = 16.dp, vertical = 18.dp),
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            item {
                Text(
                    text = title,
                    color = WatchColors.ActiveCyan,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "header_back_scale")
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun SettingsCategoryCard(title: String, subtitle: String, onClick: () -> Unit, scale: Float) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.958f else 1f,
        animationSpec = spring(stiffness = 780f, dampingRatio = 0.72f),
        label = "settings_category_scale"
    )
    val background by animateColorAsState(
        if (pressed) Color(0xFF1A1A1D) else WatchColors.SurfaceGlass,
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
            .clip(RoundedCornerShape(24.dp))
            .background(background)
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
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = WatchColors.TextTertiary, fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = WatchColors.ActiveCyan)
        }
    }
}

@Composable
private fun SectionTitle(text: String, scale: Float) {
    Text(
        text = text,
        color = WatchColors.TextTertiary,
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
private fun rememberPressedState(): MutableState<Boolean> = remember { mutableStateOf(false) }

private fun Modifier.instantPressGesture(
    pressedState: MutableState<Boolean>,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(onClick, enabled) {
        detectTapGestures(
            onPress = {
                pressedState.value = true
                val released = tryAwaitRelease()
                pressedState.value = false
                if (released) onClick()
            }
        )
    }
}

@Composable
private fun MessageCard(text: String, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp)
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
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.964f else 1f,
        animationSpec = spring(stiffness = 820f, dampingRatio = 0.74f),
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
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    pressed && selected -> WatchColors.ActiveCyan.copy(alpha = 0.10f)
                    pressed -> Color(0xFF1A1A1D)
                    selected -> WatchColors.ActiveCyan.copy(alpha = 0.16f)
                    else -> WatchColors.SurfaceGlass
                }
            )
            .instantPressGesture(pressedState, onClick = onClick)
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
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = WatchColors.TextTertiary, fontSize = 12.sp)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WatchColors.ActiveCyan)
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
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.958f else 1f,
        animationSpec = spring(stiffness = 860f, dampingRatio = 0.72f),
        label = "switch_row_scale"
    )
    val trackColor by animateColorAsState(
        when {
            !enabled -> Color(0xFF2A2A2A)
            checked -> WatchColors.ActiveGreen
            else -> Color(0xFF555555)
        },
        label = "switch_track_color"
    )
    val knobOffset by animateDpAsState(
        if (checked) 22.dp else 2.dp,
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
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed) Color(0xFF1A1A1D) else WatchColors.SurfaceGlass)
            .instantPressGesture(pressedState, enabled = enabled) { onToggle(!checked) }
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
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = if (leadingIcon != null) 0f else 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (leadingIcon != null) {
                            Image(
                                bitmap = leadingIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(11.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(subtitle, color = WatchColors.TextTertiary, fontSize = 12.sp)
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
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    scale: Float,
    enabled: Boolean = true
) {
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
            .clip(RoundedCornerShape(18.dp))
            .background(WatchColors.SurfaceGlass)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valueText, color = WatchColors.TextTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = localValue,
                onValueChange = {
                    localValue = it
                    onValueChange(it)
                },
                valueRange = range,
                steps = steps,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = WatchColors.ActiveCyan,
                    activeTrackColor = WatchColors.ActiveCyan
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
    scale: Float
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val pressedScale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 820f, dampingRatio = 0.74f),
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
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed) Color(0xFF1A1A1D) else WatchColors.SurfaceGlass)
            .instantPressGesture(pressedState, onClick = onClick)
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
                Text(title, color = WatchColors.ActiveCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(subtitle, color = WatchColors.TextTertiary, fontSize = 12.sp)
                }
            }
            icon?.invoke()
        }
    }
}

@Composable
private fun AboutCard(scale: Float, onDonateClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0.55f, 1f)
            }
            .clip(RoundedCornerShape(28.dp))
            .background(WatchColors.SurfaceGlass)
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
                    modifier = Modifier.size(76.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Flue", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(ABOUT_VERSION, color = WatchColors.TextTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.author_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("柚子柚子皮", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("QQ：3513903055", color = WatchColors.TextTertiary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            ActionCard(
                title = "加入交流群&获取更新",
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
            .background(WatchColors.SurfaceGlass)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .instantPressGesture(pressedState, onClick = onPreviewClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = WatchColors.TextTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = resId),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.14f))
            )
        }
    }
}

@Composable
private fun DonatePreviewDialog(resId: Int, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
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

private fun exportLog(context: android.content.Context) {
    try {
        val log = Runtime.getRuntime().exec("logcat -d -t 500").inputStream.bufferedReader().readText()
        val file = File(context.cacheDir, "wlauncher_log.txt")
        file.writeText(log)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "\u5bfc\u51fa\u65e5\u5fd7"
            )
        )
    } catch (_: Exception) {
        Toast.makeText(context, "\u5bfc\u51fa\u5931\u8d25", Toast.LENGTH_SHORT).show()
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

