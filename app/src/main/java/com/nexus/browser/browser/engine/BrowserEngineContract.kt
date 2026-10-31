package com.nexus.browser.browser.engine

import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

/**
 * Interface d'abstraction du moteur de navigation.
 * Permet d'isoler GeckoView (moteur principal par défaut) et garantit une gestion
 * optimale des sessions, de la mémoire et des WebExtensions.
 */
interface BrowserEngineContract {
    val engineName: String
    val engineVersion: String
    val supportsWebExtensionsXpi: Boolean
    val supportsNativeCrx: Boolean

    fun getOrCreateGeckoRuntime(): GeckoRuntime
    fun getOrCreateSession(tabId: String, isPrivate: Boolean, initialSessionStateJson: String? = null): GeckoSession
    fun loadUrl(tabId: String, url: String)
    fun reload(tabId: String)
    fun stopLoading(tabId: String)
    fun goBack(tabId: String)
    fun goForward(tabId: String)
    fun setDesktopMode(tabId: String, enabled: Boolean)
    fun findInPage(tabId: String, query: String, backwards: Boolean, onResult: (current: Int, total: Int, found: Boolean) -> Unit)
    fun clearFindInPage(tabId: String)
    fun scrollToTop(tabId: String)
    fun exitFullScreen(tabId: String)
    fun closeSession(tabId: String)
    fun closeAllPrivateSessions()
    fun onTrimMemory(level: Int)
    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearSiteData: Boolean)
}
