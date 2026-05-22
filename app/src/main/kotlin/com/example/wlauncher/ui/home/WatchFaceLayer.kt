package com.flue.launcher.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.BUILT_IN_PHOTO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_VIDEO_WATCHFACE_ID
import com.flue.launcher.watchface.BUILT_IN_WATCHFACE_ID
import com.flue.launcher.watchface.WatchClockColorMode
import com.flue.launcher.watchface.WatchClockPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClockSnapshot(
    val time: String,
    val date: String
)

val FIXED_PREVIEW_CLOCK = ClockSnapshot(
    time = "12:31",
    date = "\u0031\u0031\u6708\u0031\u0033\u65e5"
)

data class ClockPalette(
    val timeColor: Color,
    val dateColor: Color
)

@Composable
fun WatchFaceLayer(
    watchFaceId: String = BUILT_IN_WATCHFACE_ID,
    photoPath: String? = null,
    videoPath: String? = null,
    isFaceVisible: Boolean = true,
    photoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    videoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BuiltInWatchFaceSurface(
        watchFaceId = watchFaceId,
        photoPath = photoPath,
        videoPath = videoPath,
        isFaceVisible = isFaceVisible,
        photoOptions = photoOptions,
        videoOptions = videoOptions,
        clockOverride = null,
        onLongPress = onLongPress,
        showClock = true,
        modifier = modifier
    )
}

@Composable
fun BuiltInWatchFacePreview(
    watchFaceId: String,
    photoPath: String? = null,
    videoPath: String? = null,
    photoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    videoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    clockOverride: ClockSnapshot? = FIXED_PREVIEW_CLOCK,
    modifier: Modifier = Modifier,
    showClock: Boolean = false,
    playVideo: Boolean = true
) {
    BuiltInWatchFaceSurface(
        watchFaceId = watchFaceId,
        photoPath = photoPath,
        videoPath = videoPath,
        isFaceVisible = playVideo,
        photoOptions = photoOptions,
        videoOptions = videoOptions,
        clockOverride = clockOverride,
        onLongPress = null,
        showClock = showClock,
        modifier = modifier
    )
}

@Composable
private fun BuiltInWatchFaceSurface(
    watchFaceId: String,
    photoPath: String?,
    videoPath: String?,
    isFaceVisible: Boolean,
    photoOptions: BuiltInWatchFaceOptions,
    videoOptions: BuiltInWatchFaceOptions,
    clockOverride: ClockSnapshot?,
    onLongPress: (() -> Unit)?,
    showClock: Boolean,
    modifier: Modifier = Modifier
) {
    val liveClock = rememberClockSnapshot()
    val clock = clockOverride ?: liveClock
    val clockPalette = rememberClockPalette(
        watchFaceId = watchFaceId,
        photoPath = photoPath,
        clockPosition = when (watchFaceId) {
            BUILT_IN_PHOTO_WATCHFACE_ID -> photoOptions.clockPosition
            BUILT_IN_VIDEO_WATCHFACE_ID -> videoOptions.clockPosition
            else -> WatchClockPosition.CENTER
        },
        videoClockColorMode = videoOptions.clockColorMode
    )
    val backgroundModifier = modifier
        .fillMaxSize()
        .then(
            if (onLongPress != null) {
                Modifier.pointerInput(onLongPress) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
            } else {
                Modifier
            }
        )

    Box(
        modifier = backgroundModifier
            .graphicsLayer { clip = true }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (watchFaceId) {
            BUILT_IN_PHOTO_WATCHFACE_ID -> {
                MediaImageBackground(photoPath)
            }

            BUILT_IN_VIDEO_WATCHFACE_ID -> {
                MediaVideoBackground(videoPath, isFaceVisible, videoOptions.cropToFill)
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF16304A), Color(0xFF0B1322), Color.Black),
                                radius = 900f
                            )
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )

        if (showClock) {
            ClockOverlay(
                clock = clock,
                compact = false,
                clockPosition = when (watchFaceId) {
                    BUILT_IN_PHOTO_WATCHFACE_ID -> photoOptions.clockPosition
                    BUILT_IN_VIDEO_WATCHFACE_ID -> videoOptions.clockPosition
                    else -> WatchClockPosition.CENTER
                },
                clockSizeSp = when (watchFaceId) {
                    BUILT_IN_PHOTO_WATCHFACE_ID -> photoOptions.clockSizeSp
                    BUILT_IN_VIDEO_WATCHFACE_ID -> videoOptions.clockSizeSp
                    else -> 64
                },
                boldClock = when (watchFaceId) {
                    BUILT_IN_PHOTO_WATCHFACE_ID -> photoOptions.boldClock
                    BUILT_IN_VIDEO_WATCHFACE_ID -> videoOptions.boldClock
                    else -> false
                },
                palette = clockPalette,
                fallbackTitle = when (watchFaceId) {
                    BUILT_IN_PHOTO_WATCHFACE_ID -> if (photoPath.isNullOrBlank()) "\u672A\u8BBE\u7F6E\u56FE\u7247" else null
                    BUILT_IN_VIDEO_WATCHFACE_ID -> if (videoPath.isNullOrBlank()) "\u672A\u8BBE\u7F6E\u89C6\u9891" else null
                    else -> null
                },
                fallbackSubtitle = when (watchFaceId) {
                    BUILT_IN_PHOTO_WATCHFACE_ID -> if (photoPath.isNullOrBlank()) "\u5728\u8868\u76D8\u8BBE\u7F6E\u91CC\u9009\u62E9\u4E00\u5F20\u56FE\u7247" else null
                    BUILT_IN_VIDEO_WATCHFACE_ID -> if (videoPath.isNullOrBlank()) "\u5728\u8868\u76D8\u8BBE\u7F6E\u91CC\u9009\u62E9\u4E00\u4E2A\u89C6\u9891" else null
                    else -> null
                }
            )
        }
    }
}

