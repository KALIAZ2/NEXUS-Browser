package com.nexus.browser.browser.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class PageCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val previewMemoryCache = object : LruCache<String, Bitmap>(18) {}

    suspend fun saveTabPreviewBitmap(tabId: String, source: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (source.width <= 0 || source.height <= 0) return@runCatching null
            val targetWidth = 420
            val scale = targetWidth.toFloat() / source.width.toFloat()
            val targetHeight = (source.height * scale).roundToInt().coerceIn(220, 760)
            val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)

            val previewDir = File(context.cacheDir, "tab_previews").apply { mkdirs() }
            val file = File(previewDir, "tab_${tabId}.webp")
            FileOutputStream(file).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                scaled.compress(format, 82, out)
            }
            val key = "${file.absolutePath}?t=${System.currentTimeMillis()}"
            synchronized(previewMemoryCache) {
                previewMemoryCache.put(tabId, scaled)
                previewMemoryCache.put(key, scaled)
            }
            key
        }.getOrNull()
    }

    fun getCachedTabPreview(tabId: String, previewPath: String?): Bitmap? {
        synchronized(previewMemoryCache) {
            if (!previewPath.isNullOrBlank()) {
                previewMemoryCache.get(previewPath)?.let { if (!it.isRecycled) return it }
            }
            previewMemoryCache.get(tabId)?.let { if (!it.isRecycled) return it }
        }
        val cleanPath = previewPath?.substringBefore("?") ?: return null
        val file = File(cleanPath)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.also { decoded ->
                synchronized(previewMemoryCache) {
                    previewMemoryCache.put(tabId, decoded)
                    previewMemoryCache.put(previewPath, decoded)
                }
            }
        }.getOrNull()
    }

    fun deleteTabPreview(tabId: String) {
        synchronized(previewMemoryCache) {
            previewMemoryCache.remove(tabId)
        }
        runCatching {
            val file = File(File(context.cacheDir, "tab_previews"), "tab_${tabId}.webp")
            if (file.exists()) file.delete()
        }
    }
    suspend fun saveViewportBitmap(bitmap: Bitmap, pageTitle: String): File? = withContext(Dispatchers.IO) {
        saveBitmapToCapturesDir(bitmap, prefix = "NEXUS_Capture", title = pageTitle)
    }

    suspend fun saveFullArticleBitmap(article: ReaderModeArticle): File? = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val padding = 64
            val contentWidth = width - padding * 2

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F5F8FF")
                textSize = 46f
                isFakeBoldText = true
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00E5FF")
                textSize = 26f
            }
            val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2ECFF")
                textSize = 36f
                isFakeBoldText = true
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 30f
            }

            val titleLayout = StaticLayout.Builder
                .obtain(article.title, 0, article.title.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1.15f)
                .build()

            val metaText = "${article.siteName}  •  Capture intégrale NEXUS Browser"
            val metaLayout = StaticLayout.Builder
                .obtain(metaText, 0, metaText.length, metaPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            val blockLayouts = article.blocks.take(45).map { block ->
                val paint = if (block.type == ReaderBlock.BlockType.HEADING) headingPaint else bodyPaint
                val formatted = if (block.type == ReaderBlock.BlockType.LIST_ITEM) "• ${block.text}" else block.text
                StaticLayout.Builder
                    .obtain(formatted, 0, formatted.length, paint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(8f, 1.25f)
                    .build()
            }

            var totalHeight = padding + titleLayout.height + 24 + metaLayout.height + 48
            blockLayouts.forEach { layout ->
                totalHeight += layout.height + 32
            }
            totalHeight = (totalHeight + padding).coerceIn(800, 14000)

            val bitmap = Bitmap.createBitmap(width, max(800, totalHeight), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#050816"))

            var y = padding.toFloat()
            canvas.save()
            canvas.translate(padding.toFloat(), y)
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 24f

            canvas.save()
            canvas.translate(padding.toFloat(), y)
            metaLayout.draw(canvas)
            canvas.restore()
            y += metaLayout.height + 48f

            for (layout in blockLayouts) {
                if (y + layout.height > bitmap.height - padding) break
                canvas.save()
                canvas.translate(padding.toFloat(), y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 32f
            }

            val saved = saveBitmapToCapturesDir(bitmap, prefix = "NEXUS_PageComplete", title = article.title)
            bitmap.recycle()
            saved
        }.getOrNull()
    }

    fun shareCaptureFile(file: File) {
        if (!file.exists()) return
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Partager la capture NEXUS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun saveBitmapToCapturesDir(bitmap: Bitmap, prefix: String, title: String): File? {
        return runCatching {
            val publicPictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(publicPictures, "NEXUS").apply {
                if (!exists() && !mkdirs()) {
                    // Fallback to app external files dir
                    return@runCatching saveToFallbackDir(bitmap, prefix, title)
                }
            }
            val file = File(targetDir, buildFileName(prefix, title))
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            file
        }.getOrElse {
            saveToFallbackDir(bitmap, prefix, title)
        }
    }

    private fun saveToFallbackDir(bitmap: Bitmap, prefix: String, title: String): File? {
        return runCatching {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                "NEXUS_Captures"
            ).apply { mkdirs() }
            val file = File(dir, buildFileName(prefix, title))
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        }.getOrNull()
    }

    private fun buildFileName(prefix: String, title: String): String {
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24).trim('_').ifBlank { "web" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_${cleanTitle}_$stamp.png"
    }
}
