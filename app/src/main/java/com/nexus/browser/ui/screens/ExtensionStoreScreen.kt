package com.nexus.browser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusLoadingIndicator
import com.nexus.browser.core.designsystem.NexusTextField
import com.nexus.browser.extensions.permissions.ExtensionPermissionsFormatter
import com.nexus.browser.extensions.store.ExtensionStoreCategory
import com.nexus.browser.extensions.store.StoreExtensionItem

@Composable
fun ExtensionStoreScreen(
    storeItems: List<StoreExtensionItem>,
    installedExtensions: List<ExtensionMetadataEntity>,
    isSearchingOnline: Boolean,
    onFilterChange: (ExtensionStoreCategory, String, Boolean) -> Unit,
    onSearchOnlineMozilla: (String) -> Unit,
    onInstallItem: (StoreExtensionItem) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var selectedCategory by remember { mutableStateOf(ExtensionStoreCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyCompatible by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<StoreExtensionItem?>(null) }

    val installedIds = remember(installedExtensions) {
        installedExtensions.map { it.extensionId }.toSet()
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
                text = "Boutique d'Extensions NEXUS",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recherche et filtre de compatibilité
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexusTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onFilterChange(selectedCategory, it, onlyCompatible)
                            },
                            label = "Rechercher une extension...",
                            placeholder = "uBlock, Dark Reader, Bitwarden, adblock...",
                            modifier = Modifier.weight(1f),
                            onImeAction = { onSearchOnlineMozilla(searchQuery) }
                        )
                        NexusButton(
                            text = "API Mozilla",
                            icon = Icons.Default.Search,
                            onClick = { onSearchOnlineMozilla(searchQuery) }
                        )
                    }

                    // Catégories horizontales
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtensionStoreCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    onFilterChange(category, searchQuery, onlyCompatible)
                                },
                                label = { Text(category.labelFr) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Masquer les extensions incompatibles (ex: Chrome CRX)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(
                            checked = onlyCompatible,
                            onCheckedChange = {
                                onlyCompatible = it
                                onFilterChange(selectedCategory, searchQuery, it)
                            }
                        )
                    }
                }
            }

            if (isSearchingOnline) {
                item {
                    NexusLoadingIndicator(label = "Interrogation de l'API officielle Mozilla Add-ons v5...")
                }
            }

            items(storeItems, key = { it.id }) { item ->
                val isInstalled = item.id in installedIds
                val isInstallable = item.compatibilityLevel.isInstallableInGecko

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailItem = item },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isInstallable) palette.border else palette.error.copy(alpha = 0.6f)
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
                                    text = "${item.name} • v${item.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.author} • ${item.category.labelFr} • ★ ${String.format("%.1f", item.rating)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                            NexusButton(
                                text = when {
                                    isInstalled -> "Installée"
                                    !isInstallable -> "Incompatible"
                                    else -> "Installer"
                                },
                                icon = if (isInstalled) Icons.Default.Verified else Icons.Default.CloudDownload,
                                enabled = !isInstalled && isInstallable,
                                isSecondary = isInstalled || !isInstallable,
                                onClick = { detailItem = item }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.compatibilityLevel.labelFr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isInstallable) NexusColors.Success else palette.error
                        )
                    }
                }
            }
        }
    }

    detailItem?.let { item ->
        val perms = ExtensionPermissionsFormatter.describePermissions(item.permissions)
        val isInstallable = item.compatibilityLevel.isInstallableInGecko

        NexusDialog(
            title = item.name,
            onDismissRequest = { detailItem = null },
            confirmText = if (isInstallable) "Confirmer l'installation" else "Fermer",
            dismissText = "Annuler",
            onConfirm = {
                if (isInstallable) {
                    onInstallItem(item)
                }
                detailItem = null
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auteur : ${item.author} • Version : ${item.version}")
                Text("Catégorie : ${item.category.labelFr}")
                Text("Compatibilité : ${item.compatibilityLevel.labelFr}", fontWeight = FontWeight.Bold)
                Text(item.fullDescription)
                if (item.xpiDownloadUrl.isNotBlank()) {
                    Text("Source officielle HTTPS : ${item.xpiDownloadUrl}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text("Permissions demandées (${perms.size}) :", fontWeight = FontWeight.Bold)
                if (perms.isEmpty()) {
                    Text("Aucune permission spéciale requise.")
                } else {
                    perms.forEach { p ->
                        Text("• ${p.titleFr} : ${p.descriptionFr}")
                    }
                }
            }
        }
    }
}
