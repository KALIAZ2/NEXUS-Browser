package com.nexus.browser.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.browser.browser.tabs.BrowserTab
import com.nexus.browser.browser.tabs.BrowserWindow
import com.nexus.browser.browser.tabs.ClosedBrowserWindow
import com.nexus.browser.browser.tabs.ClosedTabGroup
import com.nexus.browser.browser.tabs.TabGroup
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusColors
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusTabCard
import com.nexus.browser.core.designsystem.NexusTextField

private enum class TabsCategory {
    ALL,
    PINNED,
    RECENTLY_CLOSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    windows: List<BrowserWindow> = emptyList(),
    activeWindowId: String = "default_window",
    tabGroups: List<TabGroup> = emptyList(),
    closedTabGroups: List<ClosedTabGroup> = emptyList(),
    closedWindows: List<ClosedBrowserWindow> = emptyList(),
    recentlyClosedTabs: List<BrowserTab> = emptyList(),
    tabsLayoutMode: String = "GRID",
    undoBannerMessage: String? = null,
    onSelectTab: (String) -> Unit,
    onCreateTab: (Boolean, String?, String?) -> Unit = { priv, winId, gId -> },
    onDuplicateTab: (String) -> Unit,
    onReorderTab: (String, Boolean) -> Unit,
    onCloseTab: (String) -> Unit,
    onTogglePinTab: (String) -> Unit = {},
    onCloseAllTabs: (Boolean) -> Unit,
    onRestoreClosedTab: (String?) -> Unit = {},
    onClearClosedTabs: () -> Unit = {},
    // Multi-fenêtres
    onCreateWindow: (String, Boolean) -> Unit = { _, _ -> },
    onRenameWindow: (String, String) -> Unit = { _, _ -> },
    onSelectWindow: (String) -> Unit = {},
    onCloseWindow: (String) -> Unit = {},
    onRestoreClosedWindow: (String) -> Unit = {},
    onMoveTabToWindow: (String, String) -> Unit = { _, _ -> },
    // Groupes d'onglets
    onCreateTabGroup: (String, String) -> Unit = { _, _ -> },
    onUpdateTabGroup: (String, String, String) -> Unit = { _, _, _ -> },
    onAssignTabToGroup: (String, String?) -> Unit = { _, _ -> },
    onCloseTabGroup: (String) -> Unit = {},
    onRestoreClosedTabGroup: (String) -> Unit = {},
    // Préférences & miniature
    onSetTabsLayoutMode: (String) -> Unit = {},
    onPerformUndoClose: () -> Unit = {},
    onDismissUndoBanner: () -> Unit = {},
    loadTabPreview: (String, String?) -> Bitmap? = { _, _ -> null },
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current

    val currentWindow = windows.firstOrNull { it.id == activeWindowId }
    val isCurrentWindowPrivate = currentWindow?.isPrivate ?: false

    var selectedCategory by remember { mutableStateOf(TabsCategory.ALL) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }

    // Dialogues
    var showNewWindowDialog by remember { mutableStateOf(false) }
    var showRenameWindowDialog by remember { mutableStateOf<BrowserWindow?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf<TabGroup?>(null) }
    var tabContextMenuTarget by remember { mutableStateOf<BrowserTab?>(null) }

    // Filtrage des onglets appartenant à la fenêtre active
    val windowTabs = remember(tabs, activeWindowId) {
        tabs.filter { it.windowId == activeWindowId }
    }

    val displayTabs = remember(windowTabs, selectedCategory, selectedGroupId) {
        when {
            selectedCategory == TabsCategory.PINNED -> windowTabs.filter { it.isPinned }
            selectedGroupId != null -> windowTabs.filter { it.groupId == selectedGroupId }
            else -> windowTabs
        }
    }

