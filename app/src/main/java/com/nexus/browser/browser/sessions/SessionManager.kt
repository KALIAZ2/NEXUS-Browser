package com.nexus.browser.browser.sessions

import com.nexus.browser.browser.engine.BrowserEngineManager
import com.nexus.browser.browser.tabs.BrowserTab
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordonne le cycle de vie des sessions GeckoView avec les onglets actifs et inactifs.
 */
@Singleton
class SessionManager @Inject constructor(
    private val engineManager: BrowserEngineManager
) {
    fun obtainGeckoSessionForTab(tab: BrowserTab): GeckoSession {
        return engineManager.getOrCreateSession(
            tabId = tab.id,
            isPrivate = tab.isPrivate,
            initialSessionStateJson = tab.sessionStateJson
        )
    }

    fun releaseSession(tabId: String) {
        engineManager.closeSession(tabId)
    }
}
