package com.nexus.browser.core.security

import java.net.URI

/**
 * Politique de sécurité centrale de NEXUS Browser.
 * Garantit l'absence d'exposition d'interfaces natives privilégiées,
 * la vérification des schémas d'URL, et l'évaluation des permissions sensibles.
 */
object SecurityPolicy {

    private val BLOCKED_SCHEMES = setOf(
        "javascript",
        "vbscript",
        "data",
        "intent",
        "file",
        "content"
    )

    val HIGH_RISK_EXTENSION_PERMISSIONS = setOf(
        "<all_urls>",
        "*://*/*",
        "http://*/*",
        "https://*/*",
        "webRequest",
        "webRequestBlocking",
        "DeclarativeNetRequest",
        "cookies",
        "history",
        "tabs",
        "nativeMessaging",
        "proxy",
        "privacy",
        "browsingData",
        "clipboardRead",
        "downloads",
        "Downloads.open"
    )

    /**
     * Vérifie si une URL saisie ou redirigée est sûre à charger depuis une source externe.
     */
    fun isSafeNavigationUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        if (lower == "nexus://home" || lower == "about:blank") return true

        val scheme = lower.substringBefore(":", "")
        if (scheme in BLOCKED_SCHEMES) {
            return false
        }
        return scheme == "https" || scheme == "http"
    }

    /**
     * Vérifie qu'une source de téléchargement d'extension XPI provient d'une origine HTTPS valide.
     */
    fun isTrustedExtensionSource(url: String, customAllowedHosts: Set<String> = emptySet()): Boolean {
        return try {
            val uri = URI(url.trim())
            if (!uri.scheme.equals("https", ignoreCase = true)) return false
            val host = uri.host?.lowercase() ?: return false
            val defaultTrusted = setOf(
                "addons.mozilla.org",
                "services.addons.mozilla.org",
                "github.com",
                "raw.githubusercontent.com"
            )
            host in defaultTrusted || host in customAllowedHosts.map { it.lowercase() }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Analyse la liste des permissions demandées par une extension WebExtension
     * et sépare les permissions standards des permissions sensibles.
     */
    fun classifyExtensionPermissions(permissions: List<String>): PermissionRiskReport {
        val sensitive = mutableListOf<String>()
        val standard = mutableListOf<String>()

        for (perm in permissions) {
            val clean = perm.trim()
            val isWildcardHost = clean.contains("://*") || clean == "<all_urls>"
            if (isWildcardHost || clean in HIGH_RISK_EXTENSION_PERMISSIONS) {
                sensitive.add(clean)
            } else {
                standard.add(clean)
            }
        }

        val riskLevel = when {
            sensitive.any { it == "nativeMessaging" || it == "proxy" } -> RiskLevel.HIGH
            sensitive.isNotEmpty() -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return PermissionRiskReport(
            riskLevel = riskLevel,
            sensitivePermissions = sensitive,
            standardPermissions = standard
        )
    }

    enum class RiskLevel(val labelFr: String) {
        LOW("Faible"),
        MEDIUM("Modéré (accès aux données de navigation)"),
        HIGH("Élevé (permissions étendues)")
    }

    data class PermissionRiskReport(
        val riskLevel: RiskLevel,
        val sensitivePermissions: List<String>,
        val standardPermissions: List<String>
    )
}
