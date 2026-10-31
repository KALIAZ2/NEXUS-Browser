package com.nexus.browser.browser.engine

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import com.nexus.browser.BuildConfig
import com.nexus.browser.browser.downloads.NexusDownloadManager
import com.nexus.browser.browser.permissions.SitePermissionManager
import com.nexus.browser.core.common.UrlUtils
import com.nexus.browser.core.security.SecurityPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface SessionEventListener {
    fun onUrlChanged(tabId: String, url: String)
    fun onTitleChanged(tabId: String, title: String)
    fun onLoadingStateChanged(tabId: String, isLoading: Boolean)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onCanGoBackChanged(tabId: String, canGoBack: Boolean)
    fun onCanGoForwardChanged(tabId: String, canGoForward: Boolean)
    fun onSecurityChanged(tabId: String, isSecure: Boolean, host: String?)
    fun onFullScreenChanged(tabId: String, isFullScreen: Boolean)
    fun onTrackerBlocked(tabId: String, blockedUri: String)
    fun onSessionStateUpdated(tabId: String, sessionStateJson: String)
    fun onOpenNewTabRequested(sourceTabId: String, targetUrl: String, isPrivate: Boolean)
    fun onLoadError(tabId: String, failingUrl: String, errorDescription: String)
    fun onScrollStateChanged(tabId: String, canScrollToTop: Boolean) {}
}

