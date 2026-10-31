package com.nexus.browser.extensions.installer

import com.nexus.browser.extensions.compatibility.CompatibilityLevel
import com.nexus.browser.extensions.compatibility.ExtensionCompatibilityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExtensionXpiPackagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val packager = ExtensionXpiPackager()
    private val analyzer = ExtensionCompatibilityAnalyzer()

    @Test
    fun `createXpiArchive creates valid XPI readable by ExtensionCompatibilityAnalyzer`() {
        val outFile = File(tempFolder.root, "packaged_test.xpi")
        val manifest = """
            {
              "manifest_version": 2,
              "name": "Packaged XPI Test",
              "version": "1.0.0",
              "browser_specific_settings": {
                "gecko": { "id": "packaged@nexus.browser" }
              },
              "permissions": ["activeTab"],
              "content_scripts": [{ "matches": ["<all_urls>"], "js": ["content.js"] }]
            }
        """.trimIndent()

        val result = packager.createXpiArchive(
            outputFile = outFile,
            files = mapOf(
                "manifest.json" to manifest,
                "content.js" to "console.log('Nexus XPI Ready');"
            )
        )

        assertTrue(result.isSuccess)
        assertTrue(outFile.exists() && outFile.length() > 0)

        val report = analyzer.analyzePackageFile(outFile)
        assertEquals(CompatibilityLevel.COMPATIBLE, report.level)
        assertEquals("packaged@nexus.browser", report.extensionId)
    }

    @Test
    fun `createXpiArchive rejects missing manifest or path traversal`() {
        val outFile = File(tempFolder.root, "bad.xpi")
        val noManifest = packager.createXpiArchive(outFile, mapOf("content.js" to "alert(1)"))
        assertTrue(noManifest.isFailure)

        val traversal = packager.createXpiArchive(
            outFile,
            mapOf(
                "manifest.json" to "{}",
                "../evil.js" to "alert(1)"
            )
        )
        assertTrue(traversal.isFailure)
    }
}
