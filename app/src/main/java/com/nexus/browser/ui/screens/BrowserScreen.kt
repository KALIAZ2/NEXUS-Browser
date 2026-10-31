package com.nexus.browser.ui.screens

import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexus.browser.browser.tabs.BrowserTab
import com.nexus.browser.browser.toolbar.AddressSuggestion
import com.nexus.browser.browser.toolbar.SuggestionSource
import com.nexus.browser.browser.tools.PageTranslator
import com.nexus.browser.browser.tools.ReaderBlock
import com.nexus.browser.core.common.UrlUtils
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.HistoryEntity
import com.nexus.browser.core.database.HomeShortcutEntity
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusBottomSheet
import com.nexus.browser.core.designsystem.NexusBrandLogo
import com.nexus.browser.core.designsystem.NexusButton
import com.nexus.browser.core.designsystem.NexusCosmicBackground
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusSearchBar
import com.nexus.browser.core.designsystem.NexusSiteBadge
import com.nexus.browser.core.designsystem.NexusTextField
import com.nexus.browser.core.designsystem.NexusToolbar
import com.nexus.browser.core.designsystem.NexusWordmark
import com.nexus.browser.core.navigation.NexusDestination
import com.nexus.browser.core.preferences.NexusSettingsState
import com.nexus.browser.ui.FindInPageUiState
import com.nexus.browser.ui.ReaderModeUiState
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    activeTab: BrowserTab?,
    tabCount: Int,
    activeExtensionsCount: Int,
    downloadsCount: Int = 0,
    settings: NexusSettingsState,
    suggestions: List<AddressSuggestion>,
    shortcuts: List<HomeShortcutEntity>,
    bookmarks: List<BookmarkEntity>,
    recentHistory: List<HistoryEntity>,
    isBrowserFullScreen: Boolean = false,
    findInPageState: FindInPageUiState = FindInPageUiState(),
    readerModeState: ReaderModeUiState = ReaderModeUiState(),
    undoBannerMessage: String? = null,
    obtainGeckoSession: (BrowserTab) -> GeckoSession,
    onAddressQueryChange: (String) -> Unit,
    onAddressBarFocused: (String) -> Unit = {},
    onNavigate: (String) -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadOrStopClick: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleBookmark: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onSaveShortcut: (Long, String, String, String, Int) -> Unit,
    onReorderShortcut: (Long, Boolean) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onVoiceSearchRequest: () -> Unit,
    onShowBanner: (String) -> Unit,
    onOpenDestination: (NexusDestination) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onToggleFullScreen: () -> Unit = {},
    onExitFullScreen: () -> Unit = {},
    onOpenFindInPage: () -> Unit = {},
    onCloseFindInPage: () -> Unit = {},
    onUpdateFindQuery: (String) -> Unit = {},
    onFindNext: () -> Unit = {},
    onFindPrevious: () -> Unit = {},
    onToggleReaderMode: () -> Unit = {},
    onCloseReaderMode: () -> Unit = {},
    onSetReaderFontSize: (Int) -> Unit = {},
    onSetReaderTheme: (String) -> Unit = {},
    onTranslatePage: (String) -> Unit = {},
    onRestoreTranslatedPage: () -> Unit = {},
    onCaptureViewport: (Bitmap, Boolean) -> Unit = { _, _ -> },
    onCaptureFullPageArticle: (Boolean) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    onSwipeAdjacentTab: (Int) -> Unit = {},
    onPerformUndoClose: () -> Unit = {},
    onDismissUndoBanner: () -> Unit = {},
    onCacheActiveTabPreview: (Bitmap) -> Unit = {},
    onSetHomeShortcutColumns: (Int) -> Unit = {},
    onSetHomeSections: (Boolean, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onSetHomeQuickAccess: (Boolean) -> Unit = {},
    onSetSearchEngine: (com.nexus.browser.core.common.SearchEngine) -> Unit = {},
    onClearRecentSearches: () -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEditingAddress by remember { mutableStateOf(false) }
    var addressInput by remember { mutableStateOf("") }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<HomeShortcutEntity?>(null) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showCaptureDialog by remember { mutableStateOf(false) }
    var geckoViewRef by remember { mutableStateOf<GeckoView?>(null) }

    val isHomeTab = activeTab == null || activeTab.isHomeTab
    val isFullScreen = (activeTab?.isFullScreen == true) || isBrowserFullScreen
    val currentUrl = activeTab?.url.orEmpty()
    val isTranslatedPage = remember(currentUrl) { PageTranslator.isCurrentlyTranslatedUrl(currentUrl) }

    val shareUrl: (String) -> Unit = { url ->
        runCatching {
            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                putExtra(android.content.Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager l'adresse")
            context.startActivity(shareIntent)
        }.onFailure {
            clipboardManager.setText(AnnotatedString(url))
            onShowBanner("URL copiée dans le presse-papiers")
        }
    }

    // Capture automatique de miniature dès qu'une page a terminé son chargement
    LaunchedEffect(activeTab?.id, activeTab?.isLoading) {
        if (activeTab != null && !activeTab.isHomeTab && !activeTab.isLoading) {
            kotlinx.coroutines.delay(650)
            geckoViewRef?.capturePixels()?.accept({ bmp ->
                if (bmp != null) onCacheActiveTabPreview(bmp)
            }, {})
        }
    }

    val releaseGeckoFocusForAddressBar = {
        geckoViewRef?.apply {
            clearFocus()
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isFocusable = false
            isFocusableInTouchMode = false
        }
        Unit
    }

    val restoreGeckoFocusForWebPage = {
        geckoViewRef?.apply {
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            isFocusable = true
            isFocusableInTouchMode = true
        }
        Unit
    }

    BackHandler(enabled = isEditingAddress) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        isEditingAddress = false
        restoreGeckoFocusForWebPage()
    }

    LaunchedEffect(activeTab?.id) {
        if (isEditingAddress) {
            isEditingAddress = false
            restoreGeckoFocusForWebPage()
        }
        val u = activeTab?.url.orEmpty()
        addressInput = if (UrlUtils.isHomeUrl(u)) "" else u
    }

    LaunchedEffect(isEditingAddress) {
        if (isEditingAddress) {
            releaseGeckoFocusForAddressBar()
        } else {
            restoreGeckoFocusForWebPage()
        }
    }

    LaunchedEffect(activeTab?.url, isEditingAddress) {
        if (!isEditingAddress) {
            val u = activeTab?.url.orEmpty()
            addressInput = if (UrlUtils.isHomeUrl(u)) "" else u
        }
    }

    val toolbarComposable: @Composable () -> Unit = {
        NexusToolbar(
            addressText = if (isEditingAddress) {
                addressInput
            } else {
                val u = activeTab?.url.orEmpty()
                if (UrlUtils.isHomeUrl(u)) "" else u
            },
            isEditingAddress = isEditingAddress,
            isSecureHttps = activeTab?.isSecure ?: true,
            isLoading = activeTab?.isLoading ?: false,
            progress = activeTab?.progress ?: 0,
            isPrivate = activeTab?.isPrivate ?: false,
            activeExtensionsCount = activeExtensionsCount,
            onAddressChange = {
                addressInput = it
                onAddressQueryChange(it)
            },
            onAddressFocusChange = { focused ->
                if (focused) {
                    releaseGeckoFocusForAddressBar()
                    val u = activeTab?.url.orEmpty()
                    addressInput = if (UrlUtils.isHomeUrl(u)) "" else u
                    onAddressBarFocused(addressInput)
                    isEditingAddress = true
                } else {
                    isEditingAddress = false
                    restoreGeckoFocusForWebPage()
                }
            },
            onSubmitAddress = { query ->
                isEditingAddress = false
                restoreGeckoFocusForWebPage()
                onNavigate(query)
            },
            onCopyUrl = { urlToCopy ->
                clipboardManager.setText(AnnotatedString(urlToCopy))
                onShowBanner("URL copiée dans le presse-papiers")
            },
            onShareUrl = { urlToShare ->
                shareUrl(urlToShare)
            },
            onSwipeTab = { delta ->
                onSwipeAdjacentTab(delta)
            },
            onReloadOrStopClick = onReloadOrStopClick,
            onExtensionsClick = { onOpenDestination(NexusDestination.EXTENSIONS) },
            onReleaseWebViewFocus = releaseGeckoFocusForAddressBar
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val isCtrl = event.nativeKeyEvent.isCtrlPressed
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_T -> if (isCtrl) {
                            onNewTab(false)
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_L -> if (isCtrl) {
                            isEditingAddress = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_F -> if (isCtrl) {
                            onOpenFindInPage()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_F11 -> {
                            onToggleFullScreen()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_F5 -> if (isCtrl || event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_F5) {
                            onReloadOrStopClick()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        // En mode plein écran (vidéo ou navigateur), les barres disparaissent totalement
        if (!isFullScreen && !settings.toolbarAtBottom && (!isHomeTab || isEditingAddress)) {
            toolbarComposable()
        }

        // Barre de recherche dans la page (Find in Page)
        AnimatedVisibility(visible = !isFullScreen && findInPageState.isVisible && !isHomeTab) {
            FindInPageBar(
                state = findInPageState,
                onQueryChange = onUpdateFindQuery,
                onPrevious = onFindPrevious,
                onNext = onFindNext,
                onClose = onCloseFindInPage
            )
        }

        // Bandeau intelligent de traduction lorsque la page est traduite
        AnimatedVisibility(visible = !isFullScreen && isTranslatedPage && !isHomeTab) {
            Surface(
                color = palette.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Page traduite",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showTranslateDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Langue", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                        }
                        TextButton(
                            onClick = onRestoreTranslatedPage,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Original", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        // Suggestions pertinentes basées sur l'historique et les favoris
        AnimatedVisibility(
            visible = !isFullScreen && isEditingAddress && suggestions.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Sélecteur rapide de moteur de recherche
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Moteur :",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        com.nexus.browser.core.common.SearchEngine.values().forEach { engine ->
                            val isSelected = settings.searchEngine == engine
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) palette.primary.copy(alpha = 0.2f) else palette.surface,
                                border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.border.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSetSearchEngine(engine) }
                            ) {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) palette.primary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = palette.border.copy(alpha = 0.4f))

                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    isEditingAddress = false
                                    restoreGeckoFocusForWebPage()
                                    onNavigate(suggestion.urlOrQuery)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (suggestion.source) {
                                    SuggestionSource.RECENT_SEARCH -> Icons.Default.Restore
                                    SuggestionSource.BOOKMARK -> Icons.Default.Bookmark
                                    SuggestionSource.HISTORY -> Icons.Default.History
                                    SuggestionSource.DOMAIN_AUTOCOMPLETE -> Icons.Default.Public
                                    SuggestionSource.DIRECT_URL -> Icons.Default.Public
                                    SuggestionSource.SEARCH_ENGINE -> Icons.Default.Search
                                },
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = suggestion.subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (settings.recentSearches.isNotEmpty()) {
                        HorizontalDivider(color = palette.border.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClearRecentSearches() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Effacer les recherches récentes",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Zone principale : Accueil cosmique NEXUS, GeckoView (100% en plein écran), ou Mode Lecture
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isHomeTab) {
                NexusHomeContent(
                    isPrivate = activeTab?.isPrivate ?: false,
                    settings = settings,
                    shortcuts = shortcuts,
                    bookmarks = bookmarks,
                    recentHistory = recentHistory,
                    tabCount = tabCount,
                    downloadsCount = downloadsCount,
                    onSearchSubmit = { query ->
                        isEditingAddress = false
                        onNavigate(query)
                    },
                    onVoiceSearchClick = onVoiceSearchRequest,
                    onOpenDestination = onOpenDestination,
                    onAddShortcutClick = {
                        editingShortcut = null
                        showShortcutDialog = true
                    },
                    onEditShortcutClick = { shortcut ->
                        editingShortcut = shortcut
                        showShortcutDialog = true
                    },
                    onSetShortcutColumns = onSetHomeShortcutColumns,
                    onToggleHomeSections = onSetHomeSections,
                    onToggleQuickAccess = onSetHomeQuickAccess
                )
            } else if (activeTab != null) {
                GeckoSessionViewport(
                    tab = activeTab,
                    obtainGeckoSession = obtainGeckoSession,
                    isAddressBarEditing = isEditingAddress,
                    onUserTouchWebPage = {
                        if (isEditingAddress) {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            isEditingAddress = false
                            restoreGeckoFocusForWebPage()
                        }
                    },
                    onGeckoViewReady = { geckoViewRef = it }
                )
            }

            // Superposition du Mode Lecture (Lecteur d'articles épuré)
            if (readerModeState.isVisible && !isHomeTab) {
                ReaderModeOverlay(
                    state = readerModeState,
                    onClose = onCloseReaderMode,
                    onSetFontSize = onSetReaderFontSize,
                    onSetTheme = onSetReaderTheme,
                    onCaptureArticle = { onCaptureFullPageArticle(true) }
                )
            }

            // Bouton flottant pour remonter en haut de page
            androidx.compose.animation.AnimatedVisibility(
                visible = !isFullScreen && !isHomeTab && activeTab?.canScrollToTop == true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceElevated.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable { onScrollToTop() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Haut de page",
                            tint = palette.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Bannière d'annulation (Undo)
            androidx.compose.animation.AnimatedVisibility(
                visible = undoBannerMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = undoBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = onPerformUndoClose,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Annuler",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onDismissUndoBanner,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bouton flottant discret pour quitter facilement le mode plein écran à tout moment
            if (isFullScreen) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xAA050816),
                    border = BorderStroke(1.dp, Color(0x555685FF)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onExitFullScreen() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Quitter le plein écran",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quitter",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (!isFullScreen && settings.toolbarAtBottom && (!isHomeTab || isEditingAddress)) {
            toolbarComposable()
        }

        // Barre de navigation inférieure masquée automatiquement en mode plein écran
        if (!isFullScreen) {
            NexusBottomNavBar(
                canGoBack = activeTab?.canGoBack == true || readerModeState.isVisible || findInPageState.isVisible,
                canGoForward = activeTab?.canGoForward == true,
                isHome = isHomeTab,
                tabCount = tabCount.coerceAtLeast(1),
                isPrivate = activeTab?.isPrivate == true,
                onBackClick = {
                    isEditingAddress = false
                    onBackClick()
                },
                onForwardClick = {
                    isEditingAddress = false
                    onForwardClick()
                },
                onHomeClick = {
                    isEditingAddress = false
                    onHomeClick()
                },
                onTabsClick = {
                    isEditingAddress = false
                    onOpenDestination(NexusDestination.TABS)
                },
                onNewTabQuickClick = {
                    isEditingAddress = false
                    onNewTab(activeTab?.isPrivate == true)
                },
                onMenuClick = {
                    isEditingAddress = false
                    showMenuSheet = true
                },
                onSwipeAdjacentTab = onSwipeAdjacentTab
            )
        }
    }

    // Menu principal NEXUS
    if (showMenuSheet) {
        val isWebPage = currentUrl.isNotBlank() && !UrlUtils.isHomeUrl(currentUrl)
        val isBookmarked = remember(currentUrl, bookmarks) {
            isWebPage && bookmarks.any { it.url == currentUrl }
        }
        val isDarkMode = settings.themeMode != "LIGHT"

        NexusBottomSheet(onDismissRequest = { showMenuSheet = false }) {
            // En-tête avec logo ruban NEXUS + N Ξ X U S et bouton ✕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexusBrandLogo(size = 38.dp, isPrivate = activeTab?.isPrivate == true)
                    Spacer(Modifier.width(10.dp))
                    NexusWordmark(fontSize = 18.sp, letterSpacing = 4.sp)
                }
                IconButton(
                    onClick = { showMenuSheet = false },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer le menu",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Grille d'outils rapides pour la page Web active (Mode Lecture, Traduire, Rechercher, Capturer, Plein écran, Version PC)
            if (isWebPage) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    QuickToolChip(
                        icon = Icons.Default.MenuBook,
                        label = "Mode Lecture",
                        active = readerModeState.isVisible,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        onToggleReaderMode()
                    }
                    QuickToolChip(
                        icon = Icons.Default.Translate,
                        label = "Traduire",
                        active = isTranslatedPage,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        showTranslateDialog = true
                    }
                    QuickToolChip(
                        icon = Icons.Default.FindInPage,
                        label = "Dans la page",
                        active = findInPageState.isVisible,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        onOpenFindInPage()
                    }
                    QuickToolChip(
                        icon = Icons.Default.CameraAlt,
                        label = "Capturer",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        showCaptureDialog = true
                    }
                    QuickToolChip(
                        icon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        label = "Plein écran",
                        active = isFullScreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        onToggleFullScreen()
                    }
                    QuickToolChip(
                        icon = Icons.Default.Computer,
                        label = "Version PC",
                        active = activeTab?.isDesktopMode == true,
                        modifier = Modifier.weight(1f)
                    ) {
                        showMenuSheet = false
                        onToggleDesktopMode()
                    }
                }
                HorizontalDivider(color = palette.border.copy(alpha = 0.6f))
                Spacer(Modifier.height(6.dp))
            } else {
                // Même sur l'accueil, permettre de basculer en plein écran immersif
                MenuActionRow(
                    icon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    title = if (isFullScreen) "Quitter le plein écran" else "Mode plein écran immersif"
                ) {
                    showMenuSheet = false
                    onToggleFullScreen()
                }
            }

            // Actions d'ouverture d'onglet
            MenuActionRow(
                icon = Icons.Default.Add,
                title = "Nouvel onglet",
                trailingAction = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMenuSheet = false
                                onNewTab(true)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Privé",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            ) {
                showMenuSheet = false
                onNewTab(false)
            }

            if (isWebPage) {
                MenuActionRow(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    title = if (isBookmarked) "Retirer des favoris" else "Ajouter aux favoris",
                    subtitle = UrlUtils.extractHost(currentUrl).ifBlank { currentUrl },
                    trailingAction = {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentUrl))
                                showMenuSheet = false
                                onShowBanner("Lien copié dans le presse-papiers")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier l'URL",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                ) {
                    showMenuSheet = false
                    onToggleBookmark()
                }
            }

            MenuActionRow(
                icon = Icons.Default.StarBorder,
                title = "Favoris",
                subtitle = if (bookmarks.isNotEmpty()) "${bookmarks.size} favoris enregistrés" else null
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.BOOKMARKS)
            }

            MenuActionRow(
                icon = Icons.Default.History,
                title = "Historique"
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.HISTORY)
            }

            MenuActionRow(
                icon = Icons.Default.Extension,
                title = "Extensions",
                subtitle = "$activeExtensionsCount active(s) • WebExtensions GeckoView",
                trailingAction = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMenuSheet = false
                                onOpenDestination(NexusDestination.EXTENSION_STORE)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Catalogue",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.EXTENSIONS)
            }

            MenuActionRow(
                icon = Icons.Default.Download,
                title = "Téléchargements",
                trailingAction = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMenuSheet = false
                                onOpenDestination(NexusDestination.APK_MANAGER)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "APK",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.DOWNLOADS)
            }

            MenuActionRow(
                icon = Icons.Default.Settings,
                title = "Paramètres"
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.SETTINGS)
            }

            MenuActionRow(
                icon = Icons.Default.HelpOutline,
                title = "Confidentialité & Autorisations",
                subtitle = "Protection anti-pistage (${activeTab?.blockedTrackersCount ?: 0} bloqués)"
            ) {
                showMenuSheet = false
                onOpenDestination(NexusDestination.PRIVACY_SECURITY)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))

            // Bascule Mode sombre en bas du menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode sombre",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        onToggleDarkMode(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary,
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.surfaceElevated
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // Boîte de dialogue de Traduction de page
    if (showTranslateDialog) {
        var selectedLang by remember {
            mutableStateOf(if (settings.appLanguage == "EN") "en" else "fr")
        }
        NexusDialog(
            title = "Traduire la page Web",
            confirmText = "Traduire",
            onDismissRequest = { showTranslateDialog = false },
            onConfirm = {
                showTranslateDialog = false
                onTranslatePage(selectedLang)
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sélectionnez la langue cible pour traduire l'intégralité de la page actuelle :",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PageTranslator.supportedLanguages.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang.code,
                            onClick = { selectedLang = lang.code },
                            label = { Text(lang.label) }
                        )
                    }
                }
                if (isTranslatedPage) {
                    Spacer(Modifier.height(4.dp))
                    NexusButton(
                        text = "Restaurer la page originale non traduite",
                        isSecondary = true,
                        onClick = {
                            showTranslateDialog = false
                            onRestoreTranslatedPage()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Boîte de dialogue de Capture de page Web
    if (showCaptureDialog) {
        NexusDialog(
            title = "Capturer la page Web",
            confirmText = "Fermer",
            dismissText = "",
            onDismissRequest = { showCaptureDialog = false },
            onConfirm = { showCaptureDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enregistrez une capture d'écran dans vos Images/Téléchargements ou partagez-la directement :",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                NexusButton(
                    text = "Capturer l'écran visible (PNG)",
                    icon = Icons.Default.CameraAlt,
                    onClick = {
                        showCaptureDialog = false
                        val view = geckoViewRef
                        if (view != null) {
                            view.capturePixels().accept(
                                { bmp ->
                                    if (bmp != null) {
                                        onCaptureViewport(bmp, false)
                                    } else {
                                        onShowBanner("Impossible de capturer l'écran visible")
                                    }
                                },
                                { onShowBanner("Erreur lors de la capture GeckoView") }
                            )
                        } else {
                            onShowBanner("Vue Web non prête pour la capture")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                NexusButton(
                    text = "Capturer & Partager l'écran visible",
                    icon = Icons.Default.Share,
                    isSecondary = true,
                    onClick = {
                        showCaptureDialog = false
                        val view = geckoViewRef
                        if (view != null) {
                            view.capturePixels().accept(
                                { bmp ->
                                    if (bmp != null) {
                                        onCaptureViewport(bmp, true)
                                    } else {
                                        onShowBanner("Impossible de capturer l'écran visible")
                                    }
                                },
                                { onShowBanner("Erreur lors de la capture GeckoView") }
                            )
                        } else {
                            onShowBanner("Vue Web non prête pour la capture")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                NexusButton(
                    text = "Capturer l'article complet (Haute résolution)",
                    icon = Icons.Default.MenuBook,
                    isSecondary = true,
                    onClick = {
                        showCaptureDialog = false
                        onCaptureFullPageArticle(true)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showShortcutDialog) {
        val currentEditing = editingShortcut
        var title by remember(currentEditing) { mutableStateOf(currentEditing?.title.orEmpty()) }
        var url by remember(currentEditing) { mutableStateOf(currentEditing?.url.orEmpty()) }
        NexusDialog(
            title = if (currentEditing == null) "Ajouter un raccourci" else "Modifier le raccourci",
            onDismissRequest = { showShortcutDialog = false },
            onConfirm = {
                if (title.isNotBlank() && url.isNotBlank()) {
                    onSaveShortcut(
                        currentEditing?.id ?: 0L,
                        title.trim(),
                        url.trim(),
                        currentEditing?.iconColorHex ?: "#2979FF",
                        currentEditing?.position ?: shortcuts.size
                    )
                    showShortcutDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nom du site",
                    placeholder = "Ex : Wikipedia"
                )
                NexusTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "Adresse URL",
                    placeholder = "https://fr.wikipedia.org"
                )
                if (currentEditing != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    onReorderShortcut(currentEditing.id, true)
                                    showShortcutDialog = false
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Gauche")
                            }
                            TextButton(
                                onClick = {
                                    onReorderShortcut(currentEditing.id, false)
                                    showShortcutDialog = false
                                }
                            ) {
                                Text("Droite")
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        TextButton(
                            onClick = {
                                onDeleteShortcut(currentEditing.id)
                                showShortcutDialog = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (active) palette.primary.copy(alpha = 0.2f) else palette.surfaceElevated,
        border = BorderStroke(1.dp, if (active) palette.primary else palette.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) palette.primary else palette.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) palette.primary else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FindInPageBar(
    state: FindInPageUiState,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Surface(
        color = palette.surfaceElevated,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NexusTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = "Recherche",
                    placeholder = "Rechercher dans la page..."
                )
            }
            Text(
                text = if (state.query.isBlank()) "0/0" else "${state.currentMatch}/${state.totalMatches}",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.query.isNotBlank() && !state.found) palette.error else palette.textSecondary
            )
            IconButton(
                onClick = onPrevious,
                enabled = state.totalMatches > 0,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Précédent", tint = palette.textPrimary)
            }
            IconButton(
                onClick = onNext,
                enabled = state.totalMatches > 0,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Suivant", tint = palette.textPrimary)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = palette.textSecondary)
            }
        }
    }
}

@Composable
private fun ReaderModeOverlay(
    state: ReaderModeUiState,
    onClose: () -> Unit,
    onSetFontSize: (Int) -> Unit,
    onSetTheme: (String) -> Unit,
    onCaptureArticle: () -> Unit
) {
    val bgColor = when (state.theme) {
        "LIGHT" -> Color(0xFFFBFBFD)
        "SEPIA" -> Color(0xFFF4ECD8)
        else -> Color(0xFF070C1F)
    }
    val textColor = when (state.theme) {
        "LIGHT" -> Color(0xFF111827)
        "SEPIA" -> Color(0xFF3E2C1C)
        else -> Color(0xFFE5EDFF)
    }
    val secondaryColor = when (state.theme) {
        "LIGHT" -> Color(0xFF4B5563)
        "SEPIA" -> Color(0xFF6E563E)
        else -> Color(0xFF94A3B8)
    }
    val accentColor = Color(0xFF2979FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Barre de contrôle du Mode Lecture
        Surface(
            color = bgColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quitter le mode lecture", tint = textColor)
                        }
                        Text(
                            text = "Mode Lecture",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { onSetFontSize(state.fontSizeSp - 2) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("A-", color = textColor, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { onSetFontSize(state.fontSizeSp + 2) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("A+", color = textColor, fontWeight = FontWeight.Bold)
                        }
                        FilterChip(
                            selected = state.theme == "DARK",
                            onClick = { onSetTheme("DARK") },
                            label = { Text("Sombre", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.theme == "SEPIA",
                            onClick = { onSetTheme("SEPIA") },
                            label = { Text("Sépia", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.theme == "LIGHT",
                            onClick = { onSetTheme("LIGHT") },
                            label = { Text("Clair", fontSize = 11.sp) }
                        )
                        if (state.article != null) {
                            IconButton(onClick = onCaptureArticle) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Capturer l'article", tint = accentColor)
                            }
                        }
                    }
                }
                HorizontalDivider(color = secondaryColor.copy(alpha = 0.2f))
            }
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Extraction de l'article en cours...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor
                        )
                    }
                }
            }
            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Mode Lecture indisponible",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        NexusButton(
                            text = "Revenir à la page Web",
                            onClick = onClose
                        )
                    }
                }
            }
            state.article != null -> {
                val article = state.article
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "reader_header") {
                        Text(
                            text = "${article.siteName} • Lecture estimée : ${article.estimatedMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = article.title,
                            fontSize = (state.fontSizeSp + 7).sp,
                            lineHeight = (state.fontSizeSp + 14).sp,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = secondaryColor.copy(alpha = 0.25f))
                    }

                    items(article.blocks) { block ->
                        when (block.type) {
                            ReaderBlock.BlockType.HEADING -> {
                                Text(
                                    text = block.text,
                                    fontSize = (state.fontSizeSp + 3).sp,
                                    lineHeight = (state.fontSizeSp + 10).sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            ReaderBlock.BlockType.QUOTE -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(44.dp)
                                            .background(accentColor, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = block.text,
                                        fontSize = state.fontSizeSp.sp,
                                        lineHeight = (state.fontSizeSp + 9).sp,
                                        color = secondaryColor,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                            ReaderBlock.BlockType.LIST_ITEM -> {
                                Text(
                                    text = "•  ${block.text}",
                                    fontSize = state.fontSizeSp.sp,
                                    lineHeight = (state.fontSizeSp + 9).sp,
                                    color = textColor
                                )
                            }
                            ReaderBlock.BlockType.PARAGRAPH -> {
                                Text(
                                    text = block.text,
                                    fontSize = state.fontSizeSp.sp,
                                    lineHeight = (state.fontSizeSp + 9).sp,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NexusBottomNavBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isHome: Boolean,
    tabCount: Int,
    isPrivate: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    onTabsClick: () -> Unit,
    onNewTabQuickClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSwipeAdjacentTab: (Int) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    var totalDrag by remember { mutableStateOf(0f) }

    Surface(
        color = palette.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag > 60f) {
                            onSwipeAdjacentTab(-1) // Swipe droite -> onglet précédent
                        } else if (totalDrag < -60f) {
                            onSwipeAdjacentTab(1)  // Swipe gauche -> onglet suivant
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = palette.border.copy(alpha = 0.55f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Retour (←)
                NexusIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    enabled = canGoBack,
                    onClick = onBackClick,
                    tint = palette.textPrimary
                )

                // 2. Avancer (→)
                NexusIconButton(
                    icon = Icons.Default.ArrowForward,
                    contentDescription = "Avancer",
                    enabled = canGoForward,
                    onClick = onForwardClick,
                    tint = palette.textPrimary
                )

                // 3. Accueil (⌂)
                NexusIconButton(
                    icon = Icons.Default.Home,
                    contentDescription = "Accueil",
                    onClick = onHomeClick,
                    tint = if (isHome) palette.primary else palette.textPrimary
                )

                // 4. Onglets [ 1 ] (appui court : liste des onglets, appui long : nouvel onglet)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onTabsClick,
                            onLongClick = onNewTabQuickClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.8.dp,
                                color = if (isPrivate) palette.primary else palette.textPrimary,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tabCount > 99) "99+" else tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isPrivate) palette.primary else palette.textPrimary
                        )
                    }
                }

                // 5. Menu (☰)
                NexusIconButton(
                    icon = Icons.Default.Menu,
                    contentDescription = "Menu",
                    onClick = onMenuClick,
                    tint = palette.textPrimary
                )
            }
        }
    }
}

@Composable
private fun GeckoSessionViewport(
    tab: BrowserTab,
    obtainGeckoSession: (BrowserTab) -> GeckoSession,
    isAddressBarEditing: Boolean = false,
    onUserTouchWebPage: () -> Unit = {},
    onGeckoViewReady: (GeckoView) -> Unit = {}
) {
    val geckoSession = remember(tab.id, tab.isPrivate) {
        obtainGeckoSession(tab)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            GeckoView(ctx).apply {
                setSession(geckoSession)
                setOnTouchListener { view, event ->
                    if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                        (view as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                        view.isFocusable = true
                        view.isFocusableInTouchMode = true
                        onUserTouchWebPage()
                    }
                    // Retourne toujours false pour que GeckoView traite normalement tous les clics et formulaires Web
                    false
                }
                onGeckoViewReady(this)
            }
        },
        update = { view ->
            onGeckoViewReady(view)
            if (isAddressBarEditing) {
                view.clearFocus()
                view.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                view.isFocusable = false
                view.isFocusableInTouchMode = false
            } else {
                view.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                view.isFocusable = true
                view.isFocusableInTouchMode = true
            }
            if (view.session !== geckoSession) {
                view.releaseSession()
                view.setSession(geckoSession)
            }
        }
    )

    DisposableEffect(tab.id) {
        onDispose {
            // La session est maintenue dans le pool LRU de BrowserEngineManager
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NexusHomeContent(
    isPrivate: Boolean,
    settings: NexusSettingsState,
    shortcuts: List<HomeShortcutEntity>,
    bookmarks: List<BookmarkEntity>,
    recentHistory: List<HistoryEntity>,
    tabCount: Int = 1,
    downloadsCount: Int = 0,
    onSearchSubmit: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onOpenDestination: (NexusDestination) -> Unit,
    onAddShortcutClick: () -> Unit,
    onEditShortcutClick: (HomeShortcutEntity) -> Unit,
    onSetShortcutColumns: (Int) -> Unit = {},
    onToggleHomeSections: (Boolean, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onToggleQuickAccess: (Boolean) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    var homeSearchQuery by remember { mutableStateOf("") }
    var showHomeSectionsDialog by remember { mutableStateOf(false) }

    NexusCosmicBackground(
        isPrivate = isPrivate,
        style = settings.homeBackgroundStyle,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Logo ruban 3D NEXUS + N Ξ X U S + Barre de recherche centrale
            item(key = "home_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NexusBrandLogo(size = 92.dp, isPrivate = isPrivate)
                    Spacer(Modifier.height(10.dp))
                    NexusWordmark(
                        fontSize = 24.sp,
                        letterSpacing = 7.sp
                    )
                    if (isPrivate) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = palette.primary.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "Navigation privée activée",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(26.dp))

                    NexusSearchBar(
                        query = homeSearchQuery,
                        onQueryChange = { homeSearchQuery = it },
                        onSubmit = {
                            if (it.isNotBlank()) {
                                onSearchSubmit(it)
                                homeSearchQuery = ""
                            }
                        },
                        onVoiceSearchClick = onVoiceSearchClick,
                        placeholder = "Rechercher (${settings.searchEngine.displayName}) ou saisir une URL"
                    )
                }
            }

            // 2. Cartes d'accès rapide (Onglets ouverts & Téléchargements)
            if (settings.showHomeQuickAccess) {
                item(key = "home_quick_access") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenDestination(NexusDestination.TABS) },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceElevated.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, palette.border.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Onglets ouverts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "$tabCount onglet(s)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenDestination(NexusDestination.DOWNLOADS) },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceElevated.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, palette.border.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Téléchargements",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "$downloadsCount fichier(s)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Raccourcis sous forme d'icônes d'applications blanches épurées
            if (settings.showHomeShortcuts) {
                item(key = "home_shortcuts") {
                    val numCols = settings.homeShortcutColumns.coerceIn(3, 5)
                    val (itemWidth, badgeSize) = when (numCols) {
                        3 -> 90.dp to 62.dp
                        5 -> 62.dp to 48.dp
                        else -> 74.dp to 56.dp
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        maxItemsInEachRow = numCols
                    ) {
                        shortcuts.forEach { shortcut ->
                            Column(
                                modifier = Modifier
                                    .width(itemWidth)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onSearchSubmit(shortcut.url) },
                                        onLongClick = { onEditShortcutClick(shortcut) }
                                    )
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                NexusSiteBadge(
                                    title = shortcut.title,
                                    url = shortcut.url,
                                    size = badgeSize
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = shortcut.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showHomeSectionsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Personnaliser l'accueil",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        TextButton(
                            onClick = onAddShortcutClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Ajouter un raccourci",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary
                            )
                        }
                    }
                }
            }

            // 4. Accès rapide aux favoris
            if (settings.showHomeBookmarks && bookmarks.isNotEmpty()) {
                item(key = "home_bookmarks") {
                    SectionHeader(
                        title = "Favoris",
                        actionLabel = "Tout voir",
                        onActionClick = { onOpenDestination(NexusDestination.BOOKMARKS) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookmarks.take(3).forEach { bm ->
                            CompactSiteRow(
                                title = bm.title,
                                url = bm.url,
                                onClick = { onSearchSubmit(bm.url) }
                            )
                        }
                    }
                }
            }

            // 5. Sites récemment visités
            if (!isPrivate && settings.showHomeRecentHistory && recentHistory.isNotEmpty()) {
                item(key = "home_recent_history") {
                    SectionHeader(
                        title = "Récemment visités",
                        actionLabel = "Historique",
                        onActionClick = { onOpenDestination(NexusDestination.HISTORY) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentHistory.take(3).forEach { item ->
                            CompactSiteRow(
                                title = item.title,
                                url = item.url,
                                onClick = { onSearchSubmit(item.url) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHomeSectionsDialog) {
        NexusDialog(
            title = "Personnaliser la page d'accueil",
            confirmText = "Fermer",
            dismissText = "",
            onDismissRequest = { showHomeSectionsDialog = false },
            onConfirm = { showHomeSectionsDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Colonnes de raccourcis",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 4, 5).forEach { cols ->
                        FilterChip(
                            selected = settings.homeShortcutColumns == cols,
                            onClick = { onSetShortcutColumns(cols) },
                            label = { Text("$cols colonnes") }
                        )
                    }
                }

                HorizontalDivider(color = palette.border.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cartes d'accès rapide (Onglets & Téléchargements)", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.showHomeQuickAccess,
                        onCheckedChange = { onToggleQuickAccess(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Raccourcis de sites", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.showHomeShortcuts,
                        onCheckedChange = { onToggleHomeSections(it, settings.showHomeBookmarks, settings.showHomeRecentHistory) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favoris sur l'accueil", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.showHomeBookmarks,
                        onCheckedChange = { onToggleHomeSections(settings.showHomeShortcuts, it, settings.showHomeRecentHistory) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Historique récent", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.showHomeRecentHistory,
                        onCheckedChange = { onToggleHomeSections(settings.showHomeShortcuts, settings.showHomeBookmarks, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.primary
                )
            }
        }
    }
}

@Composable
private fun CompactSiteRow(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val host = remember(url) { UrlUtils.extractHost(url).ifBlank { url } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusSiteBadge(
                title = title,
                url = url,
                size = 36.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { host },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = host,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingAction != null) {
            Spacer(Modifier.width(8.dp))
            trailingAction()
        }
    }
}
