package com.nexus.browser.extensions.compatibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExtensionCompatibilityAnalyzerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val analyzer = ExtensionCompatibilityAnalyzer()

    @Test
    fun `analyzeManifestAndScripts marks valid Gecko WebExtension as COMPATIBLE`() {
        val manifest = """
            {
              "manifest_version": 2,
              "name": "Test WebExtension",
              "version": "1.2.0",
              "description": "Extension de test pour GeckoView",
              "browser_specific_settings": {
                "gecko": {
                  "id": "test-ext@nexus.browser"
                }
              },
              "permissions": ["storage", "activeTab"],
              "content_scripts": [
                {
                  "matches": ["<all_urls>"],
                  "js": ["content.js"]
                }
              ]
            }
        """.trimIndent()

        val report = analyzer.analyzeManifestAndScripts(
            manifestJsonString = manifest,
            combinedScripts = "browser.storage.local.get('key');"
        )

        assertEquals(CompatibilityLevel.COMPATIBLE, report.level)
        assertTrue(report.level.isInstallableInGecko)
        assertEquals("test-ext@nexus.browser", report.extensionId)
        assertTrue(report.hasContentScripts)
        assertTrue(report.detectedApis.contains("storage"))
    }

    @Test
    fun `analyzeManifestAndScripts detects desktop-only Chrome APIs as INCOMPATIBLE_MANIFEST`() {
        val manifest = """
            {
              "manifest_version": 3,
              "name": "Chrome Desktop Debugger Only",
              "version": "1.0.0",
              "permissions": ["sidePanel", "debugger", "offscreen"]
            }
        """.trimIndent()

        val report = analyzer.analyzeManifestAndScripts(
            manifestJsonString = manifest,
            combinedScripts = "chrome.sidePanel.open(); chrome.debugger.attach();"
        )

        assertEquals(CompatibilityLevel.INCOMPATIBLE_MANIFEST, report.level)
        assertFalse(report.level.isInstallableInGecko)
        assertTrue(report.unavailableApis.contains("chrome.sidePanel"))
    }

    @Test
    fun `analyzePackageFile rejects Chrome CRX binary header honestly`() {
        val crxFile = tempFolder.newFile("extension_chrome.crx")
        // Write "Cr24" magic bytes followed by dummy payload
        crxFile.writeBytes(byteArrayOf('C'.code.toByte(), 'r'.code.toByte(), '2'.code.toByte(), '4'.code.toByte(), 0, 0, 0, 3))

        val report = analyzer.analyzePackageFile(crxFile)
        assertEquals(CompatibilityLevel.INCOMPATIBLE_CHROME_CRX, report.level)
        assertFalse(report.level.isInstallableInGecko)
        assertTrue(report.errors.any { it.contains("crx", ignoreCase = true) })
    }
}
