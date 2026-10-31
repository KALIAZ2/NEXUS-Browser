package com.nexus.browser.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNexusDatabase(
        @ApplicationContext context: Context
    ): NexusDatabase {
        return Room.databaseBuilder(
            context,
            NexusDatabase::class.java,
            NexusDatabase.DATABASE_NAME
        )
            .addMigrations(NexusDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTabDao(db: NexusDatabase): TabDao = db.tabDao()

    @Provides
    fun provideBookmarkDao(db: NexusDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideHistoryDao(db: NexusDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideHomeShortcutDao(db: NexusDatabase): HomeShortcutDao = db.homeShortcutDao()

    @Provides
    fun provideDownloadDao(db: NexusDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideExtensionMetadataDao(db: NexusDatabase): ExtensionMetadataDao = db.extensionMetadataDao()

    @Provides
    fun provideSitePermissionDao(db: NexusDatabase): SitePermissionDao = db.sitePermissionDao()
}
