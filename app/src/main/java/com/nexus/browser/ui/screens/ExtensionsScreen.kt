package com.nexus.browser.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.browser.core.database.ExtensionMetadataEntity
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusButton
import com.nexus.browser.core.designsystem.NexusColors
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusExtensionCard
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityReport
import com.nexus.browser.extensions.permissions.ExtensionPermissionsFormatter

@Composable
fun ExtensionsScreen(
    installedExtensions: List<ExtensionMetadataEntity>,
    lastCompatibilityReport: ExtensionCompatibilityReport?,
    onToggleExtension: (String, Boolean) -> Unit,
    onUninstallExtension: (String) -> Unit,
    onCheckUpdate: (String) -> Unit,
    onImportLocalXpiUri: (Uri) -> Unit,
    onOpenStore: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var selectedExtension by remember { mutableStateOf<ExtensionMetadataEntity?>(null) }

    val xpiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportLocalXpiUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Barre supérieure
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
                text = "Gestionnaire d'Extensions",
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
            // Actions principales : Boutique et Import .XPI local
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.border),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Moteur WebExtensions Mozilla GeckoView",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Installez des extensions officielles .xpi depuis la boutique NEXUS ou importez un paquet WebExtension local compatible.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NexusButton(
                                text = "Boutique",
                                icon = Icons.Default.Storefront,
                                onClick = onOpenStore,
                                modifier = Modifier.weight(1f)
                            )
                            NexusButton(
                                text = "Importer .XPI",
                                icon = Icons.Default.FileUpload,
                                isSecondary = true,
                                onClick = {
                                    xpiPickerLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Dernier rapport de l'analyseur de compatibilité (si disponible)
            if (lastCompatibilityReport != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (lastCompatibilityReport.level.isInstallableInGecko) NexusColors.Success else palette.error
                        ),
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Rapport d'Analyse de Compatibilité : ${lastCompatibilityReport.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Statut : ${lastCompatibilityReport.level.labelFr} • Manifest V${lastCompatibilityReport.manifestVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (lastCompatibilityReport.level.isInstallableInGecko) NexusColors.Success else palette.error
                            )
                            if (lastCompatibilityReport.warnings.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                lastCompatibilityReport.warnings.forEach { w ->
                                    Text("• Avertissement : $w", style = MaterialTheme.typography.bodyMedium, color = NexusColors.Warning)
                                }
                            }
                            if (lastCompatibilityReport.errors.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                lastCompatibilityReport.errors.forEach { err ->
                                    Text("• Erreur bloquante : $err", style = MaterialTheme.typography.bodyMedium, color = palette.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Extensions installées (${installedExtensions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(installedExtensions, key = { it.extensionId }) { ext ->
                NexusExtensionCard(
                    name = ext.name,
                    version = ext.version,
                    author = ext.author,
                    description = ext.description,
                    sourceLabel = when (ext.sourceType) {
                        "BUILT_IN" -> "Intégrée NEXUS"
                        "STORE" -> "Boutique Mozilla/NEXUS"
                        else -> "Paquet XPI local"
                    },
                    compatibilityLabel = "Compatible GeckoView • Manifest V${ext.manifestVersion}",
                    isCompatible = true,
                    enabled = ext.enabled,
                    onToggleEnabled = { enabled -> onToggleExtension(ext.extensionId, enabled) },
                    onDetailsClick = { selectedExtension = ext },
                    onUninstallClick = { onUninstallExtension(ext.extensionId) }
                )
            }
        }
    }

    selectedExtension?.let { ext ->
        val rawPerms = ext.permissionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val formattedPerms = ExtensionPermissionsFormatter.describePermissions(rawPerms)

        NexusDialog(
            title = "${ext.name} (v${ext.version})",
            onDismissRequest = { selectedExtension = null },
            confirmText = "Vérifier mise à jour",
            dismissText = "Fermer",
            onConfirm = {
                onCheckUpdate(ext.extensionId)
                selectedExtension = null
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Identifiant Gecko : ${ext.extensionId}", fontWeight = FontWeight.SemiBold)
                Text("Auteur : ${ext.author}")
                Text("Source : ${ext.sourceType} (${ext.localPathOrUri})")
                Spacer(Modifier.height(4.dp))
                Text("Permissions demandées :", fontWeight = FontWeight.Bold)
                if (formattedPerms.isEmpty()) {
                    Text("Aucune permission sensible requise.")
                } else {
                    formattedPerms.forEach { perm ->
                        Text(
                            text = "• ${perm.titleFr} (${perm.rawPermission}) : ${perm.descriptionFr}",
                            color = if (perm.isSensitive) NexusColors.Warning else palette.textSecondary
                        )
                    }
                }
            }
        }
    }
}
