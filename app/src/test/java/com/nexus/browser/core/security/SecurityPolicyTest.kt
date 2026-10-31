package com.nexus.browser.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPolicyTest {

    @Test
    fun `isSafeNavigationUrl blocks dangerous schemes`() {
        assertFalse(SecurityPolicy.isSafeNavigationUrl("javascript:alert(1)"))
        assertFalse(SecurityPolicy.isSafeNavigationUrl("file:///data/data/com.nexus.browser/databases/nexus.db"))
        assertFalse(SecurityPolicy.isSafeNavigationUrl("content://com.nexus.browser.fileprovider/root"))
        assertFalse(SecurityPolicy.isSafeNavigationUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(SecurityPolicy.isSafeNavigationUrl("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `isSafeNavigationUrl allows standard HTTPS and internal home URL`() {
        assertTrue(SecurityPolicy.isSafeNavigationUrl("https://mozilla.org"))
        assertTrue(SecurityPolicy.isSafeNavigationUrl("http://example.com"))
        assertTrue(SecurityPolicy.isSafeNavigationUrl("nexus://home"))
    }

    @Test
    fun `isTrustedExtensionSource requires HTTPS and trusted host`() {
        assertTrue(SecurityPolicy.isTrustedExtensionSource("https://addons.mozilla.org/android/downloads/latest/ublock-origin/latest.xpi"))
        assertFalse(SecurityPolicy.isTrustedExtensionSource("http://addons.mozilla.org/insecure.xpi"))
        assertFalse(SecurityPolicy.isTrustedExtensionSource("https://untrusted-malware-domain.example/ext.xpi"))
    }

    @Test
    fun `classifyExtensionPermissions detects high and medium risk permissions`() {
        val lowRisk = SecurityPolicy.classifyExtensionPermissions(listOf("storage", "activeTab"))
        assertEquals(SecurityPolicy.RiskLevel.LOW, lowRisk.riskLevel)

        val mediumRisk = SecurityPolicy.classifyExtensionPermissions(listOf("<all_urls>", "webRequest", "storage"))
        assertEquals(SecurityPolicy.RiskLevel.MEDIUM, mediumRisk.riskLevel)
        assertTrue(mediumRisk.sensitivePermissions.contains("<all_urls>"))

        val highRisk = SecurityPolicy.classifyExtensionPermissions(listOf("nativeMessaging", "tabs"))
        assertEquals(SecurityPolicy.RiskLevel.HIGH, highRisk.riskLevel)
    }
}
