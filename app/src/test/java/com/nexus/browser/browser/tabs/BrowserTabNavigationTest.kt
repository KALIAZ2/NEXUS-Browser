package com.nexus.browser.browser.tabs

import com.nexus.browser.core.common.UrlUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserTabNavigationTest {

    @Test
    fun `home tab initially has canGoBack and canGoForward disabled`() {
        val homeTab = BrowserTab(
            id = "tab-1",
            url = UrlUtils.NEXUS_HOME_URL,
            geckoCanGoBack = false,
            geckoCanGoForward = false,
            lastWebUrlBeforeHome = null
        )

        assertTrue(homeTab.isHomeTab)
        assertFalse(homeTab.canGoBack)
        assertFalse(homeTab.canGoForward)
    }

    @Test
    fun `web tab allows going back to home even when geckoCanGoBack is false`() {
        val webTab = BrowserTab(
            id = "tab-1",
            url = "https://fr.wikipedia.org",
            geckoCanGoBack = false,
            geckoCanGoForward = false
        )

        assertFalse(webTab.isHomeTab)
        assertTrue(webTab.canGoBack)
        assertFalse(webTab.canGoForward)
    }

    @Test
    fun `returning to home from web page enables canGoForward`() {
        val returnedHomeTab = BrowserTab(
            id = "tab-1",
            url = UrlUtils.NEXUS_HOME_URL,
            geckoCanGoBack = false,
            geckoCanGoForward = false,
            lastWebUrlBeforeHome = "https://fr.wikipedia.org"
        )

        assertTrue(returnedHomeTab.isHomeTab)
        assertFalse(returnedHomeTab.canGoBack)
        assertTrue(returnedHomeTab.canGoForward)
    }

    @Test
    fun `web tab with geckoCanGoForward enabled has canGoForward true`() {
        val webTab = BrowserTab(
            id = "tab-1",
            url = "https://mozilla.org",
            geckoCanGoBack = true,
            geckoCanGoForward = true
        )

        assertTrue(webTab.canGoBack)
        assertTrue(webTab.canGoForward)
    }
}
