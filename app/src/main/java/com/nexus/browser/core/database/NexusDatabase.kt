package com.nexus.browser.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TabEntity::class,
        TabGroupEntity::class,
        BrowserWindowEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        HomeShortcutEntity::class,
        DownloadEntity::class,
        ExtensionMetadataEntity::class,
        SitePermissionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun homeShortcutDao(): HomeShortcutDao
    abstract fun downloadDao(): DownloadDao
    abstract fun extensionMetadataDao(): ExtensionMetadataDao
    abstract fun sitePermissionDao(): SitePermissionDao

    companion object {
        const val DATABASE_NAME = "nexus_browser.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS ai_messages")
                db.execSQL("DELETE FROM extensions_metadata WHERE sourceType = 'AI_GENERATED'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tabs ADD COLUMN windowId TEXT NOT NULL DEFAULT 'window_main'")
                db.execSQL("ALTER TABLE tabs ADD COLUMN groupId TEXT")
                db.execSQL("ALTER TABLE tabs ADD COLUMN previewPath TEXT")
                db.execSQL("ALTER TABLE tabs ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tab_groups (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        windowId TEXT NOT NULL DEFAULT 'window_main',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tab_groups_windowId ON tab_groups(windowId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS browser_windows (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        isPrivate INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_browser_windows_createdAt ON browser_windows(createdAt)")
            }
        }
    }
}
