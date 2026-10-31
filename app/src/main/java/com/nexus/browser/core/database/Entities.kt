package com.nexus.browser.core.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "tabs",
    indices = [Index(value = ["position"])]
)
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val isPrivate: Boolean = false,
    val isActive: Boolean = false,
    val position: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val sessionStateJson: String? = null
)

@Immutable
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true), Index(value = ["folder"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val folder: String = "Général",
    val faviconUrl: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "history",
    indices = [Index(value = ["url"], unique = true), Index(value = ["lastVisitedAt"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val visitCount: Int = 1,
    val lastVisitedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "home_shortcuts",
    indices = [Index(value = ["position"])]
)
data class HomeShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val iconColorHex: String = "#5685FF",
    val position: Int = 0
)

@Immutable
@Entity(
    tableName = "downloads",
    indices = [Index(value = ["createdAt"])]
)
data class DownloadEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val sourceUrl: String,
    val mimeType: String,
    val filePath: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: String = STATUS_RUNNING, // RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val isApk: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

@Immutable
@Entity(
    tableName = "extensions_metadata",
    indices = [Index(value = ["extensionId"], unique = true)]
)
data class ExtensionMetadataEntity(
    @PrimaryKey val extensionId: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val enabled: Boolean = true,
    val sourceType: String, // BUILT_IN, LOCAL_XPI, STORE
    val localPathOrUri: String,
    val manifestVersion: Int = 2,
    val permissionsCsv: String = "",
    val compatibilityLevel: String = "COMPATIBLE",
    val installedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "site_permissions",
    indices = [Index(value = ["origin", "permissionType"], unique = true)]
)
data class SitePermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val origin: String,
    val permissionType: String,
    val granted: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
