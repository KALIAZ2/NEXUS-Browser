package com.nexus.browser.browser.tabs

import androidx.compose.runtime.Immutable
import com.nexus.browser.core.common.UrlUtils

@Immutable
data class BrowserWindow(
    val id: String,
    val name: String,
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAIN_WINDOW_ID = "window_main"
        const val PRIVATE_WINDOW_ID = "window_private"
    }
}

@Immutable
data class TabGroup(
    val id: String,
    val name: String,
    val colorHex: String = "#00E5FF",
    val windowId: String = BrowserWindow.MAIN_WINDOW_ID,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class ClosedTabGroup(
    val group: TabGroup,
    val tabs: List<BrowserTab>,
    val closedAt: Long = System.currentTimeMillis()
) {
    val id: String get() = group.id
    val name: String get() = group.name
    val colorHex: String get() = group.colorHex
    val tabsCount: Int get() = tabs.size
}

@Immutable
data class ClosedBrowserWindow(
    val window: BrowserWindow,
    val tabs: List<BrowserTab>,
    val closedAt: Long = System.currentTimeMillis()
) {
    val id: String get() = window.id
    val name: String get() = window.name
    val isPrivate: Boolean get() = window.isPrivate
    val tabsCount: Int get() = tabs.size
}

@Immutable
data class BrowserTab(
    val id: String,
    val url: String = UrlUtils.NEXUS_HOME_URL,
    val title: String = "Nouvel onglet",
    val faviconUrl: String? = null,
    val isPrivate: Boolean = false,
    val isActive: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val geckoCanGoBack: Boolean = false,
    val geckoCanGoForward: Boolean = false,
    val lastWebUrlBeforeHome: String? = null,
    val isSecure: Boolean = true,
    val certificateIssuer: String? = null,
    val isFullScreen: Boolean = false,
    val isDesktopMode: Boolean = false,
    val blockedTrackersCount: Int = 0,
    val errorDescription: String? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val sessionStateJson: String? = null,
    val windowId: String = if (isPrivate) BrowserWindow.PRIVATE_WINDOW_ID else BrowserWindow.MAIN_WINDOW_ID,
    val groupId: String? = null,
    val previewPath: String? = null,
    val isPinned: Boolean = false,
    val canScrollToTop: Boolean = false
) {
    val isHomeTab: Boolean
        get() = UrlUtils.isHomeUrl(url)

    val canGoBack: Boolean
        get() = if (isHomeTab) false else true

    val canGoForward: Boolean
        get() = if (isHomeTab) !lastWebUrlBeforeHome.isNullOrBlank() else geckoCanGoForward
}
