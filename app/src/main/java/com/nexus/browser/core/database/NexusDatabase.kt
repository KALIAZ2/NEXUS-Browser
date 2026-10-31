package com.nexus.browser.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TabEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        HomeShortcutEntity::class,
        DownloadEntity::class,
        ExtensionMetadataEntity::class,
        SitePermissionEntity::class
    ],
    version = 2,
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
    }
}
