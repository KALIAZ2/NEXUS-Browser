package com.nexus.browser.browser.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.nexus.browser.core.database.DownloadDao
import com.nexus.browser.core.database.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NexusDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    val downloadsFlow: Flow<List<DownloadEntity>> = downloadDao.observeDownloads()
    val apkDownloadsFlow: Flow<List<DownloadEntity>> = downloadDao.observeApkDownloads()

    fun getDownloadsDirectory(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Prend en charge une réponse externe issue de GeckoView (ContentDelegate.onExternalResponse).
     */
    fun handleGeckoExternalResponse(response: WebResponse) {
        val url = response.uri ?: return
        val headers = response.headers
        val contentDisposition = headers["Content-Disposition"] ?: headers["content-disposition"]
        val contentType = headers["Content-Type"] ?: headers["content-type"] ?: "application/octet-stream"
        val contentLength = (headers["Content-Length"] ?: headers["content-length"])?.toLongOrNull() ?: -1L
        val fileName = resolveFileName(url, contentDisposition, contentType)

        // Si le flux de réponse GeckoView est déjà ouvert, on le consomme directement, sinon on lance un téléchargement HTTP géré
        val bodyStream = response.body
        if (bodyStream != null) {
            startStreamDownload(
                url = url,
                fileName = fileName,
                mimeType = contentType.substringBefore(";").trim(),
                totalBytes = contentLength,
                inputStream = bodyStream
            )
        } else {
            enqueueDownload(url = url, suggestedFileName = fileName, mimeType = contentType.substringBefore(";").trim())
        }
    }

    private fun startStreamDownload(
        url: String,
        fileName: String,
        mimeType: String,
        totalBytes: Long,
        inputStream: InputStream
    ) {
        val downloadId = UUID.randomUUID().toString()
        val destFile = createUniqueFile(getDownloadsDirectory(), fileName)
        val isApk = destFile.name.endsWith(".apk", ignoreCase = true) ||
            mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)

        val job = scope.launch {
            var entity = DownloadEntity(
                id = downloadId,
                fileName = destFile.name,
                sourceUrl = url,
                mimeType = if (isApk) "application/vnd.android.package-archive" else mimeType,
                filePath = destFile.absolutePath,
                totalBytes = totalBytes,
                downloadedBytes = 0L,
                status = DownloadEntity.STATUS_RUNNING,
                isApk = isApk
            )
            downloadDao.upsert(entity)

            try {
                inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead = 0
                        var totalRead = 0L
                        var lastTickTime = System.currentTimeMillis()
                        var bytesAtLastTick = 0L

                        while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val now = System.currentTimeMillis()
                            if (now - lastTickTime >= 500) {
                                val elapsedSec = ((now - lastTickTime) / 1000.0).coerceAtLeast(0.1)
                                val speed = ((totalRead - bytesAtLastTick) / elapsedSec).toLong()
                                entity = entity.copy(
                                    downloadedBytes = totalRead,
                                    speedBytesPerSec = speed
                                )
                                downloadDao.upsert(entity)
                                lastTickTime = now
                                bytesAtLastTick = totalRead
                            }
                        }

                        if (isActive) {
                            output.flush()
                            downloadDao.upsert(
                                entity.copy(
                                    downloadedBytes = totalRead,
                                    totalBytes = if (totalBytes > 0) totalBytes else totalRead,
                                    status = DownloadEntity.STATUS_COMPLETED,
                                    speedBytesPerSec = 0L
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                downloadDao.upsert(
                    entity.copy(
                        status = DownloadEntity.STATUS_FAILED,
                        speedBytesPerSec = 0L,
                        errorMessage = e.localizedMessage ?: "Erreur réseau lors du téléchargement"
                    )
                )
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    /**
     * Lance ou reprend un téléchargement HTTP/HTTPS avec prise en charge de l'en-tête Range (Pause/Reprise).
     */
    fun enqueueDownload(
        url: String,
        suggestedFileName: String? = null,
        mimeType: String? = null,
        existingId: String? = null
    ): String {
        val downloadId = existingId ?: UUID.randomUUID().toString()
        val job = scope.launch {
            val existing = existingId?.let { downloadDao.findById(it) }
            val resolvedName = existing?.fileName
                ?: suggestedFileName
                ?: resolveFileName(url, null, mimeType)
            val destFile = if (existing != null) {
                File(existing.filePath)
            } else {
                createUniqueFile(getDownloadsDirectory(), resolvedName)
            }

            val existingBytes = if (destFile.exists() && existing != null) destFile.length() else 0L
            val isApk = destFile.name.endsWith(".apk", ignoreCase = true) ||
                mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)

            var entity = existing?.copy(
                status = DownloadEntity.STATUS_RUNNING,
                errorMessage = null
            ) ?: DownloadEntity(
                id = downloadId,
                fileName = destFile.name,
                sourceUrl = url,
                mimeType = if (isApk) "application/vnd.android.package-archive" else (mimeType ?: "application/octet-stream"),
                filePath = destFile.absolutePath,
                downloadedBytes = existingBytes,
                status = DownloadEntity.STATUS_RUNNING,
                isApk = isApk
            )
            downloadDao.upsert(entity)

            try {
                val requestBuilder = Request.Builder().url(url)
                if (existingBytes > 0) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw IllegalStateException("Code HTTP ${response.code}")
                    }

                    val isPartial = response.code == 206
                    val body = response.body ?: throw IllegalStateException("Réponse vide du serveur")
                    val contentLength = body.contentLength()
                    val totalBytes = if (contentLength > 0) {
                        if (isPartial) existingBytes + contentLength else contentLength
                    } else {
                        existing?.totalBytes ?: -1L
                    }

                    var totalRead = if (isPartial) existingBytes else 0L
                    entity = entity.copy(totalBytes = totalBytes, downloadedBytes = totalRead)
                    downloadDao.upsert(entity)

                    body.byteStream().use { input ->
                        FileOutputStream(destFile, isPartial).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var bytesRead = 0
                            var lastTickTime = System.currentTimeMillis()
                            var bytesAtLastTick = totalRead

                            while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                val now = System.currentTimeMillis()
                                if (now - lastTickTime >= 500) {
                                    val elapsedSec = ((now - lastTickTime) / 1000.0).coerceAtLeast(0.1)
                                    val speed = ((totalRead - bytesAtLastTick) / elapsedSec).toLong()
                                    entity = entity.copy(
                                        downloadedBytes = totalRead,
                                        speedBytesPerSec = speed
                                    )
                                    downloadDao.upsert(entity)
                                    lastTickTime = now
                                    bytesAtLastTick = totalRead
                                }
                            }

                            if (isActive) {
                                output.flush()
                                downloadDao.upsert(
                                    entity.copy(
                                        downloadedBytes = totalRead,
                                        totalBytes = if (totalBytes > 0) totalBytes else totalRead,
                                        status = DownloadEntity.STATUS_COMPLETED,
                                        speedBytesPerSec = 0L
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    downloadDao.upsert(
                        entity.copy(
                            status = DownloadEntity.STATUS_FAILED,
                            speedBytesPerSec = 0L,
                            errorMessage = e.localizedMessage ?: "Échec du téléchargement"
                        )
                    )
                }
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
        return downloadId
    }

    fun pauseOrResume(download: DownloadEntity) {
        scope.launch {
            if (download.status == DownloadEntity.STATUS_RUNNING) {
                activeJobs.remove(download.id)?.cancel()
                downloadDao.upsert(
                    download.copy(
                        status = DownloadEntity.STATUS_PAUSED,
                        speedBytesPerSec = 0L
                    )
                )
            } else if (download.status == DownloadEntity.STATUS_PAUSED || download.status == DownloadEntity.STATUS_FAILED) {
                enqueueDownload(
                    url = download.sourceUrl,
                    suggestedFileName = download.fileName,
                    mimeType = download.mimeType,
                    existingId = download.id
                )
            }
        }
    }

    fun cancelAndDelete(download: DownloadEntity, deleteFileFromDisk: Boolean = true) {
        scope.launch {
            activeJobs.remove(download.id)?.cancel()
            if (deleteFileFromDisk) {
                runCatching { File(download.filePath).delete() }
            }
            downloadDao.deleteById(download.id)
        }
    }

    fun openDownloadedFile(download: DownloadEntity): Boolean {
        return try {
            val file = File(download.filePath)
            if (!file.exists()) return false
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, download.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareDownloadedFile(download: DownloadEntity): Boolean {
        return try {
            val file = File(download.filePath)
            if (!file.exists()) return false
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = download.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Partager ${download.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openDownloadsFolder(): Boolean {
        return try {
            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        if (!contentDisposition.isNullOrBlank()) {
            val match = Regex("filename\\*?=['\"]?(?:UTF-8'')?([^'\";]+)['\"]?", RegexOption.IGNORE_CASE)
                .find(contentDisposition)
            val extracted = match?.groupValues?.getOrNull(1)?.trim()
            if (!extracted.isNullOrBlank()) {
                return sanitizeFileName(URLDecoder.decode(extracted, "UTF-8"))
            }
        }
        val pathPart = url.substringBefore("?").substringBefore("#").substringAfterLast("/")
        val decoded = runCatching { URLDecoder.decode(pathPart, "UTF-8") }.getOrDefault(pathPart)
        val base = sanitizeFileName(decoded).ifBlank { "telechargement_${System.currentTimeMillis()}" }
        if (!base.contains(".") && !mimeType.isNullOrBlank()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.substringBefore(";").trim())
            if (!ext.isNullOrBlank()) {
                return "$base.$ext"
            }
        }
        return base
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
    }

    private fun createUniqueFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val baseName = desiredName.substringBeforeLast(".", desiredName)
        val ext = desiredName.substringAfterLast(".", "")
        var index = 1
        while (candidate.exists()) {
            val newName = if (ext.isNotEmpty()) "${baseName}_($index).$ext" else "${baseName}_($index)"
            candidate = File(dir, newName)
            index++
        }
        return candidate
    }
}