@Composable
private fun MediaImageBackground(path: String?) {
    val filePath = path?.takeIf { File(it).exists() }
    AndroidView(
        factory = { imageContext ->
            ImageView(imageContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            if (filePath == null) {
                imageView.setImageDrawable(null)
            } else {
                imageView.setImageURI(File(filePath).toUri())
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = true }
    )
}

@Composable
private fun MediaVideoBackground(path: String?, isFaceVisible: Boolean, fillScreen: Boolean) {
    val filePath = path?.takeIf { File(it).exists() }
    if (!isFaceVisible) {
        MediaVideoThumbnail(filePath)
        return
    }
    AndroidView(
        factory = { context -> InternalLoopingVideoView(context) },
        update = { view ->
            view.bind(filePath, isFaceVisible, fillScreen)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MediaVideoThumbnail(path: String?) {
    val frameBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = runCatching {
            if (path.isNullOrBlank()) return@runCatching null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                retriever.frameAtTime
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }
    if (frameBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = frameBitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}

@Composable
fun ClockOverlayPreview(
    clock: ClockSnapshot,
    clockPosition: WatchClockPosition,
    clockSizeSp: Int,
    boldClock: Boolean = false,
    palette: ClockPalette,
    fallbackTitle: String? = null,
    fallbackSubtitle: String? = null
) {
    ClockOverlay(
        clock = clock,
        compact = false,
        clockPosition = clockPosition,
        clockSizeSp = clockSizeSp,
        boldClock = boldClock,
        palette = palette,
        fallbackTitle = fallbackTitle,
        fallbackSubtitle = fallbackSubtitle
    )
}

@Composable
private fun ClockOverlay(
    clock: ClockSnapshot,
    compact: Boolean,
    clockPosition: WatchClockPosition,
    clockSizeSp: Int,
    boldClock: Boolean,
    palette: ClockPalette,
    fallbackTitle: String? = null,
    fallbackSubtitle: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = clockAlignment(clockPosition)
    ) {
        Column(horizontalAlignment = horizontalAlignment(clockPosition)) {
            Text(
                text = clock.time,
                fontSize = if (compact) 32.sp else clockSizeSp.sp,
                fontWeight = if (boldClock) FontWeight.Bold else FontWeight.W200,
                color = palette.timeColor,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 6.dp))
            Text(
                text = clock.date,
                fontSize = if (compact) 10.sp else (clockSizeSp * 0.24f).sp,
                fontWeight = FontWeight.W500,
                color = palette.dateColor
            )
            if (fallbackTitle != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = fallbackTitle,
                    fontSize = (clockSizeSp * 0.28f).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.timeColor
                )
                fallbackSubtitle?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        fontSize = (clockSizeSp * 0.18f).sp,
                        color = palette.timeColor.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberClockSnapshot(): ClockSnapshot {
    var snapshot by remember {
        mutableStateOf(ClockSnapshot(time = "--:--", date = ""))
    }
    LaunchedEffect(Unit) {
        val locale = Locale.getDefault()
        val timeFmt = SimpleDateFormat("HH:mm", locale)
        val dateFmt = SimpleDateFormat(
            if (locale.language.startsWith("zh")) "M\u6708d\u65E5 EEEE" else "MMM d, EEEE",
            locale
        )
        while (true) {
            val now = Date()
            snapshot = ClockSnapshot(
                time = timeFmt.format(now),
                date = dateFmt.format(now)
            )
            delay(1000)
        }
    }
    return snapshot
}

@Composable
fun rememberClockPaletteForPreview(
    watchFaceId: String,
    photoPath: String?,
    clockPosition: WatchClockPosition
): ClockPalette = rememberClockPalette(
    watchFaceId = watchFaceId,
    photoPath = photoPath,
    clockPosition = clockPosition,
    videoClockColorMode = WatchClockColorMode.AUTO
)

@Composable
private fun rememberClockPalette(
    watchFaceId: String,
    photoPath: String?,
    clockPosition: WatchClockPosition,
    videoClockColorMode: WatchClockColorMode
): ClockPalette {
    var palette by remember(watchFaceId, photoPath, clockPosition, videoClockColorMode) {
        mutableStateOf(defaultClockPalette(watchFaceId))
    }
    LaunchedEffect(watchFaceId, photoPath, clockPosition, videoClockColorMode) {
        palette = if (watchFaceId == BUILT_IN_PHOTO_WATCHFACE_ID && !photoPath.isNullOrBlank()) {
            sampleClockPalette(photoPath, clockPosition) ?: defaultClockPalette(watchFaceId)
        } else if (watchFaceId == BUILT_IN_VIDEO_WATCHFACE_ID) {
            when (videoClockColorMode) {
                WatchClockColorMode.WHITE -> ClockPalette(timeColor = Color.White, dateColor = Color.White.copy(alpha = 0.82f))
                WatchClockColorMode.BLACK -> ClockPalette(timeColor = Color(0xFF101318), dateColor = Color(0xCC101318))
                WatchClockColorMode.AUTO -> defaultClockPalette(watchFaceId)
            }
        } else {
            defaultClockPalette(watchFaceId)
        }
    }
    return palette
}

private fun defaultClockPalette(watchFaceId: String): ClockPalette =
    if (watchFaceId == BUILT_IN_PHOTO_WATCHFACE_ID) {
        ClockPalette(timeColor = Color.White, dateColor = Color.White.copy(alpha = 0.82f))
    } else {
        ClockPalette(timeColor = Color.White, dateColor = Color(0xFF7BE8FF))
    }

private suspend fun sampleClockPalette(
    path: String,
    position: WatchClockPosition
): ClockPalette? = withContext(Dispatchers.Default) {
    runCatching {
        val file = File(path)
        if (!file.exists()) return@runCatching null
        val options = BitmapFactory.Options().apply { inSampleSize = 16 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@runCatching null
        val xRange = when (position) {
            WatchClockPosition.TOP_LEFT, WatchClockPosition.BOTTOM_LEFT -> 0 until (bitmap.width * 4 / 10).coerceAtLeast(1)
            WatchClockPosition.TOP_RIGHT, WatchClockPosition.BOTTOM_RIGHT -> (bitmap.width * 6 / 10).coerceAtMost(bitmap.width - 1) until bitmap.width
            WatchClockPosition.LEFT_CENTER -> 0 until (bitmap.width * 4 / 10).coerceAtLeast(1)
            WatchClockPosition.RIGHT_CENTER -> (bitmap.width * 6 / 10).coerceAtMost(bitmap.width - 1) until bitmap.width
            WatchClockPosition.CENTER, WatchClockPosition.TOP_CENTER, WatchClockPosition.BOTTOM_CENTER ->
                (bitmap.width * 3 / 10).coerceAtMost(bitmap.width - 1) until (bitmap.width * 7 / 10).coerceAtLeast(1)
        }
        val yRange = when (position) {
            WatchClockPosition.TOP_LEFT, WatchClockPosition.TOP_RIGHT, WatchClockPosition.TOP_CENTER ->
                0 until (bitmap.height * 4 / 10).coerceAtLeast(1)
            WatchClockPosition.BOTTOM_LEFT, WatchClockPosition.BOTTOM_RIGHT, WatchClockPosition.BOTTOM_CENTER ->
                (bitmap.height * 6 / 10).coerceAtMost(bitmap.height - 1) until bitmap.height
            WatchClockPosition.LEFT_CENTER, WatchClockPosition.RIGHT_CENTER -> (bitmap.height * 3 / 10).coerceAtMost(bitmap.height - 1) until (bitmap.height * 7 / 10).coerceAtLeast(1)
            WatchClockPosition.CENTER -> (bitmap.height * 3 / 10).coerceAtMost(bitmap.height - 1) until (bitmap.height * 7 / 10).coerceAtLeast(1)
        }
        var luminanceSum = 0.0
        var count = 0
        for (x in xRange step 2) {
            for (y in yRange step 2) {
                val color = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
                val red = android.graphics.Color.red(color) / 255.0
                val green = android.graphics.Color.green(color) / 255.0
                val blue = android.graphics.Color.blue(color) / 255.0
                luminanceSum += 0.2126 * red + 0.7152 * green + 0.0722 * blue
                count++
            }
        }
        bitmap.recycle()
        val luminance = if (count == 0) 0.0 else luminanceSum / count
        if (luminance >= 0.58) {
            ClockPalette(
                timeColor = Color(0xFF0F1720),
                dateColor = Color(0xCC0F1720)
            )
        } else {
            ClockPalette(
                timeColor = Color.White,
                dateColor = Color.White.copy(alpha = 0.82f)
            )
        }
    }.getOrNull()
}

private fun clockAlignment(position: WatchClockPosition): Alignment = when (position) {
    WatchClockPosition.CENTER -> Alignment.Center
    WatchClockPosition.TOP_CENTER -> Alignment.TopCenter
    WatchClockPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    WatchClockPosition.LEFT_CENTER -> Alignment.CenterStart
    WatchClockPosition.RIGHT_CENTER -> Alignment.CenterEnd
    WatchClockPosition.TOP_LEFT -> Alignment.TopStart
    WatchClockPosition.TOP_RIGHT -> Alignment.TopEnd
    WatchClockPosition.BOTTOM_LEFT -> Alignment.BottomStart
    WatchClockPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
}

private fun horizontalAlignment(position: WatchClockPosition): Alignment.Horizontal = when (position) {
    WatchClockPosition.CENTER, WatchClockPosition.TOP_CENTER, WatchClockPosition.BOTTOM_CENTER -> Alignment.CenterHorizontally
    WatchClockPosition.RIGHT_CENTER -> Alignment.End
    WatchClockPosition.TOP_RIGHT, WatchClockPosition.BOTTOM_RIGHT -> Alignment.End
    else -> Alignment.Start
}

private class InternalLoopingVideoView(context: Context) : FrameLayout(context) {
    private val videoView = VideoView(context)
    private val placeholder = TextView(context).apply {
        text = ""
        setTextColor(android.graphics.Color.WHITE)
        textSize = 15f
        gravity = android.view.Gravity.CENTER
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
    private var currentPath: String? = null
    private var shouldPlay: Boolean = true
    private var fillScreen: Boolean = true
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    init {
        addView(
            videoView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = android.view.Gravity.CENTER
            }
        )
        addView(
            placeholder,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        videoView.setOnPreparedListener { mediaPlayer ->
            videoWidth = mediaPlayer.videoWidth
            videoHeight = mediaPlayer.videoHeight
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
            updateScale()
            if (shouldPlay) {
                videoView.start()
            }
        }
        videoView.setOnErrorListener { _, _, _ -> true }
    }

    fun bind(path: String?, play: Boolean, fillScreen: Boolean) {
        shouldPlay = play
        this.fillScreen = fillScreen
        placeholder.visibility = if (path == null) VISIBLE else GONE
        videoView.visibility = if (path == null) INVISIBLE else VISIBLE
        if (path != currentPath) {
            currentPath = path
            if (path == null) {
                videoView.stopPlayback()
                runCatching { videoView.setVideoURI(null) }
            } else {
                videoView.setVideoPath(path)
            }
        }
        updateScale()
        if (path == null) return
        if (play) {
            if (!videoView.isPlaying) videoView.start()
        } else if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScale()
    }

    private fun updateScale() {
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) {
            videoView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = android.view.Gravity.CENTER
            }
            videoView.scaleX = 1f
            videoView.scaleY = 1f
            videoView.translationX = 0f
            videoView.translationY = 0f
            return
        }

        val containerWidth = width.toFloat()
        val containerHeight = height.toFloat()
        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
        val containerAspect = containerWidth / containerHeight

        val (targetWidth, targetHeight) = if (fillScreen) {
            if (videoAspect > containerAspect) {
                (containerHeight * videoAspect) to containerHeight
            } else {
                containerWidth to (containerWidth / videoAspect)
            }
        } else {
            if (videoAspect > containerAspect) {
                containerWidth to (containerWidth / videoAspect)
            } else {
                (containerHeight * videoAspect) to containerHeight
            }
        }

        val widthPx = targetWidth.toInt().coerceAtLeast(1)
        val heightPx = targetHeight.toInt().coerceAtLeast(1)
        val layoutParams = (videoView.layoutParams as? LayoutParams)
            ?: LayoutParams(widthPx, heightPx)
        if (layoutParams.width != widthPx || layoutParams.height != heightPx || layoutParams.gravity != android.view.Gravity.CENTER) {
            layoutParams.width = widthPx
            layoutParams.height = heightPx
            layoutParams.gravity = android.view.Gravity.CENTER
            videoView.layoutParams = layoutParams
        }
        videoView.scaleX = 1f
        videoView.scaleY = 1f
        videoView.translationX = 0f
        videoView.translationY = 0f
    }

    override fun onDetachedFromWindow() {
        runCatching { videoView.stopPlayback() }
        super.onDetachedFromWindow()
    }
}
