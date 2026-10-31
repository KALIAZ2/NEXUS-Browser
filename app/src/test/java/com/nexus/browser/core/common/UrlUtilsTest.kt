package com.nexus.browser.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `normalizeInputToUrlOrSearch upgrades bare domains to HTTPS`() {
        val result = UrlUtils.normalizeInputToUrlOrSearch(
            rawInput = "mozilla.org/fr",
            searchEngine = SearchEngine.GOOGLE,
            httpsOnlyMode = true
        )
        assertEquals("https://mozilla.org/fr", result)
    }

    @Test
    fun `normalizeInputToUrlOrSearch upgrades HTTP URL when forceHttps is enabled`() {
        val result = UrlUtils.normalizeInputToUrlOrSearch(
            rawInput = "http://github.com/mozilla/geckoview",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnlyMode = true
        )
        assertEquals("https://github.com/mozilla/geckoview", result)
    }

    @Test
    fun `normalizeInputToUrlOrSearch converts natural language query to search URL`() {
        val result = UrlUtils.normalizeInputToUrlOrSearch(
            rawInput = "comment installer une extension geckoview",
            searchEngine = SearchEngine.QWHANT_OR_DUCK,
            httpsOnlyMode = true
        )
        assertTrue(result.startsWith("https://www.qwant.com/?q="))
        assertTrue(result.contains("geckoview"))
    }

    @Test
    fun `isLikelyUrl distinguishes domains from search phrases`() {
        assertTrue(UrlUtils.isLikelyUrl("https://addons.mozilla.org"))
        assertTrue(UrlUtils.isLikelyUrl("lemonde.fr"))
        assertTrue(UrlUtils.isLikelyUrl("192.168.1.1:8080"))
        assertFalse(UrlUtils.isLikelyUrl("navigateur android rapide"))
        assertFalse(UrlUtils.isLikelyUrl("qu'est-ce que geckoview ?"))
    }

    @Test
    fun `extractHost strips www prefix cleanly`() {
        assertEquals("wikipedia.org", UrlUtils.extractHost("https://www.wikipedia.org/wiki/Android"))
    }

    companion object {
        private val SearchEngine.Companion.QWHANT_OR_DUCK: SearchEngine
            get() = SearchEngine.QWANT
    }
}