    val isListMode = tabsLayoutMode.equals("LIST", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // --- 1. EN-TÊTE PRINCIPAL : "Onglets & Fenêtres", Bouton Grille/Liste, "+" ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                NexusIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    onClick = onBack
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Onglets",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${windowTabs.size} onglet(s) dans ${currentWindow?.name ?: "cette fenêtre"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Bascule Grille / Liste
                IconButton(
                    onClick = {
                        val nextMode = if (isListMode) "GRID" else "LIST"
                        onSetTabsLayoutMode(nextMode)
                    }
                ) {
                    Icon(
                        imageVector = if (isListMode) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = if (isListMode) "Passer en mode grille" else "Passer en mode liste",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Nouvel onglet
                IconButton(
                    onClick = {
                        onCreateTab(isCurrentWindowPrivate, activeWindowId, selectedGroupId)
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouvel onglet",
                        tint = palette.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // --- 2. BARRE DES FENÊTRES DE NAVIGATION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            windows.forEach { win ->
                val isSelected = win.id == activeWindowId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) palette.primary else palette.surfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectWindow(win.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (win.isPrivate) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Privé",
                                tint = if (isSelected) Color.White else NexusColors.PrivateAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else palette.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = win.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else palette.textPrimary
                        )

                        if (!win.isPrivate) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Renommer la fenêtre",
                                tint = if (isSelected) Color.White.copy(alpha = 0.8f) else palette.textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { showRenameWindowDialog = win }
                            )
                        }

                        if (windows.size > 1 && win.id != "default_window") {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer la fenêtre",
                                tint = if (isSelected) Color.White.copy(alpha = 0.8f) else palette.textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onCloseWindow(win.id) }
                            )
                        }
                    }
                }
            }

            // Bouton + Fenêtre
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showNewWindowDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Fenêtre",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 3. BARRE DES GROUPES ET FILTRES (Tous, Épinglés, Groupes personnalisés, Fermés) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tous
            FilterChip(
                selected = selectedCategory == TabsCategory.ALL && selectedGroupId == null,
                onClick = {
                    selectedCategory = TabsCategory.ALL
                    selectedGroupId = null
                },
                label = { Text("Tous (${windowTabs.size})") }
            )

            // Épinglés
            val pinnedCount = windowTabs.count { it.isPinned }
            if (pinnedCount > 0) {
                FilterChip(
                    selected = selectedCategory == TabsCategory.PINNED,
                    onClick = {
                        selectedCategory = TabsCategory.PINNED
                        selectedGroupId = null
                    },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("Épinglés ($pinnedCount)") }
                )
            }

            // Groupes d'onglets personnalisés de la fenêtre
            val currentWindowGroups = tabGroups.filter { it.windowId == activeWindowId }
            currentWindowGroups.forEach { group ->
                val groupCount = windowTabs.count { it.groupId == group.id }
                val isSelected = selectedGroupId == group.id
                val groupColor = runCatching { Color(android.graphics.Color.parseColor(group.colorHex)) }
                    .getOrDefault(palette.primary)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = TabsCategory.ALL
                        selectedGroupId = if (isSelected) null else group.id
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(groupColor)
                        )
                    },
                    label = { Text("${group.name} ($groupCount)") }
                )
            }

            // Bouton + Groupe
            if (!isCurrentWindowPrivate) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showNewGroupDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = palette.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+ Groupe",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Récemment fermés
            FilterChip(
                selected = selectedCategory == TabsCategory.RECENTLY_CLOSED,
                onClick = {
                    selectedCategory = TabsCategory.RECENTLY_CLOSED
                    selectedGroupId = null
                },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text("Fermés (${recentlyClosedTabs.size + closedTabGroups.size + closedWindows.size})") }
            )
        }

        // Actions spécifiques sur le groupe sélectionné
        if (selectedGroupId != null) {
            val activeGroup = tabGroups.firstOrNull { it.id == selectedGroupId }
            if (activeGroup != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Groupe : ${activeGroup.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = runCatching { Color(android.graphics.Color.parseColor(activeGroup.colorHex)) }.getOrDefault(palette.primary)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showEditGroupDialog = activeGroup },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Modifier", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = {
                                onCloseTabGroup(activeGroup.id)
                                selectedGroupId = null
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Fermer le groupe", style = MaterialTheme.typography.labelSmall, color = palette.error)
                        }
                    }
                }
            }
        }

        // --- 4. BANNIÈRE FLOTTANTE D'ANNULATION (UNDO BANNER) ---
        AnimatedVisibility(
            visible = !undoBannerMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = undoBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onPerformUndoClose) {
                            Text("ANNULER", fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                        IconButton(onClick = onDismissUndoBanner, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // --- 5. CONTENU PRINCIPAL DES ONGLETS OU RÉCEMMENT FERMÉS ---
        if (selectedCategory == TabsCategory.RECENTLY_CLOSED) {
            // Affichage des onglets, groupes et fenêtres récemment fermés
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fenêtres fermées
                if (closedWindows.isNotEmpty()) {
                    item {
                        Text("Fenêtres fermées", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    items(closedWindows, key = { it.id }) { closedWin ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRestoreClosedWindow(closedWin.id) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(closedWin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text("${closedWin.tabsCount} onglet(s) sauvegardés", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                TextButton(onClick = { onRestoreClosedWindow(closedWin.id) }) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restaurer")
                                }
                            }
                        }
                    }
                }

                // Groupes fermés
                if (closedTabGroups.isNotEmpty()) {
                    item {
                        Text("Groupes d'onglets fermés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    items(closedTabGroups, key = { it.id }) { closedGroup ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRestoreClosedTabGroup(closedGroup.id) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(closedGroup.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text("${closedGroup.tabsCount} onglet(s) sauvegardés", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                                TextButton(onClick = { onRestoreClosedTabGroup(closedGroup.id) }) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restaurer")
                                }
                            }
                        }
                    }
                }

                // Onglets individuels fermés
                item {
                    Text("Onglets récents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                }

                if (recentlyClosedTabs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Aucun onglet récemment fermé",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Les onglets que vous fermez sont conservés ici afin de pouvoir être restaurés facilement.",
                                    style = MaterialTheme.typography.bodySmall,
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
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.border),
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = closedTab.title.ifBlank { "Nouvel onglet" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = closedTab.url.ifBlank { "nexus://home" },
                                        style = MaterialTheme.typography.labelSmall,
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
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        .padding(vertical = 12.dp),
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
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Effacer l'historique des onglets fermés",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else if (isListMode) {
            // --- MODE LISTE (Compact avec balayage SwipeToDismiss pour fermer) ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayTabs, key = { it.id }) { tab ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                onCloseTab(tab.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.error.copy(alpha = 0.25f))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Fermer", tint = palette.error)
                            }
                        }
                    ) {
                        val group = tabGroups.firstOrNull { it.id == tab.groupId }
                        NexusTabCard(
                            title = tab.title,
                            url = tab.url,
                            isActive = tab.id == activeTabId,
                            isPrivate = tab.isPrivate,
                            isPinned = tab.isPinned,
                            groupColorHex = group?.colorHex,
                            groupName = group?.name,
                            isListMode = true,
                            previewBitmap = loadTabPreview(tab.id, tab.previewPath),
                            onSelect = {
                                onSelectTab(tab.id)
                                onBack()
                            },
                            onClose = { onCloseTab(tab.id) },
                            onDuplicate = { onDuplicateTab(tab.id) },
                            onLongClick = { tabContextMenuTarget = tab }
                        )
                    }
                }
            }
        } else {
            // --- MODE GRILLE (Visuel avec miniatures capturées, 2 colonnes) ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayTabs, key = { it.id }) { tab ->
                    val group = tabGroups.firstOrNull { it.id == tab.groupId }
                    NexusTabCard(
                        title = tab.title,
                        url = tab.url,
                        isActive = tab.id == activeTabId,
                        isPrivate = tab.isPrivate,
                        isPinned = tab.isPinned,
                        groupColorHex = group?.colorHex,
                        groupName = group?.name,
                        isListMode = false,
                        previewBitmap = loadTabPreview(tab.id, tab.previewPath),
                        onSelect = {
                            onSelectTab(tab.id)
                            onBack()
                        },
                        onClose = { onCloseTab(tab.id) },
                        onDuplicate = { onDuplicateTab(tab.id) },
                        onLongClick = { tabContextMenuTarget = tab }
                    )
                }
            }
        }

        // --- 6. BAS DE PAGE : "Tout fermer" pilule centrée ---
        if (selectedCategory != TabsCategory.RECENTLY_CLOSED && windowTabs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            onCloseAllTabs(isCurrentWindowPrivate)
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
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // --- DIALOGUE CRÉATION DE FENÊTRE ---
    if (showNewWindowDialog) {
        var winName by remember { mutableStateOf("Fenêtre ${windows.size + 1}") }
        var isPriv by remember { mutableStateOf(false) }

        NexusDialog(
            title = "Nouvelle fenêtre de navigation",
            confirmText = "Créer",
            onDismissRequest = { showNewWindowDialog = false },
            onConfirm = {
                if (winName.isNotBlank()) {
                    onCreateWindow(winName.trim(), isPriv)
                    showNewWindowDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusTextField(
                    value = winName,
                    onValueChange = { winName = it },
                    label = "Nom de la fenêtre",
                    placeholder = "Ex : Travail, Personnel, Projets"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isPriv,
                        onClick = { isPriv = false },
                        label = { Text("Standard") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = isPriv,
                        onClick = { isPriv = true },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Privée") }
                    )
                }
            }
        }
    }

    // --- DIALOGUE RENOMMER FENÊTRE ---
    if (showRenameWindowDialog != null) {
        val win = showRenameWindowDialog!!
        var newWinName by remember { mutableStateOf(win.name) }

        NexusDialog(
            title = "Renommer la fenêtre",
            confirmText = "Enregistrer",
            onDismissRequest = { showRenameWindowDialog = null },
            onConfirm = {
                if (newWinName.isNotBlank()) {
                    onRenameWindow(win.id, newWinName.trim())
                    showRenameWindowDialog = null
                }
            }
        ) {
            NexusTextField(
                value = newWinName,
                onValueChange = { newWinName = it },
                label = "Nouveau nom",
                placeholder = "Ex : Recherches"
            )
        }
    }

    // --- DIALOGUE CRÉATION DE GROUPE ---
    if (showNewGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        val colors = listOf("#2979FF", "#00E5FF", "#8B5CF6", "#10B981", "#F59E0B", "#EC4899", "#EF4444")
        var selectedColor by remember { mutableStateOf(colors[0]) }

        NexusDialog(
            title = "Nouveau groupe d'onglets",
            confirmText = "Créer",
            onDismissRequest = { showNewGroupDialog = false },
            onConfirm = {
                if (groupName.isNotBlank()) {
                    onCreateTabGroup(groupName.trim(), selectedColor)
                    showNewGroupDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "Nom du groupe",
                    placeholder = "Ex : Travail, Jeux, Actualités"
                )
                Text("Couleur du badge :", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { cHex ->
                        val colorObj = Color(android.graphics.Color.parseColor(cHex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colorObj)
                                .clickable { selectedColor = cHex }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == cHex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGUE MODIFIER GROUPE ---
    if (showEditGroupDialog != null) {
        val grp = showEditGroupDialog!!
        var editName by remember { mutableStateOf(grp.name) }
        val colors = listOf("#2979FF", "#00E5FF", "#8B5CF6", "#10B981", "#F59E0B", "#EC4899", "#EF4444")
        var editColor by remember { mutableStateOf(grp.colorHex) }

        NexusDialog(
            title = "Modifier le groupe",
            confirmText = "Enregistrer",
            onDismissRequest = { showEditGroupDialog = null },
            onConfirm = {
                if (editName.isNotBlank()) {
                    onUpdateTabGroup(grp.id, editName.trim(), editColor)
                    showEditGroupDialog = null
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = "Nom du groupe",
                    placeholder = "Ex : Mes articles"
                )
                Text("Couleur du badge :", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { cHex ->
                        val colorObj = Color(android.graphics.Color.parseColor(cHex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colorObj)
                                .clickable { editColor = cHex }
                                .padding(2.dp)
                        ) {
                            if (editColor == cHex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MENU CONTEXTUEL APPUIS LONG SUR UN ONGLET ---
    if (tabContextMenuTarget != null) {
        val targetTab = tabContextMenuTarget!!
        ModalBottomSheet(
            onDismissRequest = { tabContextMenuTarget = null },
            containerColor = palette.surfaceElevated,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = targetTab.title.ifBlank { targetTab.url },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = targetTab.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = palette.border)

                // 1. Épingler / Désépingler
                ContextMenuItem(
                    icon = Icons.Default.PushPin,
                    title = if (targetTab.isPinned) "Désépingler l'onglet" else "Épingler l'onglet"
                ) {
                    onTogglePinTab(targetTab.id)
                    tabContextMenuTarget = null
                }

                // 2. Dupliquer
                ContextMenuItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Dupliquer l'onglet"
                ) {
                    onDuplicateTab(targetTab.id)
                    tabContextMenuTarget = null
                }

                // 3. Assigner à un groupe
                if (!targetTab.isPrivate && tabGroups.isNotEmpty()) {
                    Text(
                        text = "Assigner au groupe :",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (targetTab.groupId == null) palette.primary else palette.surface,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onAssignTabToGroup(targetTab.id, null)
                                    tabContextMenuTarget = null
                                }
                        ) {
                            Text("Aucun groupe", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        tabGroups.filter { it.windowId == targetTab.windowId }.forEach { grp ->
                            val isSel = targetTab.groupId == grp.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) palette.primary else palette.surface,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAssignTabToGroup(targetTab.id, grp.id)
                                        tabContextMenuTarget = null
                                    }
                            ) {
                                Text(grp.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 4. Déplacer vers une autre fenêtre
                if (windows.size > 1) {
                    Text(
                        text = "Déplacer vers la fenêtre :",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        windows.filter { it.id != targetTab.windowId && it.isPrivate == targetTab.isPrivate }.forEach { otherWin ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surface,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onMoveTabToWindow(targetTab.id, otherWin.id)
                                        tabContextMenuTarget = null
                                    }
                            ) {
                                Text(otherWin.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 5. Fermer
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = palette.border)
                ContextMenuItem(
                    icon = Icons.Default.Close,
                    title = "Fermer cet onglet",
                    tint = palette.error
                ) {
                    onCloseTab(targetTab.id)
                    tabContextMenuTarget = null
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = LocalNexusPalette.current.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, fontWeight = FontWeight.Medium)
    }
}
