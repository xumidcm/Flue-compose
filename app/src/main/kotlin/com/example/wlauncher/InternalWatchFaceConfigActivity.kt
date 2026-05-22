package com.flue.launcher

import android.app.Application
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flue.launcher.ui.home.BuiltInWatchFacePreview
import com.flue.launcher.ui.home.FIXED_PREVIEW_CLOCK
import com.flue.launcher.ui.input.flueRotaryScrollable
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.ui.theme.WatchLauncherTheme
import com.flue.launcher.util.RecentsVisibility
import com.flue.launcher.viewmodel.LauncherViewModel
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.InternalWatchFaceStorage
import com.flue.launcher.watchface.WatchClockColorMode
import com.flue.launcher.watchface.WatchClockPosition
import kotlinx.coroutines.launch

const val EXTRA_INTERNAL_WATCHFACE_ID = "internal_watchface_id"

class InternalWatchFaceConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        val watchFaceId = intent.getStringExtra(EXTRA_INTERNAL_WATCHFACE_ID) ?: BUILT_IN_PHOTO_WATCHFACE_ID
        setContent {
            WatchLauncherTheme {
                InternalWatchFaceConfigScreen(
                    watchFaceId = watchFaceId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun InternalWatchFaceConfigScreen(
    watchFaceId: String,
    onBack: () -> Unit
) {
    val vm: LauncherViewModel = viewModel()
    val photoPath by vm.builtInPhotoPath.collectAsState()
    val videoPath by vm.builtInVideoPath.collectAsState()
    val photoClockPosition by vm.builtInPhotoClockPosition.collectAsState()
    val videoClockPosition by vm.builtInVideoClockPosition.collectAsState()
    val photoClockSize by vm.builtInPhotoClockSize.collectAsState()
    val videoClockSize by vm.builtInVideoClockSize.collectAsState()
    val photoClockBold by vm.builtInPhotoClockBold.collectAsState()
    val videoClockBold by vm.builtInVideoClockBold.collectAsState()
    val videoFillScreen by vm.builtInVideoFillScreen.collectAsState()
    val videoClockColorMode by vm.builtInVideoClockColorMode.collectAsState()
    val isPhoto = watchFaceId == BUILT_IN_PHOTO_WATCHFACE_ID
    val currentPath = if (isPhoto) photoPath else videoPath
    val activeClockPosition = if (isPhoto) photoClockPosition else videoClockPosition
    val activeClockSize = if (isPhoto) photoClockSize else videoClockSize
    val activeClockBold = if (isPhoto) photoClockBold else videoClockBold
    var localPath by remember(watchFaceId) { mutableStateOf(currentPath) }
    var localClockPosition by remember(watchFaceId) { mutableStateOf(activeClockPosition) }
    var localClockSize by remember(watchFaceId) { mutableFloatStateOf(activeClockSize.toFloat()) }
    var localClockBold by remember(watchFaceId) { mutableStateOf(activeClockBold) }
    var localVideoFillScreen by remember(watchFaceId) { mutableStateOf(videoFillScreen) }
    var localVideoClockColorMode by remember(watchFaceId) { mutableStateOf(videoClockColorMode) }

    androidx.compose.runtime.LaunchedEffect(
        watchFaceId,
        currentPath,
        activeClockPosition,
        activeClockSize,
        activeClockBold,
        videoFillScreen,
        videoClockColorMode
    ) {
        localPath = currentPath
        localClockPosition = activeClockPosition
        localClockSize = activeClockSize.toFloat()
        localClockBold = activeClockBold
        localVideoFillScreen = videoFillScreen
        localVideoClockColorMode = videoClockColorMode
    }

    fun persistPendingChanges() {
        if (isPhoto) {
            if (localPath != photoPath) vm.setBuiltInPhotoPath(localPath)
            if (localClockPosition != photoClockPosition) vm.setBuiltInPhotoClockPosition(localClockPosition)
            if (localClockSize.toInt() != photoClockSize) vm.setBuiltInPhotoClockSize(localClockSize.toInt())
            if (localClockBold != photoClockBold) vm.setBuiltInPhotoClockBold(localClockBold)
        } else {
            if (localPath != videoPath) vm.setBuiltInVideoPath(localPath)
            if (localClockPosition != videoClockPosition) vm.setBuiltInVideoClockPosition(localClockPosition)
            if (localClockSize.toInt() != videoClockSize) vm.setBuiltInVideoClockSize(localClockSize.toInt())
            if (localClockBold != videoClockBold) vm.setBuiltInVideoClockBold(localClockBold)
            if (localVideoFillScreen != videoFillScreen) vm.setBuiltInVideoFillScreen(localVideoFillScreen)
            if (localVideoClockColorMode != videoClockColorMode) vm.setBuiltInVideoClockColorMode(localVideoClockColorMode)
        }
    }

    val picker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            val savedPath = handlePickedMedia(
                application = vm.getApplication(),
                watchFaceId = watchFaceId,
                sourceUri = uri,
                onMessage = { message ->
                    Toast.makeText(vm.getApplication(), message, Toast.LENGTH_SHORT).show()
                }
            )
            if (!savedPath.isNullOrBlank()) {
                localPath = savedPath
                if (isPhoto) {
                    vm.setBuiltInPhotoPath(savedPath)
                } else {
                    vm.setBuiltInVideoPath(savedPath)
                }
            }
        }
    }
    val fileManagerPicker = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val uriText = result.data?.getStringExtra(EXTRA_FILE_MANAGER_RESULT_URI).orEmpty()
        val uri = uriText.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return@rememberLauncherForActivityResult
        val savedPath = handlePickedMedia(
            application = vm.getApplication(),
            watchFaceId = watchFaceId,
            sourceUri = uri,
            onMessage = { message ->
                Toast.makeText(vm.getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        )
        if (!savedPath.isNullOrBlank()) {
            localPath = savedPath
            if (isPhoto) vm.setBuiltInPhotoPath(savedPath) else vm.setBuiltInVideoPath(savedPath)
        }
    }

    BackHandler {
        persistPendingChanges()
        onBack()
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFirstFrame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .flueRotaryScrollable(focusRequester, 0.9f) { rotaryDelta ->
                scope.launch { scrollState.scrollBy(-rotaryDelta) }
            }
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isPhoto) "\u56FE\u7247\u8868\u76D8\u8BBE\u7F6E" else "\u89C6\u9891\u8868\u76D8\u8BBE\u7F6E",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isPhoto) "\u9009\u62E9\u4E00\u5F20\u56FE\u7247\u4F5C\u4E3A\u8868\u76D8\u80CC\u666F" else "\u9009\u62E9\u4E00\u4E2A\u89C6\u9891\u4F5C\u4E3A\u8868\u76D8\u80CC\u666F",
            color = WatchColors.TextTertiary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF10141D)),
            contentAlignment = Alignment.Center
        ) {
            BuiltInWatchFacePreview(
                watchFaceId = watchFaceId,
                photoPath = if (isPhoto) localPath else photoPath,
                videoPath = if (isPhoto) videoPath else localPath,
                photoOptions = BuiltInWatchFaceOptions(
                    clockPosition = if (isPhoto) localClockPosition else photoClockPosition,
                    clockSizeSp = localClockSize.toInt(),
                    boldClock = if (isPhoto) localClockBold else photoClockBold
                ),
                videoOptions = BuiltInWatchFaceOptions(
                    clockPosition = if (isPhoto) videoClockPosition else localClockPosition,
                    clockSizeSp = localClockSize.toInt(),
                    boldClock = if (isPhoto) videoClockBold else localClockBold,
                    cropToFill = if (isPhoto) videoFillScreen else localVideoFillScreen,
                    clockColorMode = if (isPhoto) videoClockColorMode else localVideoClockColorMode
                ),
                clockOverride = FIXED_PREVIEW_CLOCK,
                showClock = true,
                playVideo = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "\u65F6\u95F4\u4F4D\u7F6E",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        PositionPickerRow(
            current = localClockPosition,
            onSelect = {
                localClockPosition = it
                if (isPhoto) {
                    vm.setBuiltInPhotoClockPosition(it)
                } else {
                    vm.setBuiltInVideoClockPosition(it)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "\u65F6\u95F4\u5927\u5C0F  ${localClockSize.toInt()}sp",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = localClockSize,
            onValueChange = {
                localClockSize = it
            },
            onValueChangeFinished = {
                val value = localClockSize.toInt()
                if (isPhoto) vm.setBuiltInPhotoClockSize(value) else vm.setBuiltInVideoClockSize(value)
            },
            valueRange = 28f..92f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = WatchColors.ActiveCyan,
                activeTrackColor = WatchColors.ActiveCyan
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        ToggleRow(
            label = "\u7C97\u4F53\u65F6\u949F",
            enabled = localClockBold,
            onToggle = {
                localClockBold = !localClockBold
                if (isPhoto) {
                    vm.setBuiltInPhotoClockBold(localClockBold)
                } else {
                    vm.setBuiltInVideoClockBold(localClockBold)
                }
            }
        )

        if (!isPhoto) {
            Spacer(modifier = Modifier.height(8.dp))
            ToggleRow(
                label = "\u89C6\u9891\u94FA\u6EE1\u5168\u5C4F",
                enabled = localVideoFillScreen,
                onToggle = {
                    localVideoFillScreen = !localVideoFillScreen
                    vm.setBuiltInVideoFillScreen(localVideoFillScreen)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u65F6\u949F\u989C\u8272",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClockColorPickerRow(
                current = localVideoClockColorMode,
                onSelect = {
                    localVideoClockColorMode = it
                    vm.setBuiltInVideoClockColorMode(it)
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ActionButton(
            text = if (localPath.isNullOrBlank()) {
                if (isPhoto) "\u9009\u62E9\u56FE\u7247" else "\u9009\u62E9\u89C6\u9891"
            } else {
                if (isPhoto) "\u66F4\u6362\u56FE\u7247" else "\u66F4\u6362\u89C6\u9891"
            },
            onLongClick = {
                val intent = Intent(vm.getApplication(), BuiltInFileManagerActivity::class.java).apply {
                    putExtra(
                        EXTRA_FILE_MANAGER_MODE,
                        if (isPhoto) FILE_MANAGER_MODE_IMAGE else FILE_MANAGER_MODE_VIDEO
                    )
                }
                fileManagerPicker.launch(intent)
            }
        ) {
            picker.launch(arrayOf(if (isPhoto) "image/*" else "video/*"))
        }

        Spacer(modifier = Modifier.height(10.dp))

        ActionButton(
            text = if (isPhoto) "\u6E05\u9664\u56FE\u7247" else "\u6E05\u9664\u89C6\u9891",
            enabled = !localPath.isNullOrBlank()
        ) {
            localPath = null
            if (isPhoto) {
                vm.setBuiltInPhotoPath(null)
            } else {
                vm.setBuiltInVideoPath(null)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ActionButton(text = "\u8FD4\u56DE") {
            persistPendingChanges()
            onBack()
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.958f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170),
        label = "internal_action_press_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fishtailScale()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) WatchColors.SurfaceGlass else Color.White.copy(alpha = 0.05f))
            .pointerInput(enabled, onLongClick) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = { onLongClick?.invoke() },
                    onTap = { onClick() }
                )
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) WatchColors.ActiveCyan else WatchColors.TextTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun handlePickedMedia(
    application: Application,
    watchFaceId: String,
    sourceUri: Uri,
    onMessage: (String) -> Unit
): String? {
    val context = application
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            sourceUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    val savedPath = if (watchFaceId == BUILT_IN_PHOTO_WATCHFACE_ID) {
        InternalWatchFaceStorage.copyPhoto(context, sourceUri)
    } else {
        InternalWatchFaceStorage.copyVideo(context, sourceUri)
    }

    if (savedPath.isNullOrBlank()) {
        onMessage("\u4FDD\u5B58\u5A92\u4F53\u5931\u8D25")
        return null
    }
    onMessage("\u8868\u76D8\u5A92\u4F53\u5DF2\u66F4\u65B0")
    return savedPath
}

@Composable
private fun PositionPickerRow(
    current: WatchClockPosition,
    onSelect: (WatchClockPosition) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            listOf(
                WatchClockPosition.TOP_LEFT to "\u5DE6\u4E0A",
                WatchClockPosition.TOP_CENTER to "\u4E2D\u4E0A",
                WatchClockPosition.TOP_RIGHT to "\u53F3\u4E0A"
            ),
            listOf(
                WatchClockPosition.LEFT_CENTER to "\u5DE6\u4E2D",
                WatchClockPosition.CENTER to "\u4E2D\u95F4",
                WatchClockPosition.RIGHT_CENTER to "\u53F3\u4E2D"
            ),
            listOf(
                WatchClockPosition.BOTTOM_LEFT to "\u5DE6\u4E0B",
                WatchClockPosition.BOTTOM_CENTER to "\u4E2D\u4E0B",
                WatchClockPosition.BOTTOM_RIGHT to "\u53F3\u4E0B"
            )
        ).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                rowItems.forEach { item ->
                    if (item == null) {
                        Spacer(
                            modifier = Modifier
                                .width(92.dp)
                                .padding(horizontal = 4.dp)
                        )
                    } else {
                        val (position, label) = item
                        SmallChoiceChip(
                            label = label,
                            selected = position == current,
                            modifier = Modifier
                                .width(92.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            onSelect(position)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockColorPickerRow(
    current: WatchClockColorMode,
    onSelect: (WatchClockColorMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        SmallChoiceChip(
            label = "\u767D\u8272",
            selected = current == WatchClockColorMode.WHITE,
            modifier = Modifier.width(120.dp)
        ) { onSelect(WatchClockColorMode.WHITE) }
        SmallChoiceChip(
            label = "\u9ED1\u8272",
            selected = current == WatchClockColorMode.BLACK,
            modifier = Modifier.width(120.dp)
        ) { onSelect(WatchClockColorMode.BLACK) }
    }
}

@Composable
private fun SmallChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.958f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170),
        label = "clock_position_chip_press_scale"
    )
    Box(
        modifier = modifier
            .fishtailScale()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WatchColors.ActiveCyan.copy(alpha = 0.22f) else WatchColors.SurfaceGlass)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) WatchColors.ActiveCyan else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.958f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170),
        label = "toggle_row_press_scale"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (enabled) 23.dp else 3.dp,
        animationSpec = spring(stiffness = 760f, dampingRatio = 0.82f),
        label = "clock_toggle_knob_offset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (enabled) WatchColors.ActiveGreen else Color.White.copy(alpha = 0.18f),
        label = "clock_toggle_track_color"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fishtailScale()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(start = knobOffset)
                    .width(20.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun Modifier.fishtailScale(): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenCenter = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() / 2f
    }
    var itemCenter by remember { mutableFloatStateOf(screenCenter) }
    val scale = if (itemCenter <= screenCenter) {
        1f
    } else {
        val normalized = ((itemCenter - screenCenter) / screenCenter).coerceIn(0f, 1f)
        (1f - normalized * 0.14f).coerceIn(0.86f, 1f)
    }
    return this
        .onGloballyPositioned { coords ->
            itemCenter = coords.positionInWindow().y + coords.size.height / 2f
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = scale.coerceIn(0.65f, 1f)
        }
}
