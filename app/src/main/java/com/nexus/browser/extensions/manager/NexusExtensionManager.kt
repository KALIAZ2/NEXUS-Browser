package com.nexus.browser.extensions.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nexus.browser.browser.engine.BrowserEngineManager
import com.nexus.browser.core.database.ExtensionMetadataDao
import com.nexus.browser.core.database.ExtensionMetadataEntity
import com.nexus.browser.extensions.compatibility.CompatibilityLevel
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityAnalyzer
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityReport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class NexusExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engineManager: BrowserEngineManager,
    private val extensionMetadataDao: ExtensionMetadataDao,
    private val compatibilityAnalyzer: ExtensionCompatibilityAnalyzer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runtimeExtensionsMap = ConcurrentHashMap<String, WebExtension>()

    val installedExtensionsFlow: Flow<List<ExtensionMetadataEntity>> = extensionMetadataDao.observeAll()

    private val _lastCompatibilityReport = MutableStateFlow<ExtensionCompatibilityReport?>(null)
    val lastCompatibilityReport: StateFlow<ExtensionCompatibilityReport?> = _lastCompatibilityReport.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    data class BuiltInSpec(
        val id: String,
        val assetUri: String,
        val name: String,
        val version: String,
        val description: String,
        val author: String,
        val permissionsCsv: String,
        val defaultEnabled: Boolean
    )

    private val builtInExtensions = listOf(
        BuiltInSpec(
            id = "privacy-shield@nexus.browser",
            assetUri = "resource://android/assets/extensions/nexus_privacy_shield/",
            name = "NEXUS Privacy Shield & Anti-Bannières",
            version = "1.1.0",
            description = "Supprime les paramètres de pistage dans les liens (utm, fbclid, gclid) et masque les bannières intrusives.",
            author = "NEXUS Security",
            permissionsCsv = "activeTab,<all_urls>",
            defaultEnabled = true
        ),
        BuiltInSpec(
            id = "dark-reader-lite@nexus.browser",
            assetUri = "resource://android/assets/extensions/nexus_dark_mode/",
            name = "NEXUS Dark Mode Pro",
            version = "1.2.0",
            description = "Applique automatiquement un thème sombre adaptatif et reposant aux pages web claires.",
            author = "NEXUS Team",
            permissionsCsv = "activeTab,<all_urls>",
            defaultEnabled = false
        ),
        BuiltInSpec(
            id = "reading-enhancer@nexus.browser",
            assetUri = "resource://android/assets/extensions/nexus_reading_mode/",
            name = "NEXUS Mobile Typography & Confort",
            version = "1.0.4",
            description = "Améliore la lisibilité mobile, le défilement fluide et l'affichage des blocs de code.",
            author = "NEXUS Design",
            permissionsCsv = "activeTab",
            defaultEnabled = true
        )
    )

    init {
        scope.launch {
            initializeExtensionsSubsystem()
        }
    }

    private suspend fun initializeExtensionsSubsystem() {
        runCatching {
            val controller = engineManager.getWebExtensionController()
            controller.promptDelegate = object : WebExtensionController.PromptDelegate {
                override fun onInstallPrompt(extension: WebExtension): GeckoResult<AllowOrDeny>? {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }

                override fun onInstallPrompt(
                    extension: WebExtension,
                    permissions: Array<out String>,
                    origins: Array<out String>
                ): GeckoResult<AllowOrDeny>? {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }
            }

            // Synchronize built-in WebExtensions
            for (spec in builtInExtensions) {
                val existingDb = withContext(Dispatchers.IO) {
                    extensionMetadataDao.findById(spec.id)
                }
                val ext = awaitGeckoResult(controller.ensureBuiltIn(spec.assetUri, spec.id))
                if (ext != null) {
                    runtimeExtensionsMap[spec.id] = ext
                    val shouldEnable = existingDb?.enabled ?: spec.defaultEnabled
                    if (shouldEnable) {
                        awaitGeckoResult(controller.enable(ext, WebExtensionController.EnableSource.USER))
                    } else {
                        awaitGeckoResult(controller.disable(ext, WebExtensionController.EnableSource.USER))
                    }
                }
                if (existingDb == null) {
                    withContext(Dispatchers.IO) {
                        extensionMetadataDao.upsert(
                            ExtensionMetadataEntity(
                                extensionId = spec.id,
                                name = spec.name,
                                version = spec.version,
                                description = spec.description,
                                author = spec.author,
                                enabled = spec.defaultEnabled,
                                sourceType = "BUILT_IN",
                                localPathOrUri = spec.assetUri,
                                manifestVersion = 2,
                                permissionsCsv = spec.permissionsCsv,
                                compatibilityLevel = CompatibilityLevel.COMPATIBLE.name
                            )
                        )
                    }
                }
            }

            // Refresh existing runtime extensions list
            val listed = awaitGeckoResult(controller.list()) ?: emptyList()
            listed.forEach { webExt ->
                runtimeExtensionsMap[webExt.id] = webExt
            }
        }.onFailure { e ->
            Log.e("NexusExtensionMgr", "Erreur d'initialisation WebExtensions: ${e.localizedMessage}")
        }
    }

    /**
     * Importe et analyse un fichier local (.xpi, .zip ou .crx) sélectionné par l'utilisateur.
     */
    suspend fun importAndInstallLocalPackage(uri: Uri): ExtensionCompatibilityReport {
        val localDir = File(context.filesDir, "extensions").apply { mkdirs() }
        val tempFile = File(localDir, "import_${System.currentTimeMillis()}.xpi")

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return installFromLocalFile(tempFile, sourceType = "LOCAL_XPI")
    }

    /**
     * Analyse et installe un fichier .xpi local (importé ou généré par NEXUS AI).
     */
    suspend fun installFromLocalFile(
        file: File,
        sourceType: String = "LOCAL_XPI"
    ): ExtensionCompatibilityReport {
        val report = withContext(Dispatchers.IO) {
            compatibilityAnalyzer.analyzePackageFile(file)
        }
        _lastCompatibilityReport.value = report

        if (!report.level.isInstallableInGecko) {
            _statusMessage.value = "Installation refusée : ${report.level.labelFr}"
            return report
        }

        val fileUri = Uri.fromFile(file).toString()
        val controller = engineManager.getWebExtensionController()
        val installedWebExt = awaitGeckoResult(controller.install(fileUri))

        val finalId = installedWebExt?.id ?: report.extensionId
        if (installedWebExt != null) {
            runtimeExtensionsMap[finalId] = installedWebExt
        }

        val allPerms = (report.permissions + report.hostPermissions).distinct().joinToString(",")
        withContext(Dispatchers.IO) {
            extensionMetadataDao.upsert(
                ExtensionMetadataEntity(
                    extensionId = finalId,
                    name = installedWebExt?.metaData?.name?.takeIf { it.isNotBlank() } ?: report.name,
                    version = installedWebExt?.metaData?.version?.takeIf { it.isNotBlank() } ?: report.version,
                    description = installedWebExt?.metaData?.description?.takeIf { it.isNotBlank() } ?: report.description,
                    author = installedWebExt?.metaData?.creatorName?.takeIf { it.isNotBlank() } ?: report.author,
                    enabled = true,
                    sourceType = sourceType,
                    localPathOrUri = file.absolutePath,
                    manifestVersion = report.manifestVersion,
                    permissionsCsv = allPerms,
                    compatibilityLevel = report.level.name
                )
            )
        }

        _statusMessage.value = if (installedWebExt != null) {
            "Extension « ${report.name} » installée et activée dans GeckoView."
        } else {
            "Extension « ${report.name} » vérifiée et enregistrée (signature Mozilla requise pour les XPI tiers en mode standard)."
        }

        return report
    }

    /**
     * Installe une extension officielle depuis une URL HTTPS autorisée (ex: addons.mozilla.org).
     */
    suspend fun installFromRemoteXpiUrl(
        downloadUrl: String,
        fallbackId: String,
        fallbackName: String,
        fallbackVersion: String,
        fallbackDescription: String,
        fallbackAuthor: String,
        fallbackPermissions: List<String>
    ): Boolean {
        _statusMessage.value = "Téléchargement et installation de $fallbackName en cours..."
        val controller = engineManager.getWebExtensionController()
        val installed = awaitGeckoResult(controller.install(downloadUrl))

        val resolvedId = installed?.id ?: fallbackId
        if (installed != null) {
            runtimeExtensionsMap[resolvedId] = installed
        }

        val perms = installed?.metaData?.permissions?.toList() ?: fallbackPermissions
        withContext(Dispatchers.IO) {
            extensionMetadataDao.upsert(
                ExtensionMetadataEntity(
                    extensionId = resolvedId,
                    name = installed?.metaData?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
                    version = installed?.metaData?.version?.takeIf { it.isNotBlank() } ?: fallbackVersion,
                    description = installed?.metaData?.description?.takeIf { it.isNotBlank() } ?: fallbackDescription,
                    author = installed?.metaData?.creatorName?.takeIf { it.isNotBlank() } ?: fallbackAuthor,
                    enabled = true,
                    sourceType = "STORE",
                    localPathOrUri = downloadUrl,
                    manifestVersion = 2,
                    permissionsCsv = perms.joinToString(","),
                    compatibilityLevel = CompatibilityLevel.COMPATIBLE.name
                )
            )
        }

        _statusMessage.value = if (installed != null) {
            "Extension « $fallbackName » installée avec succès dans GeckoView !"
        } else {
            "Extension « $fallbackName » ajoutée au gestionnaire (vérifiez votre connexion réseau pour le paquet AMO)."
        }
        return true
    }

    suspend fun setExtensionEnabled(extensionId: String, enabled: Boolean) {
        val controller = engineManager.getWebExtensionController()
        val runtimeExt = runtimeExtensionsMap[extensionId]
        if (runtimeExt != null) {
            if (enabled) {
                val updated = awaitGeckoResult(controller.enable(runtimeExt, WebExtensionController.EnableSource.USER))
                if (updated != null) runtimeExtensionsMap[extensionId] = updated
            } else {
                val updated = awaitGeckoResult(controller.disable(runtimeExt, WebExtensionController.EnableSource.USER))
                if (updated != null) runtimeExtensionsMap[extensionId] = updated
            }
        }
        withContext(Dispatchers.IO) {
            extensionMetadataDao.updateEnabled(extensionId, enabled)
        }
        _statusMessage.value = if (enabled) "Extension activée." else "Extension désactivée."
    }

    suspend fun uninstallExtension(extensionId: String) {
        val controller = engineManager.getWebExtensionController()
        val runtimeExt = runtimeExtensionsMap.remove(extensionId)
        if (runtimeExt != null) {
            awaitGeckoResult(controller.uninstall(runtimeExt))
        }
        withContext(Dispatchers.IO) {
            val meta = extensionMetadataDao.findById(extensionId)
            if (meta != null && meta.sourceType != "BUILT_IN" && !meta.localPathOrUri.startsWith("http")) {
                runCatching { File(meta.localPathOrUri).delete() }
            }
            extensionMetadataDao.deleteById(extensionId)
        }
        _statusMessage.value = "Extension supprimée."
    }

    suspend fun checkForUpdates(extensionId: String): String {
        val controller = engineManager.getWebExtensionController()
        val runtimeExt = runtimeExtensionsMap[extensionId]
        if (runtimeExt != null && !runtimeExt.isBuiltIn) {
            val updated = awaitGeckoResult(controller.update(runtimeExt))
            if (updated != null) {
                runtimeExtensionsMap[extensionId] = updated
                val newVer = updated.metaData?.version
                if (!newVer.isNullOrBlank()) {
                    withContext(Dispatchers.IO) {
                        val existing = extensionMetadataDao.findById(extensionId)
                        if (existing != null) {
                            extensionMetadataDao.upsert(existing.copy(version = newVer))
                        }
                    }
                    val msg = "Extension mise à jour vers v$newVer."
                    _statusMessage.value = msg
                    return msg
                }
            }
        }
        val msg = "Cette extension est déjà à jour."
        _statusMessage.value = msg
        return msg
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private suspend fun <T> awaitGeckoResult(geckoResult: GeckoResult<T>): T? {
        return suspendCancellableCoroutine { cont ->
            geckoResult.then(
                { value ->
                    if (cont.isActive) cont.resume(value)
                    GeckoResult.fromValue(null)
                },
                { error ->
                    Log.w("NexusExtensionMgr", "GeckoResult exception: ${error.localizedMessage}")
                    if (cont.isActive) cont.resume(null)
                    GeckoResult.fromValue(null)
                }
            )
        }
    }
}
