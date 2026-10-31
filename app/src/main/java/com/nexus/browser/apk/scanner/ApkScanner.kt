package com.nexus.browser.apk.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ApkVersionComparison(val labelFr: String) {
    NOT_INSTALLED("Nouvelle installation (non installée sur l'appareil)"),
    UPDATE_AVAILABLE("Mise à jour d'une application existante"),
    SAME_VERSION("Version identique déjà installée"),
    DOWNGRADE_WARNING("Attention : version antérieure à celle installée"),
    UNKNOWN("Statut inconnu")
}

data class ApkMetadata(
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val sourceUrl: String,
    val isCorrupted: Boolean,
    val corruptionReason: String? = null,
    val appName: String = "APK Inconnu",
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val installedVersionName: String? = null,
    val installedVersionCode: Long? = null,
    val versionComparison: ApkVersionComparison = ApkVersionComparison.UNKNOWN,
    val signatureSha256: String = "Non vérifiée",
    val requestedPermissions: List<String> = emptyList(),
    val minSdkVersion: Int = 0,
    val targetSdkVersion: Int = 0,
    val securityNoticeFr: String =
        "Avertissement de sécurité : Le téléchargement d'un fichier APK ne garantit pas son innocuité. Vérifiez toujours l'éditeur, la signature et les permissions avant d'autoriser l'installation."
)

@Singleton
class ApkScanner private constructor(
    private val context: Context?,
    private val unitTestMode: Boolean
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, false)

    constructor() : this(null, true)
    /**
     * Vérifie l'intégrité structurelle d'une archive APK (indépendamment du PackageManager, testable unitairement).
     */
    fun verifyZipStructureIntegrity(file: File): Pair<Boolean, String?> {
        if (!file.exists() || !file.isFile) {
            return false to "Le fichier APK est introuvable sur le stockage."
        }
        if (file.length() < 64L) {
            return false to "Le fichier APK est vide ou tronqué (${file.length()} octets)."
        }

        var hasAndroidManifest = false
        var hasDexOrResources = false
        var entryCount = 0

        return try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    val name = entry.name
                    if (name == "AndroidManifest.xml") {
                        hasAndroidManifest = true
                    }
                    if (name.endsWith(".dex") || name == "resources.arsc") {
                        hasDexOrResources = true
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            when {
                entryCount == 0 -> false to "Le fichier n'est pas une archive ZIP/APK valide."
                !hasAndroidManifest -> false to "Archive endommagée : AndroidManifest.xml absent."
                !hasDexOrResources -> false to "Archive incomplète : aucun binaire classes.dex ou resources.arsc détecté."
                else -> true to null
            }
        } catch (e: Exception) {
            false to "Archive APK corrompue : ${e.localizedMessage ?: "erreur de lecture ZIP"}"
        }
    }

    /**
     * Analyse complète d'un fichier APK via les API officielles Android PackageManager.
     */
    fun inspectApkFile(file: File, sourceUrl: String = ""): ApkMetadata {
        val (isValidZip, zipError) = verifyZipStructureIntegrity(file)
        if (!isValidZip) {
            return ApkMetadata(
                filePath = file.absolutePath,
                fileName = file.name,
                fileSizeBytes = if (file.exists()) file.length() else 0L,
                sourceUrl = sourceUrl,
                isCorrupted = true,
                corruptionReason = zipError
            )
        }

        val pm = context?.packageManager ?: return ApkMetadata(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSizeBytes = file.length(),
            sourceUrl = sourceUrl,
            isCorrupted = false
        )
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES

        val pkgInfo: PackageInfo? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    file.absolutePath,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, flags)
            }
        } catch (_: Exception) {
            null
        }

        if (pkgInfo == null || pkgInfo.applicationInfo == null) {
            return ApkMetadata(
                filePath = file.absolutePath,
                fileName = file.name,
                fileSizeBytes = file.length(),
                sourceUrl = sourceUrl,
                isCorrupted = true,
                corruptionReason = "Impossible d'extraire les métadonnées Android du paquet APK."
            )
        }

        val appInfo = pkgInfo.applicationInfo!!
        appInfo.sourceDir = file.absolutePath
        appInfo.publicSourceDir = file.absolutePath

        val appLabel = runCatching { pm.getApplicationLabel(appInfo).toString() }
            .getOrDefault(pkgInfo.packageName)
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val versionName = pkgInfo.versionName ?: "1.0"
        val permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()

        // Signature SHA-256 extraction
        val certSha256 = extractSignatureSha256(pkgInfo)

        // Compare with currently installed package on device
        val installedPkg = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgInfo.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgInfo.packageName, 0)
            }
        }.getOrNull()

        val installedVersionCode = installedPkg?.let { PackageInfoCompat.getLongVersionCode(it) }
        val installedVersionName = installedPkg?.versionName

        val comparison = when {
            installedVersionCode == null -> ApkVersionComparison.NOT_INSTALLED
            versionCode > installedVersionCode -> ApkVersionComparison.UPDATE_AVAILABLE
            versionCode == installedVersionCode -> ApkVersionComparison.SAME_VERSION
            else -> ApkVersionComparison.DOWNGRADE_WARNING
        }

        return ApkMetadata(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSizeBytes = file.length(),
            sourceUrl = sourceUrl,
            isCorrupted = false,
            appName = appLabel,
            packageName = pkgInfo.packageName,
            versionName = versionName,
            versionCode = versionCode,
            installedVersionName = installedVersionName,
            installedVersionCode = installedVersionCode,
            versionComparison = comparison,
            signatureSha256 = certSha256,
            requestedPermissions = permissions,
            minSdkVersion = appInfo.minSdkVersion,
            targetSdkVersion = appInfo.targetSdkVersion
        )
    }

    private fun extractSignatureSha256(pkgInfo: PackageInfo): String {
        return try {
            val signingInfo = pkgInfo.signingInfo ?: return "Non signée ou signature v1 uniquement"
            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            val firstCert = signatures?.firstOrNull()?.toByteArray() ?: return "Certificat non détecté"
            val digest = MessageDigest.getInstance("SHA-256").digest(firstCert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "Vérification par l'installateur système Android"
        }
    }
}
