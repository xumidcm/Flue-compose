package com.flue.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flue.launcher.ui.input.flueRotaryScrollable
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.ui.theme.WatchLauncherTheme
import com.flue.launcher.util.RecentsVisibility
import com.flue.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val EXTRA_FILE_MANAGER_MODE = "file_manager_mode"
const val EXTRA_FILE_MANAGER_RESULT_URI = "file_manager_result_uri"
const val FILE_MANAGER_MODE_IMAGE = "image"
const val FILE_MANAGER_MODE_VIDEO = "video"

class BuiltInFileManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        val mode = intent.getStringExtra(EXTRA_FILE_MANAGER_MODE)
            ?.takeIf { it == FILE_MANAGER_MODE_IMAGE || it == FILE_MANAGER_MODE_VIDEO }
            ?: FILE_MANAGER_MODE_IMAGE
        val requiredPermission = storagePermissionForMode(mode)
        val hasPermission = requiredPermission == null || ContextCompat.checkSelfPermission(
            this,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
        val permissionState = mutableStateOf(hasPermission)
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionState.value = granted
        }

        setContent {
            WatchLauncherTheme {
                BuiltInFileManagerScreen(
                    mode = mode,
                    hasPermission = permissionState.value,
                    onRequestPermission = {
                        if (requiredPermission != null) {
                            permissionLauncher.launch(requiredPermission)
                        }
                    },
                    onPick = { uri ->
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_FILE_MANAGER_RESULT_URI, uri.toString()))
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

private fun storagePermissionForMode(mode: String): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return if (mode == FILE_MANAGER_MODE_VIDEO) Manifest.permission.READ_MEDIA_VIDEO
        else Manifest.permission.READ_MEDIA_IMAGES
    }
    return Manifest.permission.READ_EXTERNAL_STORAGE
}

@Composable
private fun BuiltInFileManagerScreen(
    mode: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPick: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: LauncherViewModel = viewModel()
    val showThumbnails by vm.builtInManagerThumbnails.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val mediaList by produceState<List<MediaItem>?>(initialValue = null, mode, hasPermission) {
        value = if (hasPermission) withContext(Dispatchers.IO) { queryMedia(context, mode) } else emptyList()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFirstFrame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .flueRotaryScrollable(focusRequester, 0.95f) { rotaryDelta ->
                scope.launch { listState.scrollBy(-rotaryDelta) }
            }
    ) {
        Text(
            text = if (mode == FILE_MANAGER_MODE_VIDEO) "内置文件管理器（视频）" else "内置文件管理器（图片）",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (!hasPermission) {
            Text(
                text = "需要存储权限才能读取媒体文件",
                color = WatchColors.TextTertiary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            FileManagerActionButton(text = "申请权限", onClick = onRequestPermission)
            Spacer(modifier = Modifier.height(8.dp))
            FileManagerActionButton(text = "返回", onClick = onBack)
            return
        }

        if (mediaList == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = WatchColors.ActiveCyan)
            }
        } else if (mediaList!!.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == FILE_MANAGER_MODE_VIDEO) "未发现可用视频" else "未发现可用图片",
                    color = WatchColors.TextTertiary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mediaList!!, key = { it.uri.toString() }) { item ->
                    val subTitle = remember(item) {
                        val createdLabel = item.createdAtMillis?.let(::formatMediaCreatedAt) ?: "未知时间"
                        "$createdLabel\n${item.path}"
                    }
                    val scale = itemFisheye(listState, item.uri.toString())
                    val interaction = remember(item.uri) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        if (pressed) 0.958f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170),
                        label = "manager_item_press_scale"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale * pressScale
                                scaleY = scale * pressScale
                                alpha = scale.coerceIn(0.6f, 1f)
                            }
                            .background(WatchColors.SurfaceGlass, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = interaction, indication = null) { onPick(item.uri) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showThumbnails) {
                                MediaThumb(uri = item.uri, isVideo = mode == FILE_MANAGER_MODE_VIDEO)
                                Spacer(modifier = Modifier.size(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.displayName, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = subTitle, color = WatchColors.TextTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FileManagerActionButton(text = "返回", onClick = onBack)
    }
}

@Composable
private fun MediaThumb(uri: Uri, isVideo: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cacheKey = remember(uri, isVideo) { "${if (isVideo) "v" else "i"}:${uri}" }
    val thumbnail by produceState<Bitmap?>(initialValue = MediaThumbCache.get(cacheKey), cacheKey, uri, isVideo) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(88, 88), null)
                } else if (isVideo) {
                    decodeVideoFrameThumbnail(context, uri, 96)
                } else {
                    decodeSampledBitmap(context, uri, 96)
                }
            }.getOrNull()?.also { MediaThumbCache.put(cacheKey, it) }
        }
    }
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
        )
    } else {
        Spacer(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
        )
    }
}

