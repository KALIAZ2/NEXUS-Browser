package com.nexus.browser.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.browser.browser.permissions.SitePermissionManager
import com.nexus.browser.core.common.SearchEngine
import com.nexus.browser.core.database.SitePermissionEntity
import com.nexus.browser.core.preferences.NexusPreferencesRepository
import com.nexus.browser.core.preferences.NexusSettingsState
import com.nexus.browser.privacy.PrivacyManager
import com.nexus.browser.updates.AppUpdateManager
import com.nexus.browser.updates.AppUpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: NexusPreferencesRepository,
    private val privacyManager: PrivacyManager,
    private val sitePermissionManager: SitePermissionManager,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    val settings: StateFlow<NexusSettingsState> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NexusSettingsState())

    val sitePermissions: StateFlow<List<SitePermissionEntity>> = sitePermissionManager.observePermissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appUpdateState: StateFlow<AppUpdateUiState> = appUpdateManager.uiState

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { preferencesRepository.updateSearchEngine(engine) }
    }

    fun setCustomSearchUrl(template: String) {
        viewModelScope.launch { preferencesRepository.updateCustomSearchUrl(template) }
    }

    fun setHomePageUrl(url: String) {
        viewModelScope.launch { preferencesRepository.updateHomePageUrl(url) }
    }

    fun setRestoreTabsOnStartup(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateRestoreTabsOnStartup(enabled) }
    }

    fun setOpenLinksInNewTab(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateOpenLinksInNewTab(enabled) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch { preferencesRepository.updateAppLanguage(language) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
    }

    fun setAccentHex(hex: String) {
        viewModelScope.launch { preferencesRepository.updateAccentHex(hex) }
    }

    fun setTextScalePercent(percent: Int) {
        viewModelScope.launch { preferencesRepository.updateTextScalePercent(percent) }
    }

    fun setToolbarAtBottom(bottom: Boolean) {
        viewModelScope.launch { preferencesRepository.updateToolbarAtBottom(bottom) }
    }

    fun setHomeSections(shortcuts: Boolean, bookmarks: Boolean, recentHistory: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHomeSections(shortcuts, bookmarks, recentHistory)
        }
    }

    fun setHomeBackgroundStyle(style: String, customUri: String? = null) {
        viewModelScope.launch {
            preferencesRepository.updateHomeBackgroundStyle(style, customUri)
        }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateJavaScriptEnabled(enabled) }
    }

    fun setBlockPopups(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateBlockPopups(enabled) }
    }

    fun setDesktopModeDefault(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateDesktopModeDefault(enabled) }
    }

    fun setSuspendInactiveTabs(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateSuspendInactiveTabs(enabled) }
    }

    fun setAddressBarSuggestions(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAddressBarSuggestions(enabled) }
    }

    fun setFullScreenHideBars(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateFullScreenHideBars(enabled) }
    }

    fun setHttpsOnlyMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateHttpsOnlyMode(enabled) }
    }

    fun setTrackingProtection(level: String) {
        viewModelScope.launch { preferencesRepository.updateTrackingProtection(level) }
    }

    fun setCookiePolicy(policy: String) {
        viewModelScope.launch { preferencesRepository.updateCookiePolicy(policy) }
    }

    fun setClearHistoryOnExit(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateClearHistoryOnExit(enabled) }
    }

    fun setDownloadDirectoryLabel(label: String) {
        viewModelScope.launch { preferencesRepository.updateDownloadDirectoryLabel(label) }
    }

    fun setAskBeforeDownload(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAskBeforeDownload(enabled) }
    }

    fun setAutoOpenDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoOpenDownloads(enabled) }
    }

    fun setDownloadNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateDownloadNotifications(enabled) }
    }

    fun setAutoAnalyzeApks(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoAnalyzeApks(enabled) }
    }

    fun setAllowLocalXpiInstall(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAllowLocalXpiInstall(enabled) }
    }

    fun setAutoUpdateExtensions(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoUpdateExtensions(enabled) }
    }

    fun setAutoCheckAppUpdates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoCheckAppUpdates(enabled) }
    }

    fun checkForAppUpdates(onBanner: (String) -> Unit = {}) {
        appUpdateManager.checkForUpdates(manual = true, onBanner = onBanner)
    }

    fun downloadAndInstallAppUpdate(onBanner: (String) -> Unit = {}) {
        appUpdateManager.downloadAndInstallUpdate(onBanner = onBanner)
    }

    fun addOrUpdateSitePermission(origin: String, permissionType: String, granted: Boolean) {
        viewModelScope.launch {
            sitePermissionManager.setPermission(origin, permissionType, granted)
        }
    }

    fun toggleSitePermission(id: Long, granted: Boolean) {
        viewModelScope.launch {
            sitePermissionManager.togglePermission(id, granted)
        }
    }

    fun deleteSitePermission(id: Long) {
        viewModelScope.launch {
            sitePermissionManager.deletePermission(id)
        }
    }

    fun clearAllSitePermissions() {
        viewModelScope.launch {
            sitePermissionManager.clearAllPermissions()
        }
    }

    fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearSiteData: Boolean,
        clearPermissions: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            privacyManager.clearBrowsingData(
                clearHistory = clearHistory,
                clearCookies = clearCookies,
                clearCache = clearCache,
                clearSiteData = clearSiteData,
                clearPermissions = clearPermissions
            )
            onDone()
        }
    }
}
