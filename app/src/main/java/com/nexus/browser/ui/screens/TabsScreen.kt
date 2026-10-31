package com.nexus.browser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.browser.browser.tabs.BrowserTab
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusTabCard

private enum class TabsViewMode {
    STANDARD,
    PRIVATE,
    RECENTLY_CLOSED
}

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    recentlyClosedTabs: List<BrowserTab> = emptyList(),
    onSelectTab: (String) -> Unit,
    onCreateTab: (Boolean) -> Unit,
    onDuplicateTab: (String) -> Unit,
    onReorderTab: (String, Boolean) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseAllTabs: (Boolean) -> Unit,
    onRestoreClosedTab: (String?) -> Unit = {},
    onClearClosedTabs: () -> Unit = {},
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var viewMode by remember { mutableStateOf(TabsViewMode.STANDARD) }

    val filteredTabs = remember(tabs, viewMode) {
        when (viewMode) {
            TabsViewMode.STANDARD -> tabs.filter { !it.isPrivate }
            TabsViewMode.PRIVATE -> tabs.filter { it.isPrivate }
            TabsViewMode.RECENTLY_CLOSED -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // En-tête conforme à l'écran 2 de la direction artistique : "Onglets" à gauche, "+" à droite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                NexusIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    onClick = onBack
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Onglets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (recentlyClosedTabs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onRestoreClosedTab(null)
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Rouvrir le dernier onglet fermé",
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onCreateTab(viewMode == TabsViewMode.PRIVATE)
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouvel onglet",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Sélecteur Onglets Standards / Navigation Privée / Récemment fermés
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = viewMode == TabsViewMode.STANDARD,
                onClick = { viewMode = TabsViewMode.STANDARD },
                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Standards (${tabs.count { !it.isPrivate }})") }
            )
            FilterChip(
                selected = viewMode == TabsViewMode.PRIVATE,
                onClick = { viewMode = TabsViewMode.PRIVATE },
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Privés (${tabs.count { it.isPrivate }})") }
            )
            FilterChip(
                selected = viewMode == TabsViewMode.RECENTLY_CLOSED,
                onClick = { viewMode = TabsViewMode.RECENTLY_CLOSED },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Récemment fermés (${recentlyClosedTabs.size})") }
            )
        }

        if (viewMode == TabsViewMode.RECENTLY_CLOSED) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (recentlyClosedTabs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Aucun onglet récemment fermé",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Les onglets standards que vous fermez sont conservés ici afin de pouvoir les rouvrir en un geste.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(recentlyClosedTabs, key = { it.id }) { closedTab ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRestoreClosedTab(closedTab.id)
                                    onBack()
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = closedTab.title.ifBlank { "Nouvel onglet" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = closedTab.url.ifBlank { "nexus://home" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        onRestoreClosedTab(closedTab.id)
                                        onBack()
                                    }
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rouvrir")
                                }
                            }
                        }
                    }
                }
            }

            if (recentlyClosedTabs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onClearClosedTabs() },
                        shape = RoundedCornerShape(999.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Vider l'historique des onglets fermés",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // Pile verticale de cartes d'onglets (fidèle à la maquette Phone 2)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredTabs, key = { it.id }) { tab ->
                    Column {
                        NexusTabCard(
                            title = tab.title,
                            url = tab.url,
                            isActive = tab.id == activeTabId,
                            isPrivate = tab.isPrivate,
                            onSelect = {
                                onSelectTab(tab.id)
                                onBack()
                            },
                            onClose = { onCloseTab(tab.id) },
                            onDuplicate = { onDuplicateTab(tab.id) }
                        )
                        if (filteredTabs.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onDuplicateTab(tab.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Dupliquer l'onglet",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onReorderTab(tab.id, true) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Monter",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onReorderTab(tab.id, false) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Descendre",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bouton pilule centré "Tout fermer" en bas de l'écran (conforme à l'écran 2 de la direction artistique)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            onCloseAllTabs(viewMode == TabsViewMode.PRIVATE)
                            onBack()
                        },
                    shape = RoundedCornerShape(999.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        text = "Tout fermer",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
