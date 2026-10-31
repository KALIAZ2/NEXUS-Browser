package com.nexus.browser.updates

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Immutable
import com.nexus.browser.BuildConfig
import com.nexus.browser.apk.installer.ApkInstaller
import com.nexus.browser.apk.scanner.ApkScanner
import com.nexus.browser.apk.scanner.ApkVersionComparison
import com.nexus.browser.core.preferences.NexusPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR
}

@Immutable
data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: List<String>,
    val rawBody: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
)

@Immutable
data class AppUpdateUiState(
    val installedVersion: String = normalizeVersionString(BuildConfig.VERSION_NAME),
    val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    val latestVersion: String = normalizeVersionString(BuildConfig.VERSION_NAME),
    val lastCheckedAtMillis: Long = 0L,
    val status: AppUpdateStatus = AppUpdateStatus.UP_TO_DATE,
    val statusMessageFr: String = "NEXUS est à jour.",
    val releaseNotes: List<String> = emptyList(),
    val apkDownloadUrl: String = "",
    val apkFileName: String = "",
    val expectedSizeBytes: Long = 0L,
    val downloadProgressPercent: Int = 0,
    val downloadedApkPath: String? = null,
    val errorMessage: String? = null,
    val githubOwner: String = BuildConfig.GITHUB_OWNER,
    val githubRepo: String = BuildConfig.GITHUB_REPO
) {
    val isUpdateAvailable: Boolean
        get() = status == AppUpdateStatus.UPDATE_AVAILABLE ||
            status == AppUpdateStatus.DOWNLOADING ||
            status == AppUpdateStatus.READY_TO_INSTALL

    val formattedLastCheckDate: String
        get() = if (lastCheckedAtMillis <= 0L) {
            "Jamais vérifiée"
        } else {
            SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date(lastCheckedAtMillis))
        }

    companion object {
        fun normalizeVersionString(raw: String): String {
            return raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .removeSuffix("-debug")
                .trim()
        }
    }
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: NexusPreferencesRepository,
    private val apkScanner: ApkScanner,
    private val apkInstaller: ApkInstaller
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            val prefs = preferencesRepository.settingsFlow.first()
            val installedNorm = AppUpdateUiState.normalizeVersionString(BuildConfig.VERSION_NAME)
            val cachedNorm = AppUpdateUiState.normalizeVersionString(prefs.cachedLatestVersionName)
            val hasNewerCached = isRemoteVersionNewer(cachedNorm, installedNorm) &&
                prefs.cachedApkDownloadUrl.isNotBlank()

            _uiState.update {
                it.copy(
                    installedVersion = installedNorm,
                    installedVersionCode = BuildConfig.VERSION_CODE,
                    latestVersion = if (hasNewerCached) cachedNorm else installedNorm,
                    lastCheckedAtMillis = prefs.lastUpdateCheckEpochMs,
                    status = if (hasNewerCached) AppUpdateStatus.UPDATE_AVAILABLE else AppUpdateStatus.UP_TO_DATE,
                    statusMessageFr = if (hasNewerCached) "Une mise à jour est disponible." else "NEXUS est à jour.",
                    releaseNotes = parseReleaseNotesList(prefs.cachedReleaseNotes),
                    apkDownloadUrl = if (hasNewerCached) prefs.cachedApkDownloadUrl else "",
                    githubOwner = prefs.githubOwner,
                    githubRepo = prefs.githubRepo
                )
            }
        }
    }

    /**
     * Vérifie périodiquement au démarrage sans surconsommation (au maximum une fois toutes les 6 heures).
     */
    fun checkForUpdatesOnStartupIfDue(onUpdateDetectedBanner: (String) -> Unit) {
        scope.launch {
            val prefs = preferencesRepository.settingsFlow.first()
            if (!prefs.autoCheckAppUpdates) return@launch
            val now = System.currentTimeMillis()
            val sixHoursMs = 6L * 60L * 60L * 1000L
            if (now - prefs.lastUpdateCheckEpochMs < sixHoursMs) {
                return@launch
            }
            checkForUpdates(manual = false, onBanner = onUpdateDetectedBanner)
        }
    }

    /**
     * Interroge l'API officielle GitHub Releases du dépôt NEXUS Browser.
     */
    fun checkForUpdates(
        manual: Boolean = true,
        onBanner: ((String) -> Unit)? = null
    ) {
        scope.launch {
            val prefs = preferencesRepository.settingsFlow.first()
            val owner = prefs.githubOwner.ifBlank { BuildConfig.GITHUB_OWNER }
            val repo = prefs.githubRepo.ifBlank { BuildConfig.GITHUB_REPO }
            val installedNorm = AppUpdateUiState.normalizeVersionString(BuildConfig.VERSION_NAME)

            _uiState.update {
                it.copy(
                    status = AppUpdateStatus.CHECKING,
                    statusMessageFr = "Recherche de mises à jour sur GitHub...",
                    errorMessage = null,
                    githubOwner = owner,
                    githubRepo = repo
                )
            }

            val result = fetchLatestGitHubRelease(owner, repo)
            val checkedAt = System.currentTimeMillis()

            result.fold(
                onSuccess = { release ->
                    if (release == null) {
                        preferencesRepository.saveUpdateCheckResult(
                            checkedAtMs = checkedAt,
                            latestVersionName = installedNorm,
                            releaseNotes = "",
                            apkDownloadUrl = ""
                        )
                        _uiState.update {
                            it.copy(
                                latestVersion = installedNorm,
                                lastCheckedAtMillis = checkedAt,
                                status = AppUpdateStatus.UP_TO_DATE,
                                statusMessageFr = "NEXUS est à jour.",
                                releaseNotes = emptyList(),
                                apkDownloadUrl = "",
                                errorMessage = null
                            )
                        }
                        if (manual) {
                            onBanner?.invoke("NEXUS est à jour (v$installedNorm).")
                        }
                        return@fold
                    }

                    val remoteNorm = AppUpdateUiState.normalizeVersionString(release.versionName)
                    val updateAvailable = isRemoteVersionNewer(remoteNorm, installedNorm) &&
                        release.apkDownloadUrl.isNotBlank()

                    preferencesRepository.saveUpdateCheckResult(
                        checkedAtMs = checkedAt,
                        latestVersionName = if (updateAvailable) remoteNorm else installedNorm,
                        releaseNotes = release.rawBody,
                        apkDownloadUrl = release.apkDownloadUrl
                    )

                    _uiState.update {
                        it.copy(
                            installedVersion = installedNorm,
                            latestVersion = if (updateAvailable) remoteNorm else installedNorm,
                            lastCheckedAtMillis = checkedAt,
                            status = if (updateAvailable) AppUpdateStatus.UPDATE_AVAILABLE else AppUpdateStatus.UP_TO_DATE,
                            statusMessageFr = if (updateAvailable) {
                                "Une mise à jour est disponible."
                            } else {
                                "NEXUS est à jour."
                            },
                            releaseNotes = release.releaseNotes,
                            apkDownloadUrl = release.apkDownloadUrl,
                            apkFileName = release.apkFileName.ifBlank { "NEXUS-v$remoteNorm.apk" },
                            expectedSizeBytes = release.apkSizeBytes,
                            errorMessage = null
                        )
                    }

                    if (updateAvailable) {
                        onBanner?.invoke("Nouvelle version NEXUS v$remoteNorm disponible dans Paramètres → À propos.")
                    } else if (manual) {
                        onBanner?.invoke("NEXUS est à jour (v$installedNorm).")
                    }
                },
                onFailure = { error ->
                    val reason = error.localizedMessage ?: "Impossible de joindre GitHub Releases."
                    _uiState.update {
                        it.copy(
                            lastCheckedAtMillis = checkedAt,
                            status = AppUpdateStatus.ERROR,
                            statusMessageFr = "Erreur lors de la vérification : $reason",
                            errorMessage = reason
                        )
                    }
                    if (manual) {
                        onBanner?.invoke("Vérification impossible : $reason")
                    }
                }
            )
        }
    }

    /**
     * Télécharge l'APK de la GitHub Release, vérifie son intégrité ZIP/APK, son package et sa signature,
     * puis ouvre l'installateur système Android officiel.
     */
    fun downloadAndInstallUpdate(onBanner: (String) -> Unit = {}) {
        val currentState = _uiState.value
        val downloadUrl = currentState.apkDownloadUrl
        if (downloadUrl.isBlank()) {
            onBanner("Aucun fichier APK disponible pour cette version.")
            return
        }

        // Si l'APK a déjà été téléchargé et vérifié, relancer directement l'installateur Android
        val existingPath = currentState.downloadedApkPath
        if (currentState.status == AppUpdateStatus.READY_TO_INSTALL && !existingPath.isNullOrBlank()) {
            val existingFile = File(existingPath)
            if (existingFile.exists() && existingFile.length() > 0L) {
                launchAndroidInstaller(existingFile, onBanner)
                return
            }
        }

        scope.launch {
            val targetVersion = currentState.latestVersion
            val fileName = currentState.apkFileName.ifBlank { "NEXUS-v$targetVersion.apk" }
            val updatesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                "updates"
            ).apply { mkdirs() }
            val tempFile = File(updatesDir, "$fileName.part")
            val finalFile = File(updatesDir, fileName)

            _uiState.update {
                it.copy(
                    status = AppUpdateStatus.DOWNLOADING,
                    statusMessageFr = "Téléchargement de NEXUS v$targetVersion en cours...",
                    downloadProgressPercent = 0,
                    errorMessage = null
                )
            }

            val downloadResult = runCatching {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("Accept", "application/octet-stream")
                    .header("User-Agent", "NEXUS-Browser-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Échec du téléchargement HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Réponse vide du serveur GitHub")
                    val totalBytes = body.contentLength().takeIf { it > 0L } ?: currentState.expectedSizeBytes

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var downloaded = 0L
                            var lastProgress = -1
                            var read = input.read(buffer)
                            while (read != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0L) {
                                    val progress = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        _uiState.update { state ->
                                            state.copy(
                                                downloadProgressPercent = progress,
                                                statusMessageFr = "Téléchargement de NEXUS v$targetVersion ($progress%)..."
                                            )
                                        }
                                    }
                                }
                                read = input.read(buffer)
                            }
                            output.flush()
                        }
                    }
                }

                // 1. Vérification de la taille attendue
                val expectedSize = currentState.expectedSizeBytes
                if (expectedSize > 0L && tempFile.length() != expectedSize) {
                    tempFile.delete()
                    throw IllegalStateException(
                        "Taille du fichier APK incorrecte (${tempFile.length()} octets reçus sur $expectedSize attendus)."
                    )
                }

                // 2. Vérification de l'intégrité ZIP/APK et du manifest Android
                val (zipValid, zipError) = apkScanner.verifyZipStructureIntegrity(tempFile)
                if (!zipValid) {
                    tempFile.delete()
                    throw IllegalStateException(zipError ?: "Le fichier téléchargé n'est pas un APK valide.")
                }

                if (finalFile.exists()) {
                    finalFile.delete()
                }
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                // 3. Vérification de l'applicationId et de la cohérence de version via PackageManager
                val metadata = apkScanner.inspectApkFile(finalFile, downloadUrl)
                if (metadata.isCorrupted) {
                    finalFile.delete()
                    throw IllegalStateException(metadata.corruptionReason ?: "APK corrompu.")
                }
                if (metadata.packageName.isNotBlank() && metadata.packageName != BuildConfig.APPLICATION_ID) {
                    finalFile.delete()
                    throw IllegalStateException(
                        "Package incompatible : ${metadata.packageName} (attendu : ${BuildConfig.APPLICATION_ID})."
                    )
                }
                if (metadata.versionComparison == ApkVersionComparison.DOWNGRADE_WARNING) {
                    finalFile.delete()
                    throw IllegalStateException(
                        "La version téléchargée (build ${metadata.versionCode}) est antérieure à la version installée."
                    )
                }

                finalFile
            }

            downloadResult.fold(
                onSuccess = { verifiedFile ->
                    _uiState.update {
                        it.copy(
                            status = AppUpdateStatus.READY_TO_INSTALL,
                            statusMessageFr = "Téléchargement vérifié. Ouverture de l'installateur Android...",
                            downloadProgressPercent = 100,
                            downloadedApkPath = verifiedFile.absolutePath,
                            errorMessage = null
                        )
                    }
                    withContext(Dispatchers.Main) {
                        launchAndroidInstaller(verifiedFile, onBanner)
                    }
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Erreur lors du téléchargement de la mise à jour."
                    _uiState.update {
                        it.copy(
                            status = AppUpdateStatus.UPDATE_AVAILABLE,
                            statusMessageFr = "Une mise à jour est disponible.",
                            downloadProgressPercent = 0,
                            errorMessage = msg
                        )
                    }
                    withContext(Dispatchers.Main) {
                        onBanner("Échec du téléchargement : $msg")
                    }
                }
            )
        }
    }

    private fun launchAndroidInstaller(apkFile: File, onBanner: (String) -> Unit) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            onBanner("Autorisez NEXUS à installer des applications mises à jour, puis appuyez à nouveau sur Installer.")
            apkInstaller.openUnknownSourcesSettings()
            return
        }
        apkInstaller.launchSystemPackageInstaller(apkFile).onFailure { err ->
            onBanner("Impossible d'ouvrir l'installateur Android : ${err.localizedMessage}")
        }
    }

    private fun fetchLatestGitHubRelease(owner: String, repo: String): Result<GitHubReleaseInfo?> {
        return runCatching {
            val latestUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(latestUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "NEXUS-Browser-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val bodyStr = response.body?.string().orEmpty()
                        parseReleaseJson(JSONObject(bodyStr))
                    }
                    404 -> {
                        // Soit le dépôt n'a pas encore de Release publiée, soit seulement une pré-release
                        fetchFirstReleaseFromList(owner, repo)
                    }
                    else -> {
                        throw IllegalStateException("GitHub API HTTP ${response.code}")
                    }
                }
            }
        }
    }

    private fun fetchFirstReleaseFromList(owner: String, repo: String): GitHubReleaseInfo? {
        val listUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=5"
        val request = Request.Builder()
            .url(listUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NEXUS-Browser-Android/${BuildConfig.VERSION_NAME}")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API HTTP ${response.code}")
            }
            val array = JSONArray(response.body?.string().orEmpty())
            if (array.length() == 0) return null
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (!obj.optBoolean("draft", false)) {
                    return parseReleaseJson(obj)
                }
            }
            return null
        }
    }

    companion object {
        fun parseReleaseJson(json: JSONObject): GitHubReleaseInfo {
            val tagName = json.optString("tag_name", "").trim()
            val name = json.optString("name", tagName).trim()
            val rawBody = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", "")

            val versionFromTag = AppUpdateUiState.normalizeVersionString(
                tagName.ifBlank { name }
            )

            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L

            val assets = json.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    apkName = assetName
                    apkSize = asset.optLong("size", 0L)
                    // Privilégier l'APK versionné NEXUS-vX.Y.Z.apk s'il est présent
                    if (assetName.startsWith("NEXUS-v", ignoreCase = true)) {
                        break
                    }
                }
            }

            return GitHubReleaseInfo(
                tagName = tagName,
                versionName = versionFromTag,
                releaseTitle = name.ifBlank { "NEXUS Browser v$versionFromTag" },
                releaseNotes = parseReleaseNotesList(rawBody),
                rawBody = rawBody,
                publishedAt = publishedAt,
                htmlUrl = htmlUrl,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize
            )
        }

        fun parseReleaseNotesList(rawBody: String): List<String> {
            if (rawBody.isBlank()) {
                return listOf(
                    "Amélioration de la navigation et de la stabilité.",
                    "Corrections de bugs et optimisation des performances."
                )
            }
            val lines = rawBody.lines()
                .map { it.trim() }
                .filter { line ->
                    line.isNotBlank() &&
                        !line.startsWith("#") &&
                        !line.startsWith("---") &&
                        !line.startsWith("SHA-256", ignoreCase = true)
                }
                .map { line ->
                    line.removePrefix("- ")
                        .removePrefix("* ")
                        .removePrefix("• ")
                        .trim()
                }
                .filter { it.isNotBlank() }

            return if (lines.isEmpty()) {
                listOf(rawBody.trim())
            } else {
                lines.take(8)
            }
        }

        /**
         * Compare deux versions sémantiques (ex: "1.3.0" > "1.2.0", "1.2.1" > "1.2.0").
         */
        fun isRemoteVersionNewer(remoteVersion: String, installedVersion: String): Boolean {
            return compareSemanticVersions(remoteVersion, installedVersion) > 0
        }

        fun compareSemanticVersions(v1: String, v2: String): Int {
            val clean1 = AppUpdateUiState.normalizeVersionString(v1)
                .substringBefore("-")
                .substringBefore("+")
            val clean2 = AppUpdateUiState.normalizeVersionString(v2)
                .substringBefore("-")
                .substringBefore("+")

            val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }
            return 0
        }
    }
}
