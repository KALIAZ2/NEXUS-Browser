package com.nexus.browser.apk.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scanner = ApkScanner()

    @Test
    fun `verifyZipStructureIntegrity detects truncated or non-zip APK file`() {
        val truncated = tempFolder.newFile("corrupted.apk")
        truncated.writeBytes(ByteArray(16) { 0x42 })

        val (isValid, reason) = scanner.verifyZipStructureIntegrity(truncated)
        assertFalse(isValid)
        assertNotNull(reason)
    }

    @Test
    fun `verifyZipStructureIntegrity rejects ZIP missing AndroidManifest_xml`() {
        val fakeZip = tempFolder.newFile("not_an_apk.apk")
        ZipOutputStream(FileOutputStream(fakeZip)).use { zos ->
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("Bonjour NEXUS Browser, ceci est une archive de test sans manifeste Android.".toByteArray())
            zos.closeEntry()
        }

        val (isValid, reason) = scanner.verifyZipStructureIntegrity(fakeZip)
        assertFalse(isValid)
        assertTrue(reason?.contains("AndroidManifest.xml") == true)
    }

    @Test
    fun `verifyZipStructureIntegrity accepts valid APK structure with AndroidManifest and classes_dex`() {
        val validApkZip = tempFolder.newFile("valid_structure.apk")
        ZipOutputStream(FileOutputStream(validApkZip)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(ByteArray(128) { 1 })
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(ByteArray(256) { 2 })
            zos.closeEntry()
        }

        val (isValid, reason) = scanner.verifyZipStructureIntegrity(validApkZip)
        assertTrue(isValid)
        assertNull(reason)
    }
}