private object MediaThumbCache {
    private val cache = object : LruCache<String, Bitmap>(90) {}

    fun get(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: Bitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }
}

private fun decodeVideoFrameThumbnail(context: Context, uri: Uri, reqSize: Int): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(context, uri)
        val raw = retriever.frameAtTime ?: return null
        Bitmap.createScaledBitmap(raw, reqSize, reqSize, true)
    }.getOrNull().also {
        runCatching { retriever.release() }
    }
}

private fun decodeSampledBitmap(context: Context, uri: Uri, reqSize: Int): android.graphics.Bitmap? {
    val resolver = context.contentResolver
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize >= reqSize * 2 && bounds.outHeight / sampleSize >= reqSize * 2) {
        sampleSize *= 2
    }

    val opts = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sampleSize.coerceAtLeast(1)
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
}

@Composable
private fun itemFisheye(state: LazyListState, key: Any): Float {
    val info = state.layoutInfo.visibleItemsInfo.find { it.key == key } ?: return 0.9f
    val center = state.layoutInfo.viewportEndOffset / 2f
    val itemCenter = info.offset + info.size / 2f
    if (itemCenter <= center) return 1f
    val normalized = ((itemCenter - center) / center).coerceIn(0f, 1f)
    return (1f - normalized * 0.14f).coerceIn(0.86f, 1f)
}

private data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val createdAtMillis: Long?,
    val path: String
)

private fun queryMedia(context: Context, mode: String): List<MediaItem> {
    val resolver = context.contentResolver
    val baseUri = if (mode == FILE_MANAGER_MODE_VIDEO) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.MediaColumns.RELATIVE_PATH
    } else {
        MediaStore.MediaColumns.DATA
    }
    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    val items = mutableListOf<MediaItem>()

    fun collectItems(projection: Array<String>) {
        resolver.query(baseUri, projection, null, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val pathIndex = cursor.getColumnIndex(pathColumn)
            while (cursor.moveToNext() && items.size < 300) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val dateAddedSeconds = if (dateAddedIndex >= 0) cursor.getLong(dateAddedIndex) else 0L
                val path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else ""
                items += MediaItem(
                    uri = ContentUris.withAppendedId(baseUri, id),
                    displayName = name.ifBlank { "未命名文件" },
                    createdAtMillis = dateAddedSeconds.takeIf { it > 0L }?.times(1000L),
                    path = path.ifBlank { "未知路径" }
                )
            }
        }
    }

    try {
        collectItems(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                pathColumn
            )
        )
    } catch (_: SQLiteException) {
        items.clear()
        collectItems(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
            )
        )
    } catch (_: IllegalArgumentException) {
        items.clear()
        collectItems(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
            )
        )
    }

    return items
}

private fun formatMediaCreatedAt(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}

@Composable
private fun FileManagerActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed) 0.958f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170),
        label = "manager_action_press_scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .background(if (enabled) WatchColors.SurfaceGlass else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = if (enabled) WatchColors.ActiveCyan else WatchColors.TextTertiary,
            fontSize = 14.sp
        )
    }
}
