package com.nexus.browser.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE isPrivate = 0 ORDER BY isPinned DESC, position ASC, lastAccessedAt DESC")
    fun observeNormalTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE isPrivate = 0 ORDER BY isPinned DESC, position ASC, lastAccessedAt DESC")
    suspend fun getPersistedTabs(): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTab(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTabs(tabs: List<TabEntity>)

    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM tabs WHERE isPrivate = :isPrivate")
    suspend fun deleteAllByPrivacy(isPrivate: Boolean)

    @Query("DELETE FROM tabs WHERE windowId = :windowId")
    suspend fun deleteTabsByWindowId(windowId: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAll()

    @Query("SELECT * FROM tab_groups ORDER BY createdAt ASC")
    suspend fun getAllTabGroups(): List<TabGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTabGroup(group: TabGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTabGroups(groups: List<TabGroupEntity>)

    @Query("DELETE FROM tab_groups WHERE id = :groupId")
    suspend fun deleteTabGroupById(groupId: String)

    @Query("DELETE FROM tab_groups WHERE windowId = :windowId")
    suspend fun deleteTabGroupsByWindowId(windowId: String)

    @Query("DELETE FROM tab_groups")
    suspend fun clearAllTabGroups()

    @Query("SELECT * FROM browser_windows ORDER BY createdAt ASC")
    suspend fun getAllWindows(): List<BrowserWindowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWindow(window: BrowserWindowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWindows(windows: List<BrowserWindowEntity>)

    @Query("DELETE FROM browser_windows WHERE id = :windowId")
    suspend fun deleteWindowById(windowId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY folder ASC, sortOrder ASC, createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folder = :folder ORDER BY sortOrder ASC, createdAt DESC")
    fun observeBookmarksByFolder(folder: String): Flow<List<BookmarkEntity>>

    @Query("SELECT DISTINCT folder FROM bookmarks ORDER BY folder ASC")
    fun observeFolders(): Flow<List<String>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastVisitedAt DESC LIMIT 500")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitCount DESC, lastVisitedAt DESC LIMIT :limit")
    fun observeMostVisited(limit: Int = 8): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') AND lastVisitedAt >= :sinceTimestamp ORDER BY lastVisitedAt DESC")
    fun searchHistory(query: String, sinceTimestamp: Long = 0L): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE lastVisitedAt >= :sinceTimestamp")
    suspend fun deleteSince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface HomeShortcutDao {
    @Query("SELECT * FROM home_shortcuts ORDER BY position ASC, id ASC")
    fun observeShortcuts(): Flow<List<HomeShortcutEntity>>

    @Query("SELECT * FROM home_shortcuts ORDER BY position ASC, id ASC")
    suspend fun getAllShortcuts(): List<HomeShortcutEntity>

    @Query("SELECT COUNT(*) FROM home_shortcuts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: HomeShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<HomeShortcutEntity>)

    @Update
    suspend fun update(shortcut: HomeShortcutEntity)

    @Query("DELETE FROM home_shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isApk = 1 ORDER BY createdAt DESC")
    fun observeApkDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}

@Dao
interface ExtensionMetadataDao {
    @Query("SELECT * FROM extensions_metadata ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<ExtensionMetadataEntity>>

    @Query("SELECT * FROM extensions_metadata")
    suspend fun getAll(): List<ExtensionMetadataEntity>

    @Query("SELECT * FROM extensions_metadata WHERE extensionId = :extensionId LIMIT 1")
    suspend fun findById(extensionId: String): ExtensionMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExtensionMetadataEntity)

    @Query("UPDATE extensions_metadata SET enabled = :enabled WHERE extensionId = :extensionId")
    suspend fun updateEnabled(extensionId: String, enabled: Boolean)

    @Query("DELETE FROM extensions_metadata WHERE extensionId = :extensionId")
    suspend fun deleteById(extensionId: String)
}

@Dao
interface SitePermissionDao {
    @Query("SELECT * FROM site_permissions ORDER BY origin ASC, permissionType ASC")
    fun observeAll(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :type LIMIT 1")
    suspend fun findPermission(origin: String, type: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(permission: SitePermissionEntity)

    @Query("UPDATE site_permissions SET granted = :granted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGranted(id: Long, granted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM site_permissions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAll()
}
