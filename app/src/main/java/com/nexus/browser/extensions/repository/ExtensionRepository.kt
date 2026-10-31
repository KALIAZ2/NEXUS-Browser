package com.nexus.browser.extensions.repository

import android.net.Uri
import com.nexus.browser.core.database.ExtensionMetadataEntity
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityReport
import com.nexus.browser.extensions.manager.NexusExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepository @Inject constructor(
    private val extensionManager: NexusExtensionManager
) {
    val installedExtensionsFlow: Flow<List<ExtensionMetadataEntity>> = extensionManager.installedExtensionsFlow
    val lastCompatibilityReport: StateFlow<ExtensionCompatibilityReport?> = extensionManager.lastCompatibilityReport
    val statusMessage: StateFlow<String?> = extensionManager.statusMessage

    suspend fun importAndInstallUri(uri: Uri): ExtensionCompatibilityReport {
        return extensionManager.importAndInstallLocalPackage(uri)
    }

    suspend fun installLocalXpiFile(file: File, sourceType: String = "LOCAL_XPI"): ExtensionCompatibilityReport {
        return extensionManager.installFromLocalFile(file, sourceType)
    }

    suspend fun setEnabled(extensionId: String, enabled: Boolean) {
        extensionManager.setExtensionEnabled(extensionId, enabled)
    }

    suspend fun uninstall(extensionId: String) {
        extensionManager.uninstallExtension(extensionId)
    }

    suspend fun checkForUpdates(extensionId: String): String {
        return extensionManager.checkForUpdates(extensionId)
    }

    fun clearStatusMessage() {
        extensionManager.clearStatusMessage()
    }
}
