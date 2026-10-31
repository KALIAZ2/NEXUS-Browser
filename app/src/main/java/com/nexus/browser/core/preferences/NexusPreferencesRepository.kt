package com.nexus.browser.core.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.browser.BuildConfig
import com.nexus.browser.core.common.SearchEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nexusDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings_prefs")

@Immutable
data class NexusSettingsState(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customSearchUrlTemplate: String = "https://www.google.com/search?q=%s",
    val homePageUrl: String = "nexus://home",
    val restoreTabsOnStartup: Boolean = true,
    val openLinksInNewTab: Boolean = false,
    val appLanguage: String = "FR", // FR, EN, SYSTEM
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val accentHex: String = "#5685FF",
    val textScalePercent: Int = 100,
    val toolbarAtBottom: Boolean = false,
    val showHomeShortcuts: Boolean = true,
    val showHomeBookmarks: Boolean = true,
    val showHomeRecentHistory: Boolean = false,
    val homeBackgroundStyle: String = "COSMIC", // COSMIC, MINIMAL, AURORA, CUSTOM
    val homeCustomBackgroundUri: String = "",
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val suspendInactiveTabs: Boolean = true,
    val addressBarSuggestions: Boolean = true,
    val fullScreenHideBars: Boolean = true,
    val httpsOnlyMode: Boolean = true,
    val trackingProtectionLevel: String = "STRICT", // STANDARD, STRICT
    val cookiePolicy: String = "BLOCK_THIRD_PARTY", // ACCEPT_ALL, BLOCK_THIRD_PARTY, BLOCK_ALL
    val clearHistoryOnExit: Boolean = false,
    val downloadDirectoryLabel: String = "Téléchargements (Public)",
    val askBeforeDownload: Boolean = false,
    val autoOpenCompletedDownloads: Boolean = false,
    val downloadNotificationsEnabled: Boolean = true,
    val autoAnalyzeDownloadedApks: Boolean = true,
    val allowLocalXpiInstall: Boolean = true,
    val autoUpdateExtensions: Boolean = true,
    val autoCheckAppUpdates: Boolean = true,
    val githubOwner: String = BuildConfig.GITHUB_OWNER,
    val githubRepo: String = BuildConfig.GITHUB_REPO,
    val lastUpdateCheckEpochMs: Long = 0L,
    val cachedLatestVersionName: String = BuildConfig.VERSION_NAME,
    val cachedReleaseNotes: String = "",
    val cachedApkDownloadUrl: String = ""
)

