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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.HistoryEntity
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    folders: List<String>,
    onOpenUrl: (String) -> Unit,
    onSaveBookmark: (Long, String, String, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var selectedFolder by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val filtered = remember(bookmarks, selectedFolder, searchQuery) {
        bookmarks.filter { bm ->
            val matchFolder = selectedFolder == "Tous" || bm.folder.equals(selectedFolder, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchSearch = q.isEmpty() || bm.title.lowercase().contains(q) || bm.url.lowercase().contains(q)
            matchFolder && matchSearch
        }
    }

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
                text = "Favoris (${bookmarks.size})",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NexusIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Ajouter un favori",
                onClick = {
                    editingBookmark = null
                    showEditDialog = true
                },
                tint = palette.primary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NexusTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Rechercher dans les favoris..."
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allFolders = listOf("Tous") + folders.distinct()
                    allFolders.forEach { folder ->
                        FilterChip(
                            selected = selectedFolder == folder,
                            onClick = { selectedFolder = folder },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            label = { Text(folder) }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { bm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenUrl(bm.url)
                            onBack()
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bm.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${bm.folder} • ${bm.url}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            editingBookmark = bm
                            showEditDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = palette.textSecondary)
                        }
                        IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = palette.error)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var title by remember { mutableStateOf(editingBookmark?.title.orEmpty()) }
        var url by remember { mutableStateOf(editingBookmark?.url.orEmpty()) }
        var folder by remember { mutableStateOf(editingBookmark?.folder ?: "Général") }

        NexusDialog(
            title = if (editingBookmark == null) "Nouveau favori" else "Modifier le favori",
            onDismissRequest = { showEditDialog = false },
            onConfirm = {
                if (url.isNotBlank()) {
                    onSaveBookmark(
                        editingBookmark?.id ?: 0L,
                        title.ifBlank { url },
                        url,
                        folder.ifBlank { "Général" }
                    )
                    showEditDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusTextField(value = title, onValueChange = { title = it }, label = "Titre")
                NexusTextField(value = url, onValueChange = { url = it }, label = "URL (https://...)")
                NexusTextField(value = folder, onValueChange = { folder = it }, label = "Dossier")
            }
        }
    }
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    onOpenUrl: (String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearSince: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var sinceTimestamp by remember { mutableLongStateOf(0L) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    val filtered = remember(history, searchQuery, sinceTimestamp) {
        history.filter { item ->
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() || item.title.lowercase().contains(q) || item.url.lowercase().contains(q)
            val matchTime = item.lastVisitedAt >= sinceTimestamp
            matchQuery && matchTime
        }
    }

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
                text = "Historique de navigation",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NexusIconButton(
                icon = Icons.Default.DeleteSweep,
                contentDescription = "Tout effacer",
                onClick = onClearAll,
                tint = palette.error
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NexusTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Rechercher dans l'historique..."
                )
                Spacer(Modifier.height(8.dp))
                val now = System.currentTimeMillis()
                val filters = listOf(
                    0L to "Tout l'historique",
                    (now - 24 * 3600 * 1000L) to "Dernières 24h",
                    (now - 7 * 24 * 3600 * 1000L) to "7 derniers jours"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { (ts, label) ->
                        FilterChip(
                            selected = sinceTimestamp == ts,
                            onClick = { sinceTimestamp = ts },
                            label = { Text(label) }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenUrl(item.url)
                            onBack()
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.url,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${dateFormat.format(Date(item.lastVisitedAt))} • ${item.visitCount} visite(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary
                            )
                        }
                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = palette.error)
                        }
                    }
                }
            }
        }
    }
}
