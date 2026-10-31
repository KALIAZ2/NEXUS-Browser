package com.nexus.browser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.browser.apk.scanner.ApkMetadata
import com.nexus.browser.core.database.DownloadEntity
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusButton
import com.nexus.browser.core.designsystem.NexusColors
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusDownloadCard
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusTextField
import com.nexus.browser.core.designsystem.formatBytes

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onStartDirectDownload: (String) -> Unit,
    onPauseResume: (DownloadEntity) -> Unit,
    onOpen: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onOpenFolder: () -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onOpenApkManager: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var showNewDownloadDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Retour",
                onClick = onBack
            )
            Text(
                text = "Téléchargements (${downloads.size})",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NexusIconButton(
                icon = Icons.Default.Android,
                contentDescription = "Gestionnaire APK",
                onClick = onOpenApkManager,
                tint = NexusColors.Success
            )
            NexusIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Nouveau téléchargement",
                onClick = { showNewDownloadDialog = true },
                tint = palette.primary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (downloads.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.border),
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Aucun téléchargement en cours",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Les fichiers téléchargés depuis vos pages web GeckoView ou ajoutés via le bouton + apparaîtront ici avec suivi de vitesse, pause/reprise HTTP Range et partage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            items(downloads, key = { it.id }) { dl ->
                NexusDownloadCard(
                    fileName = dl.fileName,
                    sourceUrl = dl.sourceUrl,
                    status = dl.status,
                    downloadedBytes = dl.downloadedBytes,
                    totalBytes = dl.totalBytes,
                    speedBytesPerSec = dl.speedBytesPerSec,
                    onPauseResume = { onPauseResume(dl) },
                    onOpen = { onOpen(dl) },
                    onShare = { onShare(dl) },
                    onShowInFolder = onOpenFolder,
                    onDelete = { onDelete(dl) }
                )
            }
        }
    }

    if (showNewDownloadDialog) {
        var urlInput by remember { mutableStateOf("") }
        NexusDialog(
            title = "Télécharger depuis une URL",
            onDismissRequest = { showNewDownloadDialog = false },
            confirmText = "Démarrer",
            onConfirm = {
                if (urlInput.isNotBlank()) {
                    onStartDirectDownload(urlInput.trim())
                    showNewDownloadDialog = false
                }
            }
        ) {
            NexusTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = "URL directe du fichier (PDF, ZIP, APK...)",
                placeholder = "https://exemple.org/fichier.apk"
            )
        }
    }
}

@Composable
fun ApkManagerScreen(
    apks: List<ApkMetadata>,
    canInstallPackages: Boolean,
    onOpenInstallPermissionSettings: () -> Unit,
    onInstallApk: (ApkMetadata) -> Unit,
    onDeleteApk: (ApkMetadata) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var pendingInstallApk by remember { mutableStateOf<ApkMetadata?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Retour",
                onClick = onBack
            )
            Text(
                text = "Gestionnaire de Fichiers APK",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bandeau d'état de l'autorisation système REQUEST_INSTALL_PACKAGES
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (canInstallPackages) NexusColors.Success else NexusColors.Warning
                    ),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (canInstallPackages) Icons.Default.Security else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (canInstallPackages) NexusColors.Success else NexusColors.Warning
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (canInstallPackages) {
                                    "Autorisation d'installation d'applications inconnues active"
                                } else {
                                    "Autorisation Android requise pour installer des APK"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "NEXUS inspecte localement l'intégrité ZIP, le manifeste Android, la signature SHA-256 et les permissions demandées. Aucune installation n'est jamais lancée sans votre confirmation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        if (!canInstallPackages) {
                            Spacer(Modifier.height(10.dp))
                            NexusButton(
                                text = "Ouvrir les paramètres système Android",
                                onClick = onOpenInstallPermissionSettings
                            )
                        }
                    }
                }
            }

            if (apks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.border),
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Aucun fichier APK détecté",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Téléchargez un fichier .apk depuis le navigateur ou le gestionnaire de téléchargements pour analyser sa signature et ses permissions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            items(apks, key = { it.filePath }) { apk ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (apk.isCorrupted) palette.error else palette.border
                    ),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = apk.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (apk.packageName.isNotBlank()) {
                                        "${apk.packageName} • v${apk.versionName} (${apk.versionCode})"
                                    } else {
                                        apk.fileName
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                            IconButton(onClick = { onDeleteApk(apk) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer APK", tint = palette.error)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (apk.isCorrupted) {
                            Text(
                                text = "❌ Paquet endommagé : ${apk.corruptionReason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Statut : ${apk.versionComparison.labelFr}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary
                            )
                            Text(
                                text = "Taille : ${formatBytes(apk.fileSizeBytes)} • SDK min ${apk.minSdkVersion} / cible ${apk.targetSdkVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "Empreinte SHA-256 : ${apk.signatureSha256.take(47)}...",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                            if (apk.requestedPermissions.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Permissions Android (${apk.requestedPermissions.size}) : ${
                                        apk.requestedPermissions.take(4).joinToString(", ") { it.substringAfterLast(".") }
                                    }",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NexusColors.Warning
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            NexusButton(
                                text = "Vérifier et installer l'APK",
                                icon = Icons.Default.SystemUpdateAlt,
                                onClick = { pendingInstallApk = apk },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    pendingInstallApk?.let { apk ->
        NexusDialog(
            title = "Confirmation de sécurité APK",
            onDismissRequest = { pendingInstallApk = null },
            confirmText = "Lancer l'installateur Android",
            dismissText = "Annuler",
            onConfirm = {
                onInstallApk(apk)
                pendingInstallApk = null
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(apk.securityNoticeFr, color = NexusColors.Warning, fontWeight = FontWeight.SemiBold)
                Text("Application : ${apk.appName} (${apk.packageName})")
                Text("Version : ${apk.versionName} (code ${apk.versionCode})")
                if (apk.installedVersionName != null) {
                    Text("Version actuellement installée : ${apk.installedVersionName} (${apk.installedVersionCode})")
                }
                Text("Origine : ${apk.sourceUrl.ifBlank { "Stockage local" }}")
                Text("Signature SHA-256 : ${apk.signatureSha256}")
                if (apk.requestedPermissions.isNotEmpty()) {
                    Text("Permissions demandées (${apk.requestedPermissions.size}) :", fontWeight = FontWeight.Bold)
                    apk.requestedPermissions.take(8).forEach { perm ->
                        Text("• $perm")
                    }
                }
            }
        }
    }
}
