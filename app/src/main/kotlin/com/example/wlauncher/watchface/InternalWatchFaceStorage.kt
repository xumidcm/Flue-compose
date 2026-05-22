package com.flue.launcher.watchface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.roundToInt

object InternalWatchFaceStorage {
    private const val PHOTO_DIR = "photo"
    private const val VIDEO_DIR = "video"
    private const val FONT_DIR = "font"

    fun copyPhoto(context: Context, uri: Uri): String? = runCatching {
        val root = File(context.filesDir, "internal_watchfaces/$PHOTO_DIR").apply { mkdirs() }
        root.listFiles()?.forEach { existing ->
            if (existing.isFile) existing.delete()
        }

        val metrics = context.resources.displayMetrics
        val targetWidth = metrics.widthPixels.coerceAtLeast(1)
        val targetHeight = metrics.heightPixels.coerceAtLeast(1)
        val croppedBitmap = decodeCroppedPhoto(context, uri, targetWidth, targetHeight) ?: return null
        val target = File(root, "current_${System.currentTimeMillis()}.png")
        target.outputStream().use { output ->
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        croppedBitmap.recycle()
        target.absolutePath
    }.getOrNull()

    fun copyVideo(context: Context, uri: Uri): String? = copyMedia(context, uri, VIDEO_DIR, "video")

    fun copyFont(context: Context, uri: Uri): String? = runCatching {
        val root = File(context.filesDir, "internal_watchfaces/$FONT_DIR").apply { mkdirs() }
        root.listFiles()?.forEach { existing ->
            if (existing.isFile) existing.delete()
        }

        val extension = resolveExtension(context, uri, "ttf")
            .lowercase()
            .let { if (it in setOf("ttf", "otf", "ttc")) it else "ttf" }
        val target = File(root, "current_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        Typeface.createFromFile(target)
        target.absolutePath
    }.getOrNull()

    fun clearPhoto(context: Context) = clearMedia(context, PHOTO_DIR)

    fun clearVideo(context: Context) = clearMedia(context, VIDEO_DIR)

    fun clearFont(context: Context) = clearMedia(context, FONT_DIR)

    private fun copyMedia(
        context: Context,
        uri: Uri,
        folderName: String,
        fallbackExtension: String
    ): String? {
        return runCatching {
            val root = File(context.filesDir, "internal_watchfaces/$folderName").apply { mkdirs() }
            root.listFiles()?.forEach { existing ->
                if (existing.isFile) existing.delete()
            }

            val extension = resolveExtension(context, uri, fallbackExtension)
            val target = File(root, "current_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            target.absolutePath
        }.getOrNull()
    }

    private fun resolveExtension(context: Context, uri: Uri, fallbackExtension: String): String {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (!fromMime.isNullOrBlank()) return fromMime
        val fromPath = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return fromPath.ifBlank { fallbackExtension }
    }

    private fun clearMedia(context: Context, folderName: String) {
        runCatching {
            val root = File(context.filesDir, "internal_watchfaces/$folderName")
            root.listFiles()?.forEach { existing ->
                if (existing.isFile) existing.delete()
            }
        }
    }

    private fun decodeCroppedPhoto(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val decodedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodePhotoWithImageDecoder(context, uri, targetWidth, targetHeight)
        } else {
            decodeSampledPhoto(context, uri, targetWidth, targetHeight)
        } ?: return null
        return cropAndScaleBitmap(decodedBitmap, targetWidth, targetHeight)
    }

    private fun decodePhotoWithImageDecoder(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sourceWidth = info.size.width.coerceAtLeast(1)
                val sourceHeight = info.size.height.coerceAtLeast(1)
                val sampleSize = minOf(
                    (sourceWidth / targetWidth).coerceAtLeast(1),
                    (sourceHeight / targetHeight).coerceAtLeast(1)
                )
                decoder.setTargetSampleSize(sampleSize)
            }
        }.getOrNull()
    }

    private fun decodeSampledPhoto(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize >= targetWidth * 2 && bounds.outHeight / sampleSize >= targetHeight * 2) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun cropAndScaleBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val sourceAspect = source.width.toFloat() / source.height.toFloat()
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        val cropLeft: Int
        val cropTop: Int

        if (sourceAspect > targetAspect) {
            cropHeight = source.height
            cropWidth = (cropHeight * targetAspect).roundToInt().coerceIn(1, source.width)
            cropLeft = ((source.width - cropWidth) / 2).coerceAtLeast(0)
            cropTop = 0
        } else {
            cropWidth = source.width
            cropHeight = (cropWidth / targetAspect).roundToInt().coerceIn(1, source.height)
            cropLeft = 0
            cropTop = ((source.height - cropHeight) / 2).coerceAtLeast(0)
        }

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(
            source,
            Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight),
            Rect(0, 0, targetWidth, targetHeight),
            paint
        )
        if (output !== source && !source.isRecycled) {
            source.recycle()
        }
        return output
    }
}

object WatchFacePhotoCache {
    private const val MAX_ENTRIES = 3
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun get(path: String): ImageBitmap? = cache[path]

    @Synchronized
    fun put(path: String, bitmap: ImageBitmap) {
        cache[path] = bitmap
    }

    @Synchronized
    fun remove(path: String) {
        cache.remove(path)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
