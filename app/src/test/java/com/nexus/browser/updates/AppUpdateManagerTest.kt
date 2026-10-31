package com.nexus.browser.updates

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `normalizeVersionString strips v prefix and debug suffix`() {
        assertEquals("1.2.0", AppUpdateUiState.normalizeVersionString("v1.2.0"))
        assertEquals("1.2.0", AppUpdateUiState.normalizeVersionString("V1.2.0"))
        assertEquals("1.1.0", AppUpdateUiState.normalizeVersionString("1.1.0-debug"))
        assertEquals("2.0.1", AppUpdateUiState.normalizeVersionString("  v2.0.1-debug  "))
    }

    @Test
    fun `isRemoteVersionNewer accurately compares semantic versions`() {
        assertTrue(AppUpdateManager.isRemoteVersionNewer("1.2.0", "1.1.0"))
        assertTrue(AppUpdateManager.isRemoteVersionNewer("v1.2.1", "1.2.0"))
        assertTrue(AppUpdateManager.isRemoteVersionNewer("2.0.0", "1.9.9"))
        assertFalse(AppUpdateManager.isRemoteVersionNewer("1.2.0", "1.2.0"))
        assertFalse(AppUpdateManager.isRemoteVersionNewer("v1.2.0", "1.2.0-debug"))
        assertFalse(AppUpdateManager.isRemoteVersionNewer("1.1.0", "1.2.0"))
    }

    @Test
    fun `parseReleaseJson extracts version, changelog and versioned APK asset`() {
        val sampleJson = JSONObject(
            """
            {
              "tag_name": "v1.3.0",
              "name": "NEXUS Browser v1.3.0",
              "body": "## Nouveautés\n- Correction du clavier de la barre d'adresse\n- Système de mises à jour GitHub intégré\n- Optimisation des performances GeckoView",
              "published_at": "2026-10-31T15:00:00Z",
              "html_url": "https://github.com/KALIAZ2/NEXUS-Browser/releases/tag/v1.3.0",
              "assets": [
                {
                  "name": "NEXUS.apk",
                  "size": 79850000,
                  "browser_download_url": "https://github.com/KALIAZ2/NEXUS-Browser/releases/download/v1.3.0/NEXUS.apk"
                },
                {
                  "name": "NEXUS-v1.3.0.apk",
                  "size": 79850000,
                  "browser_download_url": "https://github.com/KALIAZ2/NEXUS-Browser/releases/download/v1.3.0/NEXUS-v1.3.0.apk"
                }
              ]
            }
            """.trimIndent()
        )

        val parsed = AppUpdateManager.parseReleaseJson(sampleJson)
        assertEquals("v1.3.0", parsed.tagName)
        assertEquals("1.3.0", parsed.versionName)
        assertEquals("NEXUS-v1.3.0.apk", parsed.apkFileName)
        assertEquals(79850000L, parsed.apkSizeBytes)
        assertEquals(
            "https://github.com/KALIAZ2/NEXUS-Browser/releases/download/v1.3.0/NEXUS-v1.3.0.apk",
            parsed.apkDownloadUrl
        )
        assertEquals(3, parsed.releaseNotes.size)
        assertEquals("Correction du clavier de la barre d'adresse", parsed.releaseNotes[0])
    }
}
