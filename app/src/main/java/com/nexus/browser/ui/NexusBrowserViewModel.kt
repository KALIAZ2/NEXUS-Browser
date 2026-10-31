package com.nexus.browser.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.browser.apk.manager.NexusAPKManager
import com.nexus.browser.apk.scanner.ApkMetadata
import com.nexus.browser.bookmarks.BookmarkRepository
import com.nexus.browser.browser.downloads.NexusDownloadManager
import com.nexus.browser.browser.engine.BrowserEngineManager
import com.nexus.browser.browser.engine.SessionEventListener
import com.nexus.browser.browser.permissions.SitePermissionManager
import com.nexus.browser.browser.tabs.BrowserTab
import com.nexus.browser.browser.tabs.BrowserWindow
import com.nexus.browser.browser.tabs.ClosedBrowserWindow
import com.nexus.browser.browser.tabs.ClosedTabGroup
import com.nexus.browser.browser.tabs.TabGroup
import com.nexus.browser.browser.tabs.TabRepository
import com.nexus.browser.browser.toolbar.AddressSuggestion
import com.nexus.browser.browser.toolbar.AddressSuggestionProvider
import com.nexus.browser.browser.tools.PageCaptureManager
import com.nexus.browser.browser.tools.PageTranslator
import com.nexus.browser.browser.tools.ReaderModeArticle
import com.nexus.browser.browser.tools.ReaderModeExtractor
import com.nexus.browser.browser.tools.ReaderModeResult
import com.nexus.browser.core.common.SearchEngine
import com.nexus.browser.core.common.UrlUtils
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.DownloadEntity
import com.nexus.browser.core.database.ExtensionMetadataEntity
import com.nexus.browser.core.database.HistoryEntity
import com.nexus.browser.core.database.HomeShortcutEntity
import com.nexus.browser.core.database.SitePermissionEntity
import com.nexus.browser.core.preferences.NexusPreferencesRepository
import com.nexus.browser.core.preferences.NexusSettingsState
import com.nexus.browser.core.security.SecurityPolicy
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityReport
import com.nexus.browser.extensions.manager.NexusExtensionManager
import com.nexus.browser.extensions.store.ExtensionStoreCategory
import com.nexus.browser.extensions.store.ExtensionStoreRepository
import com.nexus.browser.extensions.store.StoreExtensionItem
import com.nexus.browser.history.HistoryRepository
import com.nexus.browser.updates.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import javax.inject.Inject

@Immutable
data class FindInPageUiState(
    val isVisible: Boolean = false,
    val query: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val found: Boolean = false
)

@Immutable
data class ReaderModeUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val article: ReaderModeArticle? = null,
    val errorMessage: String? = null,
    val fontSizeSp: Int = 18,
    val theme: String = "DARK" // DARK, LIGHT, SEPIA
)

