package com.nexus.browser.browser.tabs

import androidx.compose.runtime.Immutable
import com.nexus.browser.core.common.UrlUtils

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
    val sessionStateJson: String? = null
) {
    val isHomeTab: Boolean
        get() = UrlUtils.isHomeUrl(url)

    val canGoBack: Boolean
        get() = if (isHomeTab) false else true

    val canGoForward: Boolean
        get() = if (isHomeTab) !lastWebUrlBeforeHome.isNullOrBlank() else geckoCanGoForward
}
