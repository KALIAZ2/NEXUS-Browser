package com.nexus.browser.apk.repository

import com.nexus.browser.apk.manager.NexusAPKManager
import com.nexus.browser.apk.scanner.ApkMetadata
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkRepository @Inject constructor(
    private val nexusApkManager: NexusAPKManager
) {
    val apksFlow: Flow<List<ApkMetadata>> = nexusApkManager.detectedApksFlow

    fun canInstallPackages(): Boolean = nexusApkManager.canInstallPackages()

    fun openInstallPermissionSettings() = nexusApkManager.openInstallPermissionSettings()

    fun installApk(metadata: ApkMetadata): Result<Unit> = nexusApkManager.requestUserConfirmedInstall(metadata)

    fun deleteApk(metadata: ApkMetadata): Boolean = nexusApkManager.deleteApkFile(metadata)
}
