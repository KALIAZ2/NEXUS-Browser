package com.nexus.browser.bookmarks

import com.nexus.browser.core.database.BookmarkDao
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.HomeShortcutDao
import com.nexus.browser.core.database.HomeShortcutEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val shortcutDao: HomeShortcutDao
) {
    val allBookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.observeAllBookmarks()
    val foldersFlow: Flow<List<String>> = bookmarkDao.observeFolders()
    val shortcutsFlow: Flow<List<HomeShortcutEntity>> = shortcutDao.observeShortcuts()

    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> {
        return if (query.isBlank()) {
            allBookmarksFlow
        } else {
            bookmarkDao.searchBookmarks(query.trim())
        }
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.findByUrl(url) != null
    }

    suspend fun toggleBookmark(url: String, title: String, folder: String = "Général"): Boolean {
        val existing = bookmarkDao.findByUrl(url)
        return if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    url = url,
                    title = title.ifBlank { url },
                    folder = folder.ifBlank { "Général" }
                )
            )
            true
        }
    }

    suspend fun saveBookmark(
        id: Long = 0,
        url: String,
        title: String,
        folder: String = "Général"
    ) {
        if (id > 0) {
            bookmarkDao.updateBookmark(
                BookmarkEntity(
                    id = id,
                    url = url.trim(),
                    title = title.trim().ifBlank { url.trim() },
                    folder = folder.trim().ifBlank { "Général" }
                )
            )
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    url = url.trim(),
                    title = title.trim().ifBlank { url.trim() },
                    folder = folder.trim().ifBlank { "Général" }
                )
            )
        }
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun ensureDefaultShortcutsSeeded() {
        if (shortcutDao.count() == 0) {
            val defaults = listOf(
                HomeShortcutEntity(title = "Google", url = "https://www.google.com", iconColorHex = "#4285F4", position = 0),
                HomeShortcutEntity(title = "YouTube", url = "https://m.youtube.com", iconColorHex = "#FF0000", position = 1),
                HomeShortcutEntity(title = "GitHub", url = "https://github.com", iconColorHex = "#24292F", position = 2),
                HomeShortcutEntity(title = "Wikipedia", url = "https://fr.wikipedia.org", iconColorHex = "#10B981", position = 3)
            )
            shortcutDao.insertAll(defaults)
        }
    }

    suspend fun addOrUpdateShortcut(id: Long = 0, title: String, url: String, colorHex: String = "#5685FF", position: Int = 0) {
        val currentCount = shortcutDao.count()
        if (id > 0) {
            shortcutDao.update(
                HomeShortcutEntity(
                    id = id,
                    title = title.trim(),
                    url = url.trim(),
                    iconColorHex = colorHex,
                    position = position
                )
            )
        } else {
            shortcutDao.insert(
                HomeShortcutEntity(
                    title = title.trim(),
                    url = url.trim(),
                    iconColorHex = colorHex,
                    position = currentCount
                )
            )
        }
    }

    suspend fun reorderShortcut(shortcutId: Long, moveLeft: Boolean) {
        val all = shortcutDao.getAllShortcuts().toMutableList()
        val index = all.indexOfFirst { it.id == shortcutId }
        if (index == -1) return
        val targetIndex = if (moveLeft) {
            (index - 1).coerceAtLeast(0)
        } else {
            (index + 1).coerceAtMost(all.lastIndex)
        }
        if (index == targetIndex) return
        val moved = all.removeAt(index)
        all.add(targetIndex, moved)
        val reindexed = all.mapIndexed { pos, item -> item.copy(position = pos) }
        shortcutDao.insertAll(reindexed)
    }

    suspend fun deleteShortcut(id: Long) {
        shortcutDao.deleteById(id)
    }
}
