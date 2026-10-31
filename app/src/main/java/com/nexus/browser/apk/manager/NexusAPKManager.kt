package com.nexus.browser.apk.manager

import com.nexus.browser.apk.installer.ApkInstaller
import com.nexus.browser.apk.scanner.ApkMetadata
import com.nexus.browser.apk.scanner.ApkScanner
import com.nexus.browser.browser.downloads.NexusDownloadManager
import com.nexus.browser.core.database.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NexusAPKManager @Inject constructor(
    private val downloadManager: NexusDownloadManager,
    private val apkScanner: ApkScanner,
    private val apkInstaller: ApkInstaller
) {
    /**
     * Observe automatiquement tous les fichiers APK présents dans le dossier de téléchargements NEXUS
     * ainsi que dans l'historique Room.
     */
    val detectedApksFlow: Flow<List<ApkMetadata>> = downloadManager.downloadsFlow.map { downloads ->
        withContext(Dispatchers.IO) {
            val fromDb = downloads.filter {
                it.isApk || it.fileName.endsWith(".apk", ignoreCase = true)
            }
            val dbFiles = fromDb.mapNotNull { entity ->
                val f = File(entity.filePath)
                if (f.exists() && entity.status == DownloadEntity.STATUS_COMPLETED) {
                    apkScanner.inspectApkFile(f, entity.sourceUrl)
                } else null
            }

            // Détecte aussi les fichiers .apk copiés manuellement dans le répertoire de téléchargements NEXUS
            val dirFiles = downloadManager.getDownloadsDirectory()
                .listFiles { file -> file.isFile && file.name.endsWith(".apk", ignoreCase = true) }
                ?.toList()
                ?: emptyList()

            val extraApks = dirFiles
                .filter { diskFile -> dbFiles.none { it.filePath == diskFile.absolutePath } }
                .map { diskFile -> apkScanner.inspectApkFile(diskFile, "Importé localement") }

            (dbFiles + extraApks).sortedByDescending { File(it.filePath).lastModified() }
        }
    }

    fun inspectSingleApk(file: File, sourceUrl: String = ""): ApkMetadata {
        return apkScanner.inspectApkFile(file, sourceUrl)
    }

    fun canInstallPackages(): Boolean {
        return apkInstaller.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        apkInstaller.openUnknownSourcesSettings()
    }

    fun requestUserConfirmedInstall(metadata: ApkMetadata): Result<Unit> {
        if (metadata.isCorrupted) {
            return Result.failure(IllegalStateException(metadata.corruptionReason ?: "APK endommagé"))
        }
        return apkInstaller.launchSystemPackageInstaller(File(metadata.filePath))
    }

    fun deleteApkFile(metadata: ApkMetadata): Boolean {
        return runCatching {
            File(metadata.filePath).delete()
        }.getOrDefault(false)
    }
}