@Singleton
class BrowserEngineManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val downloadManager: NexusDownloadManager,
    private val permissionManager: SitePermissionManager
) : BrowserEngineContract {

    companion object {
        private const val MAX_OPEN_BACKGROUND_SESSIONS = 5
    }

    override val engineName: String = "Mozilla GeckoView"
    override val engineVersion: String = BuildConfig.GECKOVIEW_VERSION
    override val supportsWebExtensionsXpi: Boolean = true
    override val supportsNativeCrx: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = ConcurrentHashMap<String, GeckoSession>()
    private val sessionPrivacyMap = ConcurrentHashMap<String, Boolean>()
    private val sessionLastUsedMap = ConcurrentHashMap<String, Long>()
    private val sessionCanScrollTopMap = ConcurrentHashMap<String, Boolean>()

    @Volatile
    private var currentActiveTabId: String? = null

    @Volatile
    private var geckoRuntime: GeckoRuntime? = null

    @Volatile
    private var eventListener: SessionEventListener? = null

    fun setSessionEventListener(listener: SessionEventListener) {
        this.eventListener = listener
    }

    @Synchronized
    override fun getOrCreateGeckoRuntime(): GeckoRuntime {
        val existing = geckoRuntime
        if (existing != null) return existing

        val contentBlocking = ContentBlocking.Settings.Builder()
            .antiTracking(
                ContentBlocking.AntiTracking.DEFAULT or
                    ContentBlocking.AntiTracking.STP
            )
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .queryParameterStrippingEnabled(true)
            .queryParameterStrippingPrivateBrowsingEnabled(true)
            .strictSocialTrackingProtection(true)
            .build()

        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .globalPrivacyControlEnabled(true)
            .extensionsProcessEnabled(true)
            .extensionsWebAPIEnabled(true)
            .aboutConfigEnabled(false)
            .contentBlocking(contentBlocking)
            .build()

        val created = GeckoRuntime.create(appContext, runtimeSettings)
        geckoRuntime = created
        return created
    }

    fun getWebExtensionController(): WebExtensionController {
        return getOrCreateGeckoRuntime().webExtensionController
    }

    override fun getOrCreateSession(
        tabId: String,
        isPrivate: Boolean,
        initialSessionStateJson: String?
    ): GeckoSession {
        sessionLastUsedMap[tabId] = System.currentTimeMillis()
        sessions[tabId]?.let { existing ->
            if (!existing.isOpen) {
                existing.open(getOrCreateGeckoRuntime())
            }
            return existing
        }

        evictLeastRecentlyUsedSessionsIfNeeded(keepTabId = tabId)

        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(true)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()

        val session = GeckoSession(settings)
        attachDelegates(tabId, isPrivate, session)
        session.open(getOrCreateGeckoRuntime())

        if (!isPrivate && !initialSessionStateJson.isNullOrBlank()) {
            runCatching {
                val restored = GeckoSession.SessionState.fromString(initialSessionStateJson)
                if (restored != null) {
                    session.restoreState(restored)
                }
            }
        }

        sessions[tabId] = session
        sessionPrivacyMap[tabId] = isPrivate
        return session
    }

    fun hasOpenSession(tabId: String): Boolean {
        return sessions[tabId]?.isOpen == true
    }

    private fun evictLeastRecentlyUsedSessionsIfNeeded(keepTabId: String) {
        if (sessions.size <= MAX_OPEN_BACKGROUND_SESSIONS) return
        val candidates = sessions.keys
            .filter { it != keepTabId && it != currentActiveTabId && sessionPrivacyMap[it] != true }
            .sortedBy { sessionLastUsedMap[it] ?: 0L }

        val toEvictCount = (sessions.size - MAX_OPEN_BACKGROUND_SESSIONS).coerceAtLeast(0)
        candidates.take(toEvictCount).forEach { oldTabId ->
            sessions.remove(oldTabId)?.let { oldSession ->
                runCatching {
                    oldSession.setActive(false)
                    if (oldSession.isOpen) {
                        oldSession.close()
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        ) {
            val activeId = currentActiveTabId
            val backgroundIds = sessions.keys.filter { it != activeId && sessionPrivacyMap[it] != true }
            backgroundIds.forEach { tabId ->
                sessions.remove(tabId)?.let { session ->
                    runCatching {
                        session.setActive(false)
                        if (session.isOpen) {
                            session.close()
                        }
                    }
                }
            }
        }
    }

    private fun attachDelegates(tabId: String, isPrivate: Boolean, session: GeckoSession) {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                val safeUrl = url ?: return
                eventListener?.onUrlChanged(tabId, safeUrl)
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                eventListener?.onCanGoBackChanged(tabId = tabId, canGoBack = canGoBack)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                eventListener?.onCanGoForwardChanged(tabId = tabId, canGoForward = canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny> {
                val targetUri = request.uri
                if (!SecurityPolicy.isSafeNavigationUrl(targetUri)) {
                    Log.w("NexusEngine", "Navigation bloquée vers schéma non autorisé: $targetUri")
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW) {
                    eventListener?.onOpenNewTabRequested(tabId, targetUri, isPrivate)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession> {
                eventListener?.onOpenNewTabRequested(tabId, uri, isPrivate)
                return GeckoResult.fromValue(null)
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                val desc = describeWebError(error)
                eventListener?.onLoadError(tabId, uri ?: "", desc)
                val html = buildErrorPageHtml(uri ?: "URL inconnue", desc)
                return GeckoResult.fromValue("data:text/html;charset=utf-8," + android.net.Uri.encode(html))
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                eventListener?.onLoadingStateChanged(tabId, true)
                eventListener?.onProgressChanged(tabId, 10)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                eventListener?.onLoadingStateChanged(tabId, false)
                eventListener?.onProgressChanged(tabId, 100)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                eventListener?.onProgressChanged(tabId, progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                eventListener?.onSecurityChanged(
                    tabId = tabId,
                    isSecure = securityInfo.isSecure,
                    host = securityInfo.host
                )
            }

            override fun onSessionStateChange(
                session: GeckoSession,
                sessionState: GeckoSession.SessionState
            ) {
                if (!isPrivate) {
                    eventListener?.onSessionStateUpdated(tabId, sessionState.toString())
                }
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (!title.isNullOrBlank()) {
                    eventListener?.onTitleChanged(tabId, title)
                }
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                eventListener?.onFullScreenChanged(tabId, fullScreen)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                downloadManager.handleGeckoExternalResponse(response)
            }

            override fun onCrash(session: GeckoSession) {
                Log.e("NexusEngine", "Processus de contenu GeckoView arrêté pour l'onglet $tabId, réouverture.")
                runCatching { session.open(getOrCreateGeckoRuntime()) }
            }

            override fun onKill(session: GeckoSession) {
                Log.w("NexusEngine", "Processus de contenu GeckoView recyclé pour l'onglet $tabId.")
            }
        }

        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(
                session: GeckoSession,
                event: ContentBlocking.BlockEvent
            ) {
                eventListener?.onTrackerBlocked(tabId, event.uri)
            }
        }

        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                val previous = sessionCanScrollTopMap[tabId] ?: false
                val shouldShow = when {
                    scrollY > 520 -> true
                    scrollY < 160 -> false
                    else -> previous
                }
                if (shouldShow != previous) {
                    sessionCanScrollTopMap[tabId] = shouldShow
                    eventListener?.onScrollStateChanged(tabId, shouldShow)
                }
            }
        }

        session.permissionDelegate = permissionManager
    }

    override fun loadUrl(tabId: String, url: String) {
        if (UrlUtils.isHomeUrl(url)) return
        if (!SecurityPolicy.isSafeNavigationUrl(url)) return
        val isPrivate = sessionPrivacyMap[tabId] ?: false
        val session = getOrCreateSession(tabId, isPrivate)
        session.loadUri(url)
    }

    override fun reload(tabId: String) {
        sessions[tabId]?.reload()
    }

    override fun stopLoading(tabId: String) {
        sessions[tabId]?.stop()
    }

    override fun goBack(tabId: String) {
        sessions[tabId]?.goBack()
    }

    override fun goForward(tabId: String) {
        sessions[tabId]?.goForward()
    }

    override fun setDesktopMode(tabId: String, enabled: Boolean) {
        val session = sessions[tabId] ?: return
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.settings.viewportMode = if (enabled) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        session.reload()
    }

    override fun findInPage(
        tabId: String,
        query: String,
        backwards: Boolean,
        onResult: (current: Int, total: Int, found: Boolean) -> Unit
    ) {
        val session = sessions[tabId]
        val trimmed = query.trim()
        if (session == null || trimmed.isEmpty()) {
            session?.finder?.clear()
            onResult(0, 0, false)
            return
        }
        session.finder.displayFlags = GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL or GeckoSession.FINDER_DISPLAY_DIM_PAGE
        val flags = if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else 0
        session.finder.find(trimmed, flags).accept(
            { result ->
                if (result != null) {
                    onResult(result.current, result.total, result.found)
                } else {
                    onResult(0, 0, false)
                }
            },
            {
                onResult(0, 0, false)
            }
        )
    }

    override fun clearFindInPage(tabId: String) {
        sessions[tabId]?.finder?.clear()
    }

    override fun scrollToTop(tabId: String) {
        val session = sessions[tabId] ?: return
        runCatching {
            session.loadUri("javascript:window.scrollTo({top:0,behavior:'smooth'})")
        }
        sessionCanScrollTopMap[tabId] = false
        eventListener?.onScrollStateChanged(tabId, false)
    }

    override fun exitFullScreen(tabId: String) {
        sessions[tabId]?.exitFullScreen()
    }

    fun setActiveSession(activeTabId: String) {
        currentActiveTabId = activeTabId
        sessionLastUsedMap[activeTabId] = System.currentTimeMillis()
        sessions.forEach { (tabId, session) ->
            val isTarget = tabId == activeTabId
            runCatching {
                session.setActive(isTarget)
                getWebExtensionController().setTabActive(session, isTarget)
            }
        }
        evictLeastRecentlyUsedSessionsIfNeeded(keepTabId = activeTabId)
    }

    override fun closeSession(tabId: String) {
        sessionPrivacyMap.remove(tabId)
        sessionLastUsedMap.remove(tabId)
        sessionCanScrollTopMap.remove(tabId)
        sessions.remove(tabId)?.let { session ->
            runCatching {
                session.setActive(false)
                if (session.isOpen) {
                    session.close()
                }
            }
        }
    }

    override fun closeAllPrivateSessions() {
        val privateTabIds = sessionPrivacyMap.filterValues { it }.keys.toList()
        privateTabIds.forEach { closeSession(it) }
    }

    override fun clearBrowsingData(
        clearCookies: Boolean,
        clearCache: Boolean,
        clearSiteData: Boolean
    ) {
        val runtime = geckoRuntime ?: return
        var flags = 0L
        if (clearCookies) {
            flags = flags or StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.AUTH_SESSIONS
        }
        if (clearCache) {
            flags = flags or StorageController.ClearFlags.ALL_CACHES
        }
        if (clearSiteData) {
            flags = flags or StorageController.ClearFlags.DOM_STORAGES or StorageController.ClearFlags.SITE_DATA
        }
        if (flags != 0L) {
            runtime.storageController.clearData(flags)
        }
    }

    private fun describeWebError(error: WebRequestError): String {
        return when (error.code) {
            WebRequestError.ERROR_SECURITY_SSL,
            WebRequestError.ERROR_SECURITY_BAD_CERT,
            WebRequestError.ERROR_BAD_HSTS_CERT ->
                "Certificat TLS/SSL invalide ou compromis. NEXUS bloque automatiquement les connexions HTTPS non fiables."
            WebRequestError.ERROR_OFFLINE ->
                "Aucune connexion Internet détectée. Vérifiez votre réseau Wi-Fi ou vos données mobiles."
            WebRequestError.ERROR_NET_TIMEOUT ->
                "Le serveur a mis trop de temps à répondre (délai d'attente dépassé)."
            WebRequestError.ERROR_UNKNOWN_HOST ->
                "Adresse introuvable : impossible de résoudre le nom de domaine."
            WebRequestError.ERROR_CONNECTION_REFUSED ->
                "Le serveur distant a refusé la connexion."
            WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI,
            WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI,
            WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI ->
                "Alerte de sécurité : ce site est signalé comme dangereux (hameçonnage ou logiciel malveillant)."
            else ->
                "Impossible de charger la page (Code erreur GeckoView: ${error.code})."
        }
    }

    private fun buildErrorPageHtml(url: String, message: String): String {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>Connexion interrompue — NEXUS Browser</title>
              <style>
                :root {
                  --bg: #050816;
                  --card: #0D152C;
                  --border: #1E2D52;
                  --text: #F5F8FF;
                  --muted: #94A3B8;
                  --url-bg: #080E20;
                  --accent: #2979FF;
                  --danger: #FF5964;
                }
                @media (prefers-color-scheme: light) {
                  :root {
                    --bg: #F5F7FC;
                    --card: #FFFFFF;
                    --border: #D8E2F0;
                    --text: #0F172A;
                    --muted: #64748B;
                    --url-bg: #EDF2FA;
                    --accent: #2563EB;
                    --danger: #E11D48;
                  }
                }
                body {
                  background: var(--bg);
                  color: var(--text);
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  padding: 28px 18px;
                  margin: 0;
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  justify-content: center;
                  min-height: 82vh;
                }
                .card {
                  background: var(--card);
                  border: 1px solid var(--border);
                  border-radius: 20px;
                  padding: 26px 22px;
                  max-width: 440px;
                  width: 100%;
                  box-sizing: border-box;
                  box-shadow: 0 12px 32px rgba(0,0,0,0.22);
                }
                .badge {
                  display: inline-block;
                  background: rgba(255, 89, 100, 0.14);
                  color: var(--danger);
                  font-weight: 700;
                  font-size: 11px;
                  letter-spacing: 0.6px;
                  padding: 5px 11px;
                  border-radius: 99px;
                  margin-bottom: 14px;
                }
                h1 { font-size: 20px; margin: 0 0 10px 0; font-weight: 700; }
                p { color: var(--muted); font-size: 14px; line-height: 1.55; margin: 0 0 16px 0; }
                .url {
                  font-family: monospace;
                  font-size: 12px;
                  color: var(--accent);
                  word-break: break-all;
                  background: var(--url-bg);
                  border: 1px solid var(--border);
                  padding: 10px 12px;
                  border-radius: 12px;
                  margin-bottom: 18px;
                }
                .actions {
                  display: flex;
                  gap: 10px;
                }
                button {
                  background: linear-gradient(135deg, #00E5FF 0%, #2563EB 55%, #7C3AED 100%);
                  color: #FFFFFF;
                  border: none;
                  border-radius: 12px;
                  padding: 13px 18px;
                  font-weight: 600;
                  width: 100%;
                  font-size: 14px;
                  cursor: pointer;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <span class="badge">DIAGNOSTIC RÉSEAU NEXUS</span>
                <h1>Impossible d'ouvrir cette page</h1>
                <p>$message</p>
                <div class="url">$url</div>
                <div class="actions">
                  <button onclick="window.location.reload()">Réessayer maintenant</button>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
