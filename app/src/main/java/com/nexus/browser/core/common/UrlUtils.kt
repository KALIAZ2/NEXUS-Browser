package com.nexus.browser.core.common

import java.net.URI

object UrlUtils {

    private val DOMAIN_REGEX = Regex(
        pattern = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(/.*)?$"
    )
    private val IPV4_REGEX = Regex(
        pattern = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}(:\\d+)?(/.*)?$"
    )

    const val NEXUS_HOME_URL = "nexus://home"

    fun isHomeUrl(url: String?): Boolean {
        return url.isNullOrBlank() || url == NEXUS_HOME_URL || url == "about:blank"
    }

    fun isLikelyUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) return false
        val lower = trimmed.lowercase()
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("about:") ||
            lower.startsWith("nexus://") ||
            lower.startsWith("localhost")
        ) {
            return true
        }
        return DOMAIN_REGEX.matches(trimmed) || IPV4_REGEX.matches(trimmed)
    }

    fun normalizeInputToUrlOrSearch(
        rawInput: String,
        searchEngine: SearchEngine = SearchEngine.GOOGLE,
        httpsOnlyMode: Boolean = true
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return NEXUS_HOME_URL
        if (trimmed.equals(NEXUS_HOME_URL, ignoreCase = true)) return NEXUS_HOME_URL

        return if (isLikelyUrl(trimmed)) {
            val lower = trimmed.lowercase()
            when {
                lower.startsWith("https://") || lower.startsWith("about:") || lower.startsWith("nexus://") -> trimmed
                lower.startsWith("http://") -> {
                    if (httpsOnlyMode && !isLocalAddress(trimmed)) {
                        "https://" + trimmed.substring(7)
                    } else {
                        trimmed
                    }
                }
                isLocalAddress(trimmed) -> "http://$trimmed"
                else -> "https://$trimmed"
            }
        } else {
            searchEngine.buildSearchUrl(trimmed)
        }
    }

    fun isLocalAddress(urlOrHost: String): Boolean {
        val lower = urlOrHost.lowercase().removePrefix("http://").removePrefix("https://")
        return lower.startsWith("localhost") ||
            lower.startsWith("127.0.0.1") ||
            lower.startsWith("10.0.2.2") ||
            lower.startsWith("192.168.")
    }

    fun extractHost(url: String?): String {
        if (url.isNullOrBlank() || isHomeUrl(url)) return "NEXUS Accueil"
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url.removePrefix("https://").removePrefix("http://").substringBefore("/")
        }
    }

    fun isSecureHttps(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.lowercase().startsWith("https://") || url.lowercase().startsWith("nexus://")
    }
}
