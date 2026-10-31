package com.nexus.browser.privacy

import com.nexus.browser.browser.engine.BrowserEngineManager
import com.nexus.browser.browser.permissions.SitePermissionManager
import com.nexus.browser.history.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyManager @Inject constructor(
    private val engineManager: BrowserEngineManager,
    private val historyRepository: HistoryRepository,
    private val permissionManager: SitePermissionManager
) {
    suspend fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearSiteData: Boolean,
        clearPermissions: Boolean
    ) {
        if (clearHistory) {
            historyRepository.clearAll()
        }
        if (clearCookies || clearCache || clearSiteData) {
            engineManager.clearBrowsingData(
                clearCookies = clearCookies,
                clearCache = clearCache,
                clearSiteData = clearSiteData
            )
        }
        if (clearPermissions) {
            permissionManager.clearAllPermissions()
        }
    }

    fun closePrivateBrowsingSessions() {
        engineManager.closeAllPrivateSessions()
    }
}