@Singleton
class NexusPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")
        val HOME_PAGE_URL = stringPreferencesKey("home_page_url")
        val RESTORE_TABS = booleanPreferencesKey("restore_tabs_on_startup")
        val OPEN_LINKS_NEW_TAB = booleanPreferencesKey("open_links_in_new_tab")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_HEX = stringPreferencesKey("accent_hex")
        val TEXT_SCALE_PERCENT = intPreferencesKey("text_scale_percent")
        val TOOLBAR_AT_BOTTOM = booleanPreferencesKey("toolbar_at_bottom")
        val SHOW_HOME_SHORTCUTS = booleanPreferencesKey("show_home_shortcuts")
        val SHOW_HOME_BOOKMARKS = booleanPreferencesKey("show_home_bookmarks")
        val SHOW_HOME_RECENT_HISTORY = booleanPreferencesKey("show_home_recent_history")
        val HOME_BACKGROUND_STYLE = stringPreferencesKey("home_background_style")
        val HOME_CUSTOM_BG_URI = stringPreferencesKey("home_custom_bg_uri")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val SUSPEND_INACTIVE_TABS = booleanPreferencesKey("suspend_inactive_tabs")
        val ADDRESS_BAR_SUGGESTIONS = booleanPreferencesKey("address_bar_suggestions")
        val FULLSCREEN_HIDE_BARS = booleanPreferencesKey("fullscreen_hide_bars")
        val HTTPS_ONLY_MODE = booleanPreferencesKey("https_only_mode")
        val TRACKING_PROTECTION = stringPreferencesKey("tracking_protection_level")
        val COOKIE_POLICY = stringPreferencesKey("cookie_policy")
        val CLEAR_HISTORY_ON_EXIT = booleanPreferencesKey("clear_history_on_exit")
        val DOWNLOAD_DIRECTORY_LABEL = stringPreferencesKey("download_directory_label")
        val ASK_BEFORE_DOWNLOAD = booleanPreferencesKey("ask_before_download")
        val AUTO_OPEN_DOWNLOADS = booleanPreferencesKey("auto_open_completed_downloads")
        val DOWNLOAD_NOTIFICATIONS = booleanPreferencesKey("download_notifications_enabled")
        val AUTO_ANALYZE_APKS = booleanPreferencesKey("auto_analyze_downloaded_apks")
        val ALLOW_LOCAL_XPI_INSTALL = booleanPreferencesKey("allow_local_xpi_install")
        val AUTO_UPDATE_EXTENSIONS = booleanPreferencesKey("auto_update_extensions")
        val AUTO_CHECK_APP_UPDATES = booleanPreferencesKey("auto_check_app_updates")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val LAST_UPDATE_CHECK_MS = longPreferencesKey("last_update_check_ms")
        val CACHED_LATEST_VERSION = stringPreferencesKey("cached_latest_version")
        val CACHED_RELEASE_NOTES = stringPreferencesKey("cached_release_notes")
        val CACHED_APK_DOWNLOAD_URL = stringPreferencesKey("cached_apk_download_url")
    }

    val settingsFlow: Flow<NexusSettingsState> = context.nexusDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            NexusSettingsState(
                searchEngine = SearchEngine.fromId(prefs[Keys.SEARCH_ENGINE] ?: SearchEngine.GOOGLE.id),
                customSearchUrlTemplate = prefs[Keys.CUSTOM_SEARCH_URL] ?: "https://www.google.com/search?q=%s",
                homePageUrl = prefs[Keys.HOME_PAGE_URL] ?: "nexus://home",
                restoreTabsOnStartup = prefs[Keys.RESTORE_TABS] ?: true,
                openLinksInNewTab = prefs[Keys.OPEN_LINKS_NEW_TAB] ?: false,
                appLanguage = prefs[Keys.APP_LANGUAGE] ?: "FR",
                themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
                accentHex = prefs[Keys.ACCENT_HEX] ?: "#5685FF",
                textScalePercent = (prefs[Keys.TEXT_SCALE_PERCENT] ?: 100).coerceIn(80, 140),
                toolbarAtBottom = prefs[Keys.TOOLBAR_AT_BOTTOM] ?: false,
                showHomeShortcuts = prefs[Keys.SHOW_HOME_SHORTCUTS] ?: true,
                showHomeBookmarks = prefs[Keys.SHOW_HOME_BOOKMARKS] ?: true,
                showHomeRecentHistory = prefs[Keys.SHOW_HOME_RECENT_HISTORY] ?: false,
                homeBackgroundStyle = prefs[Keys.HOME_BACKGROUND_STYLE] ?: "COSMIC",
                homeCustomBackgroundUri = prefs[Keys.HOME_CUSTOM_BG_URI] ?: "",
                javaScriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
                blockPopups = prefs[Keys.BLOCK_POPUPS] ?: true,
                desktopModeDefault = prefs[Keys.DESKTOP_MODE_DEFAULT] ?: false,
                suspendInactiveTabs = prefs[Keys.SUSPEND_INACTIVE_TABS] ?: true,
                addressBarSuggestions = prefs[Keys.ADDRESS_BAR_SUGGESTIONS] ?: true,
                fullScreenHideBars = prefs[Keys.FULLSCREEN_HIDE_BARS] ?: true,
                httpsOnlyMode = prefs[Keys.HTTPS_ONLY_MODE] ?: true,
                trackingProtectionLevel = prefs[Keys.TRACKING_PROTECTION] ?: "STRICT",
                cookiePolicy = prefs[Keys.COOKIE_POLICY] ?: "BLOCK_THIRD_PARTY",
                clearHistoryOnExit = prefs[Keys.CLEAR_HISTORY_ON_EXIT] ?: false,
                downloadDirectoryLabel = prefs[Keys.DOWNLOAD_DIRECTORY_LABEL] ?: "Téléchargements (Public)",
                askBeforeDownload = prefs[Keys.ASK_BEFORE_DOWNLOAD] ?: false,
                autoOpenCompletedDownloads = prefs[Keys.AUTO_OPEN_DOWNLOADS] ?: false,
                downloadNotificationsEnabled = prefs[Keys.DOWNLOAD_NOTIFICATIONS] ?: true,
                autoAnalyzeDownloadedApks = prefs[Keys.AUTO_ANALYZE_APKS] ?: true,
                allowLocalXpiInstall = prefs[Keys.ALLOW_LOCAL_XPI_INSTALL] ?: true,
                autoUpdateExtensions = prefs[Keys.AUTO_UPDATE_EXTENSIONS] ?: true,
                autoCheckAppUpdates = prefs[Keys.AUTO_CHECK_APP_UPDATES] ?: true,
                githubOwner = prefs[Keys.GITHUB_OWNER]?.takeIf { it.isNotBlank() } ?: BuildConfig.GITHUB_OWNER,
                githubRepo = prefs[Keys.GITHUB_REPO]?.takeIf { it.isNotBlank() } ?: BuildConfig.GITHUB_REPO,
                lastUpdateCheckEpochMs = prefs[Keys.LAST_UPDATE_CHECK_MS] ?: 0L,
                cachedLatestVersionName = prefs[Keys.CACHED_LATEST_VERSION]?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME,
                cachedReleaseNotes = prefs[Keys.CACHED_RELEASE_NOTES] ?: "",
                cachedApkDownloadUrl = prefs[Keys.CACHED_APK_DOWNLOAD_URL] ?: ""
            )
        }

    suspend fun updateSearchEngine(engine: SearchEngine) {
        context.nexusDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.id }
    }

    suspend fun updateCustomSearchUrl(template: String) {
        context.nexusDataStore.edit { it[Keys.CUSTOM_SEARCH_URL] = template.trim() }
    }

    suspend fun updateHomePageUrl(url: String) {
        context.nexusDataStore.edit { it[Keys.HOME_PAGE_URL] = url.trim().ifEmpty { "nexus://home" } }
    }

    suspend fun updateRestoreTabsOnStartup(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.RESTORE_TABS] = enabled }
    }

    suspend fun updateOpenLinksInNewTab(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.OPEN_LINKS_NEW_TAB] = enabled }
    }

    suspend fun updateAppLanguage(language: String) {
        context.nexusDataStore.edit { it[Keys.APP_LANGUAGE] = language }
    }

    suspend fun updateThemeMode(mode: String) {
        context.nexusDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun updateAccentHex(hex: String) {
        context.nexusDataStore.edit { it[Keys.ACCENT_HEX] = hex }
    }

    suspend fun updateTextScalePercent(percent: Int) {
        context.nexusDataStore.edit { it[Keys.TEXT_SCALE_PERCENT] = percent.coerceIn(80, 140) }
    }

    suspend fun updateToolbarAtBottom(bottom: Boolean) {
        context.nexusDataStore.edit { it[Keys.TOOLBAR_AT_BOTTOM] = bottom }
    }

    suspend fun updateHomeSections(shortcuts: Boolean, bookmarks: Boolean, recentHistory: Boolean) {
        context.nexusDataStore.edit {
            it[Keys.SHOW_HOME_SHORTCUTS] = shortcuts
            it[Keys.SHOW_HOME_BOOKMARKS] = bookmarks
            it[Keys.SHOW_HOME_RECENT_HISTORY] = recentHistory
        }
    }

    suspend fun updateHomeBackgroundStyle(style: String, customUri: String? = null) {
        context.nexusDataStore.edit {
            it[Keys.HOME_BACKGROUND_STYLE] = style
            if (customUri != null) {
                it[Keys.HOME_CUSTOM_BG_URI] = customUri
            }
        }
    }

    suspend fun updateJavaScriptEnabled(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun updateBlockPopups(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.BLOCK_POPUPS] = enabled }
    }

    suspend fun updateDesktopModeDefault(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.DESKTOP_MODE_DEFAULT] = enabled }
    }

    suspend fun updateSuspendInactiveTabs(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.SUSPEND_INACTIVE_TABS] = enabled }
    }

    suspend fun updateAddressBarSuggestions(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.ADDRESS_BAR_SUGGESTIONS] = enabled }
    }

    suspend fun updateFullScreenHideBars(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.FULLSCREEN_HIDE_BARS] = enabled }
    }

    suspend fun updateHttpsOnlyMode(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.HTTPS_ONLY_MODE] = enabled }
    }

    suspend fun updateTrackingProtection(level: String) {
        context.nexusDataStore.edit { it[Keys.TRACKING_PROTECTION] = level }
    }

    suspend fun updateCookiePolicy(policy: String) {
        context.nexusDataStore.edit { it[Keys.COOKIE_POLICY] = policy }
    }

    suspend fun updateClearHistoryOnExit(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.CLEAR_HISTORY_ON_EXIT] = enabled }
    }

    suspend fun updateDownloadDirectoryLabel(label: String) {
        context.nexusDataStore.edit { it[Keys.DOWNLOAD_DIRECTORY_LABEL] = label }
    }

    suspend fun updateAskBeforeDownload(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.ASK_BEFORE_DOWNLOAD] = enabled }
    }

    suspend fun updateAutoOpenDownloads(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_OPEN_DOWNLOADS] = enabled }
    }

    suspend fun updateDownloadNotifications(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.DOWNLOAD_NOTIFICATIONS] = enabled }
    }

    suspend fun updateAutoAnalyzeApks(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_ANALYZE_APKS] = enabled }
    }

    suspend fun updateAllowLocalXpiInstall(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.ALLOW_LOCAL_XPI_INSTALL] = enabled }
    }

    suspend fun updateAutoUpdateExtensions(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_UPDATE_EXTENSIONS] = enabled }
    }

    suspend fun updateAutoCheckAppUpdates(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_CHECK_APP_UPDATES] = enabled }
    }

    suspend fun updateGitHubRepository(owner: String, repo: String) {
        context.nexusDataStore.edit {
            it[Keys.GITHUB_OWNER] = owner.trim().ifEmpty { BuildConfig.GITHUB_OWNER }
            it[Keys.GITHUB_REPO] = repo.trim().ifEmpty { BuildConfig.GITHUB_REPO }
        }
    }

    suspend fun saveUpdateCheckResult(
        checkedAtMs: Long,
        latestVersionName: String,
        releaseNotes: String,
        apkDownloadUrl: String
    ) {
        context.nexusDataStore.edit {
            it[Keys.LAST_UPDATE_CHECK_MS] = checkedAtMs
            it[Keys.CACHED_LATEST_VERSION] = latestVersionName
            it[Keys.CACHED_RELEASE_NOTES] = releaseNotes
            it[Keys.CACHED_APK_DOWNLOAD_URL] = apkDownloadUrl
        }
    }
}
