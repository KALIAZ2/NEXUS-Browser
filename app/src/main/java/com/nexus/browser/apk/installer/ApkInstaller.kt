package com.nexus.browser.apk.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Vérifie si l'utilisateur a autorisé NEXUS à proposer l'installation de paquets APK.
     */
    fun canRequestPackageInstalls(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    /**
     * Ouvre l'écran officiel des paramètres Android pour autoriser la source d'installation.
     */
    fun openUnknownSourcesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Lance l'installateur système Android officiel après confirmation explicite de l'utilisateur.
     * Ne contourne jamais la confirmation système d'Android.
     */
    fun launchSystemPackageInstaller(apkFile: File): Result<Unit> {
        return runCatching {
            require(apkFile.exists() && apkFile.isFile) {
                "Le fichier APK n'existe plus sur le stockage."
            }
            if (!canRequestPackageInstalls()) {
                openUnknownSourcesSettings()
                return@runCatching
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            context.startActivity(installIntent)
        }
    }
}
