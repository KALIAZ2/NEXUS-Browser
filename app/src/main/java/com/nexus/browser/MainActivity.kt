package com.nexus.browser

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusTheme
import com.nexus.browser.core.navigation.NexusDestination
import com.nexus.browser.core.security.SecurityPolicy
import com.nexus.browser.settings.SettingsViewModel
import com.nexus.browser.ui.NexusBrowserViewModel
import com.nexus.browser.ui.screens.ApkManagerScreen
import com.nexus.browser.ui.screens.BookmarksScreen
import com.nexus.browser.ui.screens.BrowserScreen
import com.nexus.browser.ui.screens.DownloadsScreen
import com.nexus.browser.ui.screens.ExtensionStoreScreen
import com.nexus.browser.ui.screens.ExtensionsScreen
import com.nexus.browser.ui.screens.HistoryScreen
import com.nexus.browser.ui.screens.PrivacySecurityScreen
import com.nexus.browser.ui.screens.SettingsScreen
import com.nexus.browser.ui.screens.TabsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val browserViewModel: NexusBrowserViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            val settings by browserViewModel.settings.collectAsState()
            val activeTab by browserViewModel.activeTab.collectAsState()
            val tabs by browserViewModel.tabs.collectAsState()
            val recentlyClosedTabs by browserViewModel.recentlyClosedTabs.collectAsState()
            val activeTabId by browserViewModel.activeTabId.collectAsState()
            val suggestions by browserViewModel.addressSuggestions.collectAsState()
            val shortcuts by browserViewModel.homeShortcuts.collectAsState()
            val bookmarks by browserViewModel.bookmarks.collectAsState()
            val folders by browserViewModel.bookmarkFolders.collectAsState()
            val history by browserViewModel.history.collectAsState()
            val downloads by browserViewModel.downloads.collectAsState()
            val detectedApks by browserViewModel.detectedApks.collectAsState()
            val installedExtensions by browserViewModel.installedExtensions.collectAsState()
            val lastCompatReport by browserViewModel.lastExtensionCompatibilityReport.collectAsState()
            val storeItems by browserViewModel.storeItems.collectAsState()
            val isSearchingStoreOnline by browserViewModel.isSearchingStoreOnline.collectAsState()
            val sitePermissions by browserViewModel.sitePermissions.collectAsState()
            val uiBanner by browserViewModel.uiBannerMessage.collectAsState()
            val extBanner by browserViewModel.extensionStatusMessage.collectAsState()
            val isBrowserFullScreen by browserViewModel.isBrowserFullScreen.collectAsState()
            val findInPageState by browserViewModel.findInPageState.collectAsState()
            val readerModeState by browserViewModel.readerModeState.collectAsState()

            val activeExtensionsCount = installedExtensions.count { it.enabled }
            val isVideoFullScreen = activeTab?.isFullScreen == true
            val isEffectiveFullScreen = isVideoFullScreen || isBrowserFullScreen

            // Gestion matérielle et système du mode plein écran (barre d'état, barre de navigation, orientation)
            LaunchedEffect(isEffectiveFullScreen, isVideoFullScreen, settings.fullScreenHideBars) {
                applySystemFullScreenMode(
                    enableImmersive = isVideoFullScreen || (isBrowserFullScreen && settings.fullScreenHideBars),
                    isVideoFullScreen = isVideoFullScreen
                )
            }

            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val spoken = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                    if (!spoken.isNullOrBlank()) {
                        browserViewModel.navigateToInput(spoken)
                    }
                }
            }

            NexusTheme(
                themeMode = settings.themeMode,
                accentHex = settings.accentHex,
                textScalePercent = settings.textScalePercent,
                isPrivateMode = activeTab?.isPrivate == true
            ) {
                val navController = rememberNavController()
                val palette = LocalNexusPalette.current

                BackHandler(enabled = navController.currentDestination?.route == NexusDestination.BROWSER.route) {
                    when {
                        readerModeState.isVisible -> browserViewModel.closeReaderMode()
                        findInPageState.isVisible -> browserViewModel.closeFindInPage()
                        isEffectiveFullScreen -> browserViewModel.exitAnyFullScreen()
                        !browserViewModel.goBack() -> finish()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = NexusDestination.BROWSER.route
                    ) {
                        composable(NexusDestination.BROWSER.route) {
                            BrowserScreen(
                                activeTab = activeTab,
                                tabCount = tabs.size,
                                activeExtensionsCount = activeExtensionsCount,
                                settings = settings,
                                suggestions = suggestions,
                                shortcuts = shortcuts,
                                bookmarks = bookmarks,
                                recentHistory = history,
                                isBrowserFullScreen = isBrowserFullScreen,
                                findInPageState = findInPageState,
                                readerModeState = readerModeState,
                                obtainGeckoSession = { tab -> browserViewModel.obtainGeckoSession(tab) },
                                onAddressQueryChange = { browserViewModel.onAddressQueryChanged(it) },
                                onNavigate = { browserViewModel.navigateToInput(it) },
                                onHomeClick = { browserViewModel.goHome() },
                                onBackClick = { browserViewModel.goBack() },
                                onForwardClick = { browserViewModel.goForward() },
                                onReloadOrStopClick = { browserViewModel.reloadOrStop() },
                                onToggleDesktopMode = { browserViewModel.toggleDesktopMode() },
                                onToggleBookmark = { browserViewModel.toggleCurrentTabBookmark() },
                                onNewTab = { isPriv -> browserViewModel.createNewTab(isPrivate = isPriv) },
                                onSaveShortcut = { id, title, url, hex, pos ->
                                    browserViewModel.saveHomeShortcut(id, title, url, hex, pos)
                                },
                                onReorderShortcut = { id, moveLeft ->
                                    browserViewModel.reorderHomeShortcut(id, moveLeft)
                                },
                                onDeleteShortcut = { browserViewModel.deleteHomeShortcut(it) },
                                onVoiceSearchRequest = {
                                    runCatching {
                                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictez votre recherche NEXUS...")
                                        }
                                        voiceLauncher.launch(speechIntent)
                                    }.onFailure {
                                        browserViewModel.showBanner("Reconnaissance vocale non disponible sur cet appareil")
                                    }
                                },
                                onShowBanner = { browserViewModel.showBanner(it) },
                                onOpenDestination = { dest ->
                                    navController.navigate(dest.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onToggleDarkMode = { isDark ->
                                    settingsViewModel.setThemeMode(if (isDark) "DARK" else "LIGHT")
                                },
                                onToggleFullScreen = { browserViewModel.toggleBrowserFullScreen() },
                                onExitFullScreen = { browserViewModel.exitAnyFullScreen() },
                                onOpenFindInPage = { browserViewModel.openFindInPage() },
                                onCloseFindInPage = { browserViewModel.closeFindInPage() },
                                onUpdateFindQuery = { browserViewModel.updateFindQuery(it) },
                                onFindNext = { browserViewModel.findNextInPage() },
                                onFindPrevious = { browserViewModel.findPreviousInPage() },
                                onToggleReaderMode = { browserViewModel.toggleReaderMode() },
                                onCloseReaderMode = { browserViewModel.closeReaderMode() },
                                onSetReaderFontSize = { browserViewModel.setReaderFontSize(it) },
                                onSetReaderTheme = { browserViewModel.setReaderTheme(it) },
                                onTranslatePage = { browserViewModel.translateCurrentPage(it) },
                                onRestoreTranslatedPage = { browserViewModel.restoreOriginalUntranslatedPage() },
                                onCaptureViewport = { bmp, share -> browserViewModel.saveViewportScreenshot(bmp, share) },
                                onCaptureFullPageArticle = { share -> browserViewModel.captureFullPageArticle(share) }
                            )
                        }

                        composable(NexusDestination.TABS.route) {
                            TabsScreen(
                                tabs = tabs,
                                activeTabId = activeTabId,
                                recentlyClosedTabs = recentlyClosedTabs,
                                onSelectTab = { browserViewModel.selectTab(it) },
                                onCreateTab = { isPriv -> browserViewModel.createNewTab(isPrivate = isPriv) },
                                onDuplicateTab = { browserViewModel.duplicateTab(it) },
                                onReorderTab = { id, up -> browserViewModel.reorderTab(id, up) },
                                onCloseTab = { browserViewModel.closeTab(it) },
                                onCloseAllTabs = { onlyPriv -> browserViewModel.closeAllTabs(onlyPriv) },
                                onRestoreClosedTab = { browserViewModel.restoreRecentlyClosedTab(it) },
                                onClearClosedTabs = { browserViewModel.clearRecentlyClosedTabs() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.EXTENSIONS.route) {
                            ExtensionsScreen(
                                installedExtensions = installedExtensions,
                                lastCompatibilityReport = lastCompatReport,
                                onToggleExtension = { id, en -> browserViewModel.setExtensionEnabled(id, en) },
                                onUninstallExtension = { browserViewModel.uninstallExtension(it) },
                                onCheckUpdate = { browserViewModel.checkExtensionUpdate(it) },
                                onImportLocalXpiUri = { browserViewModel.importLocalExtensionUri(it) },
                                onOpenStore = { navController.navigate(NexusDestination.EXTENSION_STORE.route) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.EXTENSION_STORE.route) {
                            ExtensionStoreScreen(
                                storeItems = storeItems,
                                installedExtensions = installedExtensions,
                                isSearchingOnline = isSearchingStoreOnline,
                                onFilterChange = { cat, q, compat ->
                                    browserViewModel.filterStoreCatalog(cat, q, compat)
                                },
                                onSearchOnlineMozilla = { browserViewModel.searchOfficialMozillaStore(it) },
                                onInstallItem = { browserViewModel.installStoreExtension(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.DOWNLOADS.route) {
                            DownloadsScreen(
                                downloads = downloads,
                                onStartDirectDownload = { browserViewModel.startDirectDownload(it) },
                                onPauseResume = { browserViewModel.pauseOrResumeDownload(it) },
                                onOpen = { browserViewModel.openDownload(it) },
                                onShare = { browserViewModel.shareDownload(it) },
                                onOpenFolder = { browserViewModel.openSystemDownloadsFolder() },
                                onDelete = { browserViewModel.deleteDownload(it) },
                                onOpenApkManager = { navController.navigate(NexusDestination.APK_MANAGER.route) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.APK_MANAGER.route) {
                            ApkManagerScreen(
                                apks = detectedApks,
                                canInstallPackages = browserViewModel.canInstallPackages(),
                                onOpenInstallPermissionSettings = { browserViewModel.openInstallPermissionSettings() },
                                onInstallApk = { browserViewModel.installApk(it) },
                                onDeleteApk = { browserViewModel.deleteApk(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.BOOKMARKS.route) {
                            BookmarksScreen(
                                bookmarks = bookmarks,
                                folders = folders,
                                onOpenUrl = { browserViewModel.navigateToInput(it) },
                                onSaveBookmark = { id, title, url, folder ->
                                    browserViewModel.saveBookmark(id, title, url, folder)
                                },
                                onDeleteBookmark = { browserViewModel.deleteBookmark(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.HISTORY.route) {
                            HistoryScreen(
                                history = history,
                                onOpenUrl = { browserViewModel.navigateToInput(it) },
                                onDeleteItem = { browserViewModel.deleteHistoryItem(it) },
                                onClearSince = { browserViewModel.clearHistorySince(it) },
                                onClearAll = { browserViewModel.clearAllHistory() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.PRIVACY_SECURITY.route) {
                            PrivacySecurityScreen(
                                settings = settings,
                                sitePermissions = sitePermissions,
                                viewModel = settingsViewModel,
                                onOpenNewPrivateTab = {
                                    browserViewModel.createNewTab(isPrivate = true)
                                    navController.popBackStack(NexusDestination.BROWSER.route, inclusive = false)
                                },
                                onShowBanner = { browserViewModel.showBanner(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusDestination.SETTINGS.route) {
                            SettingsScreen(
                                settings = settings,
                                viewModel = settingsViewModel,
                                sitePermissions = sitePermissions,
                                installedExtensions = installedExtensions,
                                downloadsCount = downloads.size,
                                onToggleExtension = { id, en -> browserViewModel.setExtensionEnabled(id, en) },
                                onUninstallExtension = { browserViewModel.uninstallExtension(it) },
                                onOpenDestination = { dest -> navController.navigate(dest.route) },
                                onOpenNewPrivateTab = { browserViewModel.createNewTab(isPrivate = true) },
                                onReloadCurrentTab = { browserViewModel.reloadOrStop() },
                                onToggleBrowserFullScreen = { browserViewModel.toggleBrowserFullScreen() },
                                onOpenSystemDownloadsFolder = { browserViewModel.openSystemDownloadsFolder() },
                                onShowBanner = { browserViewModel.showBanner(it) },
                                onOpenPrivacyScreen = { navController.navigate(NexusDestination.PRIVACY_SECURITY.route) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Bannière de notification flottante
                    val activeBanner = uiBanner ?: extBanner
                    AnimatedVisibility(
                        visible = !activeBanner.isNullOrBlank(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = if (isEffectiveFullScreen) 18.dp else 68.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.surfaceElevated)
                                .clickable { browserViewModel.dismissBanner() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = palette.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = activeBanner.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = palette.textSecondary)
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val isVideoFs = browserViewModel.activeTab.value?.isFullScreen == true
            val isBrowserFs = browserViewModel.isBrowserFullScreen.value
            val hideBars = browserViewModel.settings.value.fullScreenHideBars
            if (isVideoFs || (isBrowserFs && hideBars)) {
                applySystemFullScreenMode(enableImmersive = true, isVideoFullScreen = isVideoFs)
            }
        }
    }

    private fun applySystemFullScreenMode(enableImmersive: Boolean, isVideoFullScreen: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (enableImmersive) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            if (isVideoFullScreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        browserViewModel.onTrimMemory(level)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString ?: return
            if (SecurityPolicy.isSafeNavigationUrl(url)) {
                browserViewModel.createNewTab(url = url, isPrivate = false)
            }
        }
    }
}
