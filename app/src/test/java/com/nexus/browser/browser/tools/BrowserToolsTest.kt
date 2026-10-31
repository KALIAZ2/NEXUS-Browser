package com.nexus.browser.browser.tools

import com.nexus.browser.browser.permissions.SitePermissionManager
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserToolsTest {

    private val extractor = ReaderModeExtractor(OkHttpClient())

    @Test
    fun `ReaderModeExtractor extracts article paragraphs and reading time`() {
        val sampleHtml = """
            <html>
            <head>
                <title>Guide de l'Architecture GeckoView Mobile</title>
                <meta name="author" content="Équipe NEXUS" />
            </head>
            <body>
                <nav><a href="/">Menu</a></nav>
                <article>
                    <h1>Guide de l'Architecture GeckoView Mobile</h1>
                    <p>Le moteur Mozilla GeckoView offre un environnement d'exécution multi-processus sécurisé pour les navigateurs Android modernes et permet l'installation native de WebExtensions signées.</p>
                    <p>Contrairement à une simple WebView système, GeckoView intègre son propre moteur Quantum CSS, son compilateur JavaScript SpiderMonkey et une isolation stricte des sessions privées.</p>
                    <p>Les utilisateurs profitent ainsi d'une protection avancée contre le pistage inter-sites, d'un mode lecture épuré et d'un contrôle granulaire des permissions par domaine.</p>
                </article>
                <footer>Pied de page</footer>
            </body>
            </html>
        """.trimIndent()

        val result = extractor.parseHtmlToReaderResult(
            url = "https://exemple.fr/articles/geckoview-architecture",
            fallbackTitle = "Article",
            rawHtml = sampleHtml
        )

        assertTrue(result is ReaderModeResult.Success)
        val article = (result as ReaderModeResult.Success).article
        assertEquals("Guide de l'Architecture GeckoView Mobile", article.title)
        assertEquals("exemple.fr", article.siteName)
        assertTrue(article.blocks.size >= 3)
        assertTrue(article.estimatedMinutes >= 1)
    }

    @Test
    fun `ReaderModeExtractor marks non-article short page as incompatible`() {
        val shortHtml = """
            <html><body><div>Connexion requise</div></body></html>
        """.trimIndent()

        val result = extractor.parseHtmlToReaderResult(
            url = "https://exemple.fr/login",
            fallbackTitle = "Connexion",
            rawHtml = shortHtml
        )

        assertTrue(result is ReaderModeResult.Incompatible)
    }

    @Test
    fun `PageTranslator builds valid translation URL and restores original URL`() {
        val originalUrl = "https://developer.mozilla.org/en-US/docs/Mozilla/GeckoView"
        assertTrue(PageTranslator.isTranslatableUrl(originalUrl))

        val translatedUrl = PageTranslator.buildTranslatedUrl(originalUrl, "fr")
        assertTrue(PageTranslator.isCurrentlyTranslatedUrl(translatedUrl))

        val restored = PageTranslator.extractOriginalUrl(translatedUrl)
        assertEquals(originalUrl, restored)
    }

    @Test
    fun `PageTranslator rejects internal nexus urls`() {
        assertFalse(PageTranslator.isTranslatableUrl("nexus://home"))
    }

    @Test
    fun `SitePermissionManager formats permission types in French`() {
        assertEquals("Caméra", SitePermissionManager.formatPermissionTypeFr("CAMERA"))
        assertEquals("Microphone", SitePermissionManager.formatPermissionTypeFr("MICROPHONE"))
        assertEquals("Localisation (GPS)", SitePermissionManager.formatPermissionTypeFr("GEOLOCATION"))
        assertEquals("Notifications Web", SitePermissionManager.formatPermissionTypeFr("NOTIFICATIONS"))
    }
}
