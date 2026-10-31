package com.nexus.browser.browser.tools

import androidx.compose.runtime.Immutable
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

@Immutable
data class TranslationLanguage(
    val code: String,
    val label: String
)

object PageTranslator {

    val supportedLanguages: List<TranslationLanguage> = listOf(
        TranslationLanguage("fr", "Français"),
        TranslationLanguage("en", "Anglais"),
        TranslationLanguage("es", "Espagnol"),
        TranslationLanguage("de", "Allemand"),
        TranslationLanguage("it", "Italien"),
        TranslationLanguage("pt", "Portugais"),
        TranslationLanguage("ar", "Arabe"),
        TranslationLanguage("nl", "Néerlandais"),
        TranslationLanguage("ja", "Japonais"),
        TranslationLanguage("zh-CN", "Chinois")
    )

    fun isTranslatableUrl(url: String): Boolean {
        if (url.isBlank() || url.startsWith("nexus://")) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    fun isCurrentlyTranslatedUrl(url: String): Boolean {
        if (!isTranslatableUrl(url)) return false
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        return host == "translate.google.com" || host.endsWith(".translate.goog")
    }

    fun extractOriginalUrl(url: String): String? {
        if (!isCurrentlyTranslatedUrl(url)) return null
        return runCatching {
            val uri = URI(url)
            val query = uri.rawQuery.orEmpty()
            val paramU = query.split("&")
                .firstOrNull { it.startsWith("u=") }
                ?.removePrefix("u=")
                ?.let { URLDecoder.decode(it, "UTF-8") }
            if (!paramU.isNullOrBlank()) {
                return@runCatching paramU
            }
            val host = uri.host ?: return@runCatching null
            if (host.endsWith(".translate.goog")) {
                val translatedSubdomain = host.removeSuffix(".translate.goog")
                val restoredHost = translatedSubdomain
                    .replace("--", "\u0000")
                    .replace("-", ".")
                    .replace("\u0000", "-")
                val cleanPath = uri.rawPath ?: "/"
                "https://$restoredHost$cleanPath"
            } else {
                null
            }
        }.getOrNull()
    }

    fun buildTranslatedUrl(url: String, targetLangCode: String): String {
        val baseOriginal = extractOriginalUrl(url) ?: url
        val encoded = URLEncoder.encode(baseOriginal, "UTF-8")
        return "https://translate.google.com/translate?sl=auto&tl=$targetLangCode&hl=$targetLangCode&u=$encoded"
    }
}