@HiltViewModel
class NexusBrowserViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val engineManager: BrowserEngineManager,
    private val preferencesRepository: NexusPreferencesRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val suggestionProvider: AddressSuggestionProvider,
    private val downloadManager: NexusDownloadManager,
    private val apkManager: NexusAPKManager,
    private val extensionManager: NexusExtensionManager,
    private val extensionStoreRepository: ExtensionStoreRepository,
    private val sitePermissionManager: SitePermissionManager,
    private val readerModeExtractor: ReaderModeExtractor,
    private val pageCaptureManager: PageCaptureManager,
    private val appUpdateManager: AppUpdateManager
) : ViewModel(), SessionEventListener {

    val settings: StateFlow<NexusSettingsState> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, NexusSettingsState())

    val tabs: StateFlow<List<BrowserTab>> = tabRepository.tabs
    val activeTabId: StateFlow<String?> = tabRepository.activeTabId
    val recentlyClosedTabs: StateFlow<List<BrowserTab>> = tabRepository.recentlyClosedTabs

    // Gestion multi-fenêtres et groupes d'onglets
    val windows: StateFlow<List<BrowserWindow>> = tabRepository.windows
    val activeWindowId: StateFlow<String> = tabRepository.activeWindowId
    val tabGroups: StateFlow<List<TabGroup>> = tabRepository.tabGroups
    val closedTabGroups: StateFlow<List<ClosedTabGroup>> = tabRepository.closedTabGroups
    val closedWindows: StateFlow<List<ClosedBrowserWindow>> = tabRepository.closedWindows

    // Bannière interactive d'annulation (Undo) pour onglet / groupe fermé
    private val _undoBannerMessage = MutableStateFlow<String?>(null)
    val undoBannerMessage: StateFlow<String?> = _undoBannerMessage.asStateFlow()
    private var lastClosedAction: (() -> Unit)? = null

    val activeTab: StateFlow<BrowserTab?> = combine(tabs, activeTabId) { list, activeId ->
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isBrowserFullScreen = MutableStateFlow(false)
    val isBrowserFullScreen: StateFlow<Boolean> = _isBrowserFullScreen.asStateFlow()

    private val _findInPageState = MutableStateFlow(FindInPageUiState())
    val findInPageState: StateFlow<FindInPageUiState> = _findInPageState.asStateFlow()

    private val _readerModeState = MutableStateFlow(ReaderModeUiState())
    val readerModeState: StateFlow<ReaderModeUiState> = _readerModeState.asStateFlow()

    private val _addressSuggestions = MutableStateFlow<List<AddressSuggestion>>(emptyList())
    val addressSuggestions: StateFlow<List<AddressSuggestion>> = _addressSuggestions.asStateFlow()

    val homeShortcuts: StateFlow<List<HomeShortcutEntity>> = bookmarkRepository.shortcutsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.allBookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkFolders: StateFlow<List<String>> = bookmarkRepository.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Général"))

    val history: StateFlow<List<HistoryEntity>> = historyRepository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = downloadManager.downloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedApks: StateFlow<List<ApkMetadata>> = apkManager.detectedApksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedExtensions: StateFlow<List<ExtensionMetadataEntity>> = extensionManager.installedExtensionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastExtensionCompatibilityReport: StateFlow<ExtensionCompatibilityReport?> = extensionManager.lastCompatibilityReport
    val extensionStatusMessage: StateFlow<String?> = extensionManager.statusMessage

    private val _storeItems = MutableStateFlow<List<StoreExtensionItem>>(extensionStoreRepository.getCuratedCatalog())
    val storeItems: StateFlow<List<StoreExtensionItem>> = _storeItems.asStateFlow()

    private val _isSearchingStoreOnline = MutableStateFlow(false)
    val isSearchingStoreOnline: StateFlow<Boolean> = _isSearchingStoreOnline.asStateFlow()

    val sitePermissions: StateFlow<List<SitePermissionEntity>> = sitePermissionManager.observePermissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiBannerMessage = MutableStateFlow<String?>(null)
    val uiBannerMessage: StateFlow<String?> = _uiBannerMessage.asStateFlow()

    init {
        engineManager.setSessionEventListener(this)
        viewModelScope.launch {
            bookmarkRepository.ensureDefaultShortcutsSeeded()
            val initialSettings = preferencesRepository.settingsFlow.first()
            tabRepository.initializeTabs(
                restoreOnStartup = initialSettings.restoreTabsOnStartup,
                defaultHomeUrl = initialSettings.homePageUrl
            )
            appUpdateManager.checkForUpdatesOnStartupIfDue { bannerMsg ->
                _uiBannerMessage.value = bannerMsg
            }
        }
    }

    fun showBanner(message: String) {
        _uiBannerMessage.value = message
    }

    fun dismissBanner() {
        _uiBannerMessage.value = null
        extensionManager.clearStatusMessage()
    }

    fun obtainGeckoSession(tab: BrowserTab): GeckoSession {
        val session = engineManager.getOrCreateSession(
            tabId = tab.id,
            isPrivate = tab.isPrivate,
            initialSessionStateJson = tab.sessionStateJson
        )
        engineManager.setActiveSession(tab.id)
        if (!UrlUtils.isHomeUrl(tab.url) && tab.progress == 0 && !tab.isLoading) {
            engineManager.loadUrl(tab.id, tab.url)
        }
        return session
    }

    fun onAddressQueryChanged(query: String) {
        if (!settings.value.addressBarSuggestions) {
            _addressSuggestions.value = emptyList()
        } else {
            _addressSuggestions.value = suggestionProvider.getSuggestions(
                rawQuery = query,
                searchEngine = settings.value.searchEngine,
                bookmarks = bookmarks.value,
                history = history.value,
                recentSearches = settings.value.recentSearches
            )
        }
    }

    fun onAddressBarFocused(currentText: String) {
        if (!settings.value.addressBarSuggestions) {
            _addressSuggestions.value = emptyList()
        } else {
            _addressSuggestions.value = suggestionProvider.getSuggestions(
                rawQuery = currentText,
                searchEngine = settings.value.searchEngine,
                bookmarks = bookmarks.value,
                history = history.value,
                recentSearches = settings.value.recentSearches
            )
        }
    }

    fun navigateToInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        val currentSettings = settings.value
        val isLikely = UrlUtils.isLikelyUrl(trimmed)

        // Enregistrer dans les recherches récentes si ce n'est pas une URL pure et que l'onglet n'est pas privé
        val isPriv = activeTab.value?.isPrivate == true
        if (!isLikely && !isPriv) {
            viewModelScope.launch {
                preferencesRepository.recordRecentSearch(trimmed)
            }
        }

        val targetUrl = UrlUtils.normalizeInputToUrlOrSearch(
            rawInput = trimmed,
            searchEngine = currentSettings.searchEngine,
            httpsOnlyMode = currentSettings.httpsOnlyMode
        )

        val currentTab = activeTab.value ?: tabRepository.createTab(
            url = targetUrl,
            isPrivate = false,
            select = true,
            isDesktopMode = currentSettings.desktopModeDefault
        )

        _readerModeState.value = _readerModeState.value.copy(isVisible = false, errorMessage = null)
        if (_findInPageState.value.isVisible) {
            closeFindInPage()
        }

        tabRepository.updateTab(currentTab.id) {
            it.copy(
                url = targetUrl,
                title = if (UrlUtils.isHomeUrl(targetUrl)) "Nouvel onglet" else UrlUtils.extractHost(targetUrl),
                errorDescription = null,
                lastWebUrlBeforeHome = null,
                canScrollToTop = false
            )
        }

        _addressSuggestions.value = emptyList()

        if (!UrlUtils.isHomeUrl(targetUrl)) {
            engineManager.loadUrl(currentTab.id, targetUrl)
        }
    }

    fun goHome() {
        val homeUrl = settings.value.homePageUrl
        val tab = activeTab.value ?: return
        _readerModeState.value = _readerModeState.value.copy(isVisible = false, errorMessage = null)
        if (_findInPageState.value.isVisible) {
            closeFindInPage()
        }

        // Sauvegarder la dernière URL web avant de revenir à l'accueil pour permettre un "Avancer" fluide
        val previousWebUrl = tab.url.takeIf { !UrlUtils.isHomeUrl(it) }

        tabRepository.updateTab(tab.id) {
            it.copy(
                url = homeUrl,
                title = "Nouvel onglet",
                isLoading = false,
                progress = 100,
                errorDescription = null,
                lastWebUrlBeforeHome = previousWebUrl,
                canScrollToTop = false
            )
        }
        if (!UrlUtils.isHomeUrl(homeUrl)) {
            engineManager.loadUrl(tab.id, homeUrl)
        }
    }

    fun goBack(): Boolean {
        if (_readerModeState.value.isVisible) {
            closeReaderMode()
            return true
        }
        if (_findInPageState.value.isVisible) {
            closeFindInPage()
            return true
        }
        val tab = activeTab.value ?: return false
        if (tab.isFullScreen || _isBrowserFullScreen.value) {
            exitAnyFullScreen()
            return true
        }
        if (tab.canGoBack) {
            engineManager.goBack(tab.id)
            return true
        }
        if (!UrlUtils.isHomeUrl(tab.url)) {
            goHome()
            return true
        }
        return false
    }

    fun goForward() {
        val tab = activeTab.value ?: return
        // Si l'utilisateur est sur l'accueil et avait précédemment une page web, rouvrir cette page
        if (tab.isHomeTab && !tab.lastWebUrlBeforeHome.isNullOrBlank()) {
            val target = tab.lastWebUrlBeforeHome
            tabRepository.updateTab(tab.id) {
                it.copy(
                    url = target,
                    title = UrlUtils.extractHost(target),
                    lastWebUrlBeforeHome = null
                )
            }
            engineManager.loadUrl(tab.id, target)
            return
        }

        if (tab.canGoForward) {
            engineManager.goForward(tab.id)
        }
    }

    fun scrollToTop() {
        val tab = activeTab.value ?: return
        engineManager.scrollToTop(tab.id)
    }

    fun reloadOrStop() {
        val tab = activeTab.value ?: return
        if (UrlUtils.isHomeUrl(tab.url)) return
        if (tab.isLoading) {
            engineManager.stopLoading(tab.id)
        } else {
            engineManager.reload(tab.id)
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value ?: return
        val next = !tab.isDesktopMode
        tabRepository.updateTab(tab.id) { it.copy(isDesktopMode = next) }
        engineManager.setDesktopMode(tab.id, next)
    }

    // --- FullScreen Management ---

    fun setBrowserFullScreen(enabled: Boolean) {
        _isBrowserFullScreen.value = enabled
        if (enabled) {
            showBanner("Mode plein écran activé • Appuyez sur le bouton flottant ou Retour pour quitter")
        }
    }

    fun toggleBrowserFullScreen() {
        setBrowserFullScreen(!_isBrowserFullScreen.value)
    }

    fun exitAnyFullScreen() {
        _isBrowserFullScreen.value = false
        val tab = activeTab.value
        if (tab != null && tab.isFullScreen) {
            engineManager.exitFullScreen(tab.id)
            tabRepository.updateTab(tab.id) { it.copy(isFullScreen = false) }
        }
    }

    // --- Find in Page ---

    fun openFindInPage() {
        val tab = activeTab.value
        if (tab == null || UrlUtils.isHomeUrl(tab.url)) {
            showBanner("Ouvrez d'abord une page Web pour rechercher du texte")
            return
        }
        _findInPageState.value = FindInPageUiState(isVisible = true, query = "")
    }

    fun closeFindInPage() {
        val tab = activeTab.value
        if (tab != null) {
            engineManager.clearFindInPage(tab.id)
        }
        _findInPageState.value = FindInPageUiState(isVisible = false)
    }

    fun updateFindQuery(query: String) {
        _findInPageState.value = _findInPageState.value.copy(query = query)
        val tab = activeTab.value ?: return
        if (query.isBlank()) {
            engineManager.clearFindInPage(tab.id)
            _findInPageState.value = _findInPageState.value.copy(currentMatch = 0, totalMatches = 0, found = false)
            return
        }
        engineManager.findInPage(tab.id, query, backwards = false) { current, total, found ->
            _findInPageState.value = _findInPageState.value.copy(
                currentMatch = current,
                totalMatches = total,
                found = found
            )
        }
    }

    fun findNextInPage() {
        val tab = activeTab.value ?: return
        val query = _findInPageState.value.query
        if (query.isBlank()) return
        engineManager.findInPage(tab.id, query, backwards = false) { current, total, found ->
            _findInPageState.value = _findInPageState.value.copy(
                currentMatch = current,
                totalMatches = total,
                found = found
            )
        }
    }

    fun findPreviousInPage() {
        val tab = activeTab.value ?: return
        val query = _findInPageState.value.query
        if (query.isBlank()) return
        engineManager.findInPage(tab.id, query, backwards = true) { current, total, found ->
            _findInPageState.value = _findInPageState.value.copy(
                currentMatch = current,
                totalMatches = total,
                found = found
            )
        }
    }

    // --- Reader Mode (Mode Lecture) ---

    fun toggleReaderMode() {
        if (_readerModeState.value.isVisible) {
            closeReaderMode()
            return
        }
        val tab = activeTab.value
        if (tab == null || UrlUtils.isHomeUrl(tab.url)) {
            showBanner("Le Mode Lecture est uniquement disponible sur les articles Web")
            return
        }
        _readerModeState.value = _readerModeState.value.copy(
            isVisible = true,
            isLoading = true,
            article = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val res = readerModeExtractor.extractArticle(tab.url, tab.title)) {
                is ReaderModeResult.Success -> {
                    _readerModeState.value = _readerModeState.value.copy(
                        isVisible = true,
                        isLoading = false,
                        article = res.article,
                        errorMessage = null
                    )
                }
                is ReaderModeResult.Incompatible -> {
                    _readerModeState.value = _readerModeState.value.copy(
                        isVisible = true,
                        isLoading = false,
                        article = null,
                        errorMessage = res.reason
                    )
                }
            }
        }
    }

    fun closeReaderMode() {
        _readerModeState.value = _readerModeState.value.copy(isVisible = false, isLoading = false, errorMessage = null)
    }

    fun setReaderFontSize(sizeSp: Int) {
        _readerModeState.value = _readerModeState.value.copy(fontSizeSp = sizeSp.coerceIn(14, 28))
    }

    fun setReaderTheme(theme: String) {
        _readerModeState.value = _readerModeState.value.copy(theme = theme)
    }

    // --- Page Translation ---

    fun translateCurrentPage(targetLangCode: String) {
        val tab = activeTab.value
        if (tab == null || !PageTranslator.isTranslatableUrl(tab.url)) {
            showBanner("Ouvrez une page Web pour la traduire")
            return
        }
        val translatedUrl = PageTranslator.buildTranslatedUrl(tab.url, targetLangCode)
        val langLabel = PageTranslator.supportedLanguages.firstOrNull { it.code == targetLangCode }?.label ?: targetLangCode
        showBanner("Traduction de la page en $langLabel...")
        navigateToInput(translatedUrl)
    }

    fun restoreOriginalUntranslatedPage() {
        val tab = activeTab.value ?: return
        val original = PageTranslator.extractOriginalUrl(tab.url)
        if (original != null) {
            showBanner("Retour à la page d'origine")
            navigateToInput(original)
        } else {
            showBanner("Cette page est déjà dans sa langue d'origine")
        }
    }

    // --- Web Page Capture ---

    fun saveViewportScreenshot(bitmap: Bitmap, shareImmediately: Boolean) {
        val title = activeTab.value?.title ?: "NEXUS"
        viewModelScope.launch {
            val savedFile = pageCaptureManager.saveViewportBitmap(bitmap, title)
            if (savedFile != null) {
                showBanner("Capture enregistrée : ${savedFile.name}")
                if (shareImmediately) {
                    pageCaptureManager.shareCaptureFile(savedFile)
                }
            } else {
                showBanner("Impossible d'enregistrer la capture d'écran")
            }
        }
    }

    fun captureFullPageArticle(shareImmediately: Boolean) {
        val tab = activeTab.value
        if (tab == null || UrlUtils.isHomeUrl(tab.url)) {
            showBanner("Ouvrez une page Web avant de lancer une capture")
            return
        }
        viewModelScope.launch {
            showBanner("Génération de la capture intégrale...")
            val article = _readerModeState.value.article ?: when (val res = readerModeExtractor.extractArticle(tab.url, tab.title)) {
                is ReaderModeResult.Success -> res.article
                is ReaderModeResult.Incompatible -> null
            }
            if (article == null) {
                showBanner("Page non compatible avec la capture d'article complet : utilisez la capture d'écran visible")
                return@launch
            }
            val savedFile = pageCaptureManager.saveFullArticleBitmap(article)
            if (savedFile != null) {
                showBanner("Capture complète enregistrée : ${savedFile.name}")
                if (shareImmediately) {
                    pageCaptureManager.shareCaptureFile(savedFile)
                }
            } else {
                showBanner("Échec de l'enregistrement de la capture complète")
            }
        }
    }

    // --- Cache et miniatures des onglets ---

    fun cacheTabPreview(bitmap: Bitmap) {
        val currentId = activeTab.value?.id ?: return
        cacheTabPreview(currentId, bitmap)
    }

    fun cacheTabPreview(tabId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val path = pageCaptureManager.saveTabPreviewBitmap(tabId, bitmap)
            if (path != null) {
                tabRepository.updateTab(tabId) { it.copy(previewPath = path) }
            }
        }
    }

    fun getCachedTabPreview(tabId: String, previewPath: String?): Bitmap? {
        return pageCaptureManager.getCachedTabPreview(tabId, previewPath)
    }

    // --- Gestion interactive de l'annulation (Undo Banner) ---

    fun triggerUndoAction(message: String, undoAction: () -> Unit) {
        lastClosedAction = undoAction
        _undoBannerMessage.value = message
    }

    fun performUndoClose() {
        lastClosedAction?.invoke()
        lastClosedAction = null
        _undoBannerMessage.value = null
    }

    fun dismissUndoBanner() {
        lastClosedAction = null
        _undoBannerMessage.value = null
    }

    // --- Sessions, Onglets, Groupes et Fenêtres ---

    fun createNewTab(
        url: String = settings.value.homePageUrl,
        isPrivate: Boolean = false,
        windowId: String? = null,
        groupId: String? = null
    ) {
        val targetWindowId = windowId ?: activeWindowId.value
        val newTab = tabRepository.createTab(
            url = url,
            isPrivate = isPrivate,
            select = true,
            isDesktopMode = settings.value.desktopModeDefault,
            windowId = targetWindowId,
            groupId = groupId
        )
        engineManager.setActiveSession(newTab.id)
    }

    fun selectTab(tabId: String) {
        _readerModeState.value = _readerModeState.value.copy(isVisible = false, errorMessage = null)
        if (_findInPageState.value.isVisible) {
            closeFindInPage()
        }
        tabRepository.selectTab(tabId)
        engineManager.setActiveSession(tabId)
    }

    fun duplicateTab(tabId: String) {
        val copy = tabRepository.duplicateTab(tabId) ?: return
        engineManager.setActiveSession(copy.id)
    }

    fun reorderTab(tabId: String, moveUp: Boolean) {
        tabRepository.reorderTab(tabId, moveUp)
    }

    fun togglePinTab(tabId: String) {
        val isNowPinned = tabRepository.togglePinTab(tabId)
        showBanner(if (isNowPinned) "Onglet épinglé" else "Onglet désépinglé")
    }

    fun switchAdjacentTab(direction: Int) {
        val nextTab = tabRepository.switchAdjacentTab(direction)
        if (nextTab != null) {
            engineManager.setActiveSession(nextTab.id)
        }
    }

    fun closeTab(tabId: String) {
        val closingTab = tabs.value.firstOrNull { it.id == tabId }
        engineManager.closeSession(tabId)
        val nextId = tabRepository.closeTab(tabId, settings.value.homePageUrl)
        if (nextId != null) {
            engineManager.setActiveSession(nextId)
        }
        if (closingTab != null && !closingTab.isPrivate) {
            triggerUndoAction("Onglet fermé : ${closingTab.title.ifBlank { "Nouvel onglet" }}") {
                restoreRecentlyClosedTab(closingTab.id)
            }
        }
    }

    fun closeAllTabs(onlyPrivate: Boolean) {
        if (onlyPrivate) {
            engineManager.closeAllPrivateSessions()
        } else {
            tabs.value.forEach { engineManager.closeSession(it.id) }
        }
        tabRepository.closeAllTabs(onlyPrivate, settings.value.homePageUrl)
    }

    fun restoreRecentlyClosedTab(tabId: String? = null) {
        val restored = tabRepository.restoreRecentlyClosedTab(tabId)
        if (restored != null) {
            engineManager.setActiveSession(restored.id)
            showBanner("Onglet restauré : ${restored.title}")
        }
    }

    fun clearRecentlyClosedTabs() {
        tabRepository.clearRecentlyClosedTabs()
    }

    // --- Gestion des fenêtres de navigation ---

    fun createWindow(name: String, isPrivate: Boolean = false): String {
        return tabRepository.createWindow(customName = name).id
    }

    fun renameWindow(windowId: String, newName: String) {
        tabRepository.renameWindow(windowId, newName)
    }

    fun selectWindow(windowId: String) {
        tabRepository.selectWindow(windowId)
        val activeInWin = tabs.value.firstOrNull { it.windowId == windowId }
        if (activeInWin != null) {
            engineManager.setActiveSession(activeInWin.id)
        }
    }

    fun moveTabToWindow(tabId: String, targetWindowId: String) {
        tabRepository.moveTabToWindow(tabId, targetWindowId)
    }

    fun closeWindow(windowId: String) {
        val win = windows.value.firstOrNull { it.id == windowId }
        val nextTabId = tabRepository.closeWindow(windowId, settings.value.homePageUrl)
        if (nextTabId != null) {
            engineManager.setActiveSession(nextTabId)
        }
        if (win != null && !win.isPrivate) {
            triggerUndoAction("Fenêtre fermée : ${win.name}") {
                restoreClosedWindow(win.id)
            }
        }
    }

    fun restoreClosedWindow(closedWindowId: String) {
        val restored = tabRepository.restoreClosedWindow(closedWindowId)
        if (restored != null) {
            showBanner("Fenêtre restaurée : ${restored.name}")
        }
    }

    // --- Gestion des groupes d'onglets ---

    fun createTabGroup(name: String, colorHex: String, windowId: String? = null): String {
        val winId = windowId ?: activeWindowId.value
        return tabRepository.createTabGroup(name, colorHex, winId).id
    }

    fun updateTabGroup(groupId: String, name: String, colorHex: String) {
        tabRepository.updateTabGroup(groupId, name, colorHex)
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        tabRepository.assignTabToGroup(tabId, groupId)
    }

    fun closeTabGroup(groupId: String) {
        val group = tabGroups.value.firstOrNull { it.id == groupId }
        val closedIds = tabRepository.closeTabGroup(groupId, settings.value.homePageUrl)
        if (group != null) {
            triggerUndoAction("Groupe fermé : ${group.name}") {
                restoreClosedTabGroup(group.id)
            }
        }
    }

    fun restoreClosedTabGroup(closedGroupId: String) {
        val restored = tabRepository.restoreClosedTabGroup(closedGroupId)
        if (restored != null) {
            showBanner("Groupe d'onglets restauré : ${restored.name}")
        }
    }

    // --- Paramètres rapides (colonnes raccourcis, mode onglets, moteur) ---

    fun setTabsLayoutMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.updateTabsLayoutMode(mode)
        }
    }

    fun setHomeShortcutColumns(columns: Int) {
        viewModelScope.launch {
            preferencesRepository.updateHomeShortcutColumns(columns)
        }
    }

    fun setHomeQuickAccess(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHomeQuickAccess(enabled)
        }
    }

    fun setHomeSections(shortcuts: Boolean, bookmarks: Boolean, recentHistory: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHomeSections(shortcuts, bookmarks, recentHistory)
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            preferencesRepository.updateSearchEngine(engine)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
            showBanner("Recherches récentes effacées")
        }
    }

    fun handleExternalIntentUrl(externalUrl: String) {
        if (!SecurityPolicy.isSafeNavigationUrl(externalUrl)) return
        val openInNew = settings.value.openLinksInNewTab
        if (openInNew || activeTab.value == null) {
            createNewTab(url = externalUrl, isPrivate = false)
        } else {
            navigateToInput(externalUrl)
        }
    }

    fun toggleCurrentTabBookmark() {
        val tab = activeTab.value ?: return
        if (UrlUtils.isHomeUrl(tab.url)) return
        viewModelScope.launch {
            val added = bookmarkRepository.toggleBookmark(url = tab.url, title = tab.title)
            showBanner(if (added) "Ajouté aux favoris" else "Retiré des favoris")
        }
    }

    fun saveBookmark(id: Long, title: String, url: String, folder: String) {
        viewModelScope.launch {
            bookmarkRepository.saveBookmark(id = id, url = url, title = title, folder = folder)
            showBanner("Favori enregistré")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { bookmarkRepository.deleteBookmark(id) }
    }

    fun saveHomeShortcut(id: Long = 0L, title: String, url: String, colorHex: String, position: Int = 0) {
        viewModelScope.launch {
            val cleanUrl = UrlUtils.normalizeInputToUrlOrSearch(url, settings.value.searchEngine)
            bookmarkRepository.addOrUpdateShortcut(
                id = id,
                title = title.trim().ifBlank { UrlUtils.extractHost(cleanUrl) },
                url = cleanUrl,
                colorHex = colorHex,
                position = position
            )
            showBanner(if (id == 0L) "Raccourci ajouté à l'accueil" else "Raccourci mis à jour")
        }
    }

    fun reorderHomeShortcut(shortcutId: Long, moveLeft: Boolean) {
        viewModelScope.launch {
            bookmarkRepository.reorderShortcut(shortcutId, moveLeft)
        }
    }

    fun deleteHomeShortcut(id: Long) {
        viewModelScope.launch { bookmarkRepository.deleteShortcut(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { historyRepository.deleteItem(id) }
    }

    fun clearHistorySince(sinceMs: Long) {
        viewModelScope.launch {
            historyRepository.clearSince(sinceMs)
            showBanner("Historique effacé")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            showBanner("Tout l'historique a été effacé")
        }
    }

    // --- Downloads & APK ---

    fun startDirectDownload(url: String) {
        val fileName = url.substringBefore("?").substringAfterLast("/").ifBlank { "fichier_${System.currentTimeMillis()}" }
        val mime = if (fileName.endsWith(".apk", ignoreCase = true)) {
            "application/vnd.android.package-archive"
        } else {
            "*/*"
        }
        downloadManager.enqueueDownload(url = url, suggestedFileName = fileName, mimeType = mime)
        showBanner("Téléchargement lancé : $fileName")
    }

    fun pauseOrResumeDownload(item: DownloadEntity) {
        downloadManager.pauseOrResume(item)
    }

    fun openDownload(item: DownloadEntity) {
        if (item.isApk) {
            val apkItem = detectedApks.value.firstOrNull { it.filePath == item.filePath }
            if (apkItem != null) {
                installApk(apkItem)
                return
            }
        }
        if (!downloadManager.openDownloadedFile(item)) {
            showBanner("Aucune application trouvée pour ouvrir ${item.fileName}")
        }
    }

    fun shareDownload(item: DownloadEntity) {
        if (!downloadManager.shareDownloadedFile(item)) {
            showBanner("Impossible de partager ce fichier")
        }
    }

    fun openSystemDownloadsFolder() {
        if (!downloadManager.openDownloadsFolder()) {
            showBanner("Impossible d'ouvrir le gestionnaire de fichiers système")
        }
    }

    fun deleteDownload(item: DownloadEntity) {
        downloadManager.cancelAndDelete(item, deleteFileFromDisk = true)
    }

    fun installApk(item: ApkMetadata) {
        val res = apkManager.requestUserConfirmedInstall(item)
        res.onFailure { err ->
            showBanner(err.localizedMessage ?: "Erreur lors de l'ouverture de l'installateur APK")
        }
    }

    fun deleteApk(item: ApkMetadata) {
        apkManager.deleteApkFile(item)
        showBanner("Fichier APK supprimé")
    }

    fun canInstallPackages(): Boolean = apkManager.canInstallPackages()
    fun openInstallPermissionSettings() = apkManager.openInstallPermissionSettings()

    // --- Extensions & Store ---

    fun filterStoreCatalog(category: ExtensionStoreCategory, query: String, onlyCompatible: Boolean) {
        _storeItems.value = extensionStoreRepository.getCuratedCatalog(category, query, onlyCompatible)
    }

    fun searchOfficialMozillaStore(query: String) {
        if (query.isBlank()) {
            _storeItems.value = extensionStoreRepository.getCuratedCatalog()
            return
        }
        viewModelScope.launch {
            _isSearchingStoreOnline.value = true
            val onlineResults = extensionStoreRepository.searchOfficialMozillaAddons(query)
            _isSearchingStoreOnline.value = false
            if (onlineResults.isNotEmpty()) {
                _storeItems.value = onlineResults
            } else {
                showBanner("Aucun résultat AMO en ligne, affichage du catalogue local vérifié.")
                _storeItems.value = extensionStoreRepository.getCuratedCatalog(ExtensionStoreCategory.ALL, query, false)
            }
        }
    }

    fun installStoreExtension(item: StoreExtensionItem) {
        viewModelScope.launch {
            extensionStoreRepository.installStoreItem(item)
        }
    }

    fun importLocalExtensionUri(uri: Uri) {
        viewModelScope.launch {
            extensionManager.importAndInstallLocalPackage(uri)
        }
    }

    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {
        viewModelScope.launch {
            extensionManager.setExtensionEnabled(extensionId, enabled)
        }
    }

    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            extensionManager.uninstallExtension(extensionId)
        }
    }

    fun checkExtensionUpdate(extensionId: String) {
        viewModelScope.launch {
            extensionManager.checkForUpdates(extensionId)
        }
    }

    fun onTrimMemory(level: Int) {
        engineManager.onTrimMemory(level)
    }

    // --- SessionEventListener callbacks ---

    override fun onUrlChanged(tabId: String, url: String) {
        tabRepository.updateTab(tabId) { it.copy(url = url, errorDescription = null) }
    }

    override fun onTitleChanged(tabId: String, title: String) {
        tabRepository.updateTab(tabId) { tab ->
            val updated = tab.copy(title = title)
            if (!tab.isPrivate && !UrlUtils.isHomeUrl(tab.url)) {
                viewModelScope.launch {
                    historyRepository.recordVisit(url = tab.url, title = title, isPrivate = false)
                }
            }
            updated
        }
    }

    override fun onProgressChanged(tabId: String, progress: Int) {
        tabRepository.updateTab(tabId) { it.copy(progress = progress) }
    }

    override fun onScrollStateChanged(tabId: String, canScrollToTop: Boolean) {
        tabRepository.updateTab(tabId) { it.copy(canScrollToTop = canScrollToTop) }
    }

    override fun onLoadingStateChanged(tabId: String, isLoading: Boolean) {
        tabRepository.updateTab(tabId) { it.copy(isLoading = isLoading) }
    }

    override fun onCanGoBackChanged(tabId: String, canGoBack: Boolean) {
        tabRepository.updateTab(tabId) { it.copy(geckoCanGoBack = canGoBack) }
    }

    override fun onCanGoForwardChanged(tabId: String, canGoForward: Boolean) {
        tabRepository.updateTab(tabId) { it.copy(geckoCanGoForward = canGoForward) }
    }

    override fun onSecurityChanged(tabId: String, isSecure: Boolean, host: String?) {
        tabRepository.updateTab(tabId) { it.copy(isSecure = isSecure, certificateIssuer = host) }
    }

    override fun onTrackerBlocked(tabId: String, blockedUri: String) {
        tabRepository.updateTab(tabId) { it.copy(blockedTrackersCount = it.blockedTrackersCount + 1) }
    }

    override fun onFullScreenChanged(tabId: String, isFullScreen: Boolean) {
        tabRepository.updateTab(tabId) { it.copy(isFullScreen = isFullScreen) }
    }

    override fun onSessionStateUpdated(tabId: String, sessionStateJson: String) {
        tabRepository.updateTab(tabId) { it.copy(sessionStateJson = sessionStateJson) }
    }

    override fun onOpenNewTabRequested(sourceTabId: String, targetUrl: String, isPrivate: Boolean) {
        createNewTab(url = targetUrl, isPrivate = isPrivate)
    }

    override fun onLoadError(tabId: String, failingUrl: String, errorDescription: String) {
        tabRepository.updateTab(tabId) { it.copy(errorDescription = errorDescription, isLoading = false) }
    }
}
