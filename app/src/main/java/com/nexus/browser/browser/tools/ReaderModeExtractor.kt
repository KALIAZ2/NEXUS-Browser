package com.nexus.browser.browser.tools

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Immutable
data class ReaderBlock(
    val type: BlockType,
    val text: String
) {
    enum class BlockType {
        HEADING,
        PARAGRAPH,
        QUOTE,
        LIST_ITEM
    }
}

@Immutable
data class ReaderModeArticle(
    val url: String,
    val title: String,
    val siteName: String,
    val estimatedMinutes: Int,
    val blocks: List<ReaderBlock>
)

sealed interface ReaderModeResult {
    data class Success(val article: ReaderModeArticle) : ReaderModeResult
    data class Incompatible(val reason: String) : ReaderModeResult
}

@Singleton
class ReaderModeExtractor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun extractArticle(url: String, fallbackTitle: String): ReaderModeResult = withContext(Dispatchers.IO) {
        if (url.isBlank() || url.startsWith("nexus://") || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return@withContext ReaderModeResult.Incompatible(
                "Cette page interne ne peut pas être ouverte en Mode Lecture."
            )
        }

        val html = runCatching {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) Gecko/130.0 Firefox/130.0"
                )
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            }
        }.getOrNull()

        if (html.isNullOrBlank()) {
            return@withContext ReaderModeResult.Incompatible(
                "Impossible d'extraire le contenu de cette page (connexion interrompue ou contenu protégé)."
            )
        }

        parseHtmlToReaderResult(url, fallbackTitle, html)
    }

    internal fun parseHtmlToReaderResult(url: String, fallbackTitle: String, rawHtml: String): ReaderModeResult {
        val host = runCatching { URI(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)

        val ogTitle = Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(rawHtml)?.groupValues?.getOrNull(1)
        val htmlTitle = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(rawHtml)?.groupValues?.getOrNull(1)
        val h1Title = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(rawHtml)?.groupValues?.getOrNull(1)

        val resolvedTitle = listOfNotNull(h1Title, ogTitle, htmlTitle, fallbackTitle)
            .map { stripTagsAndDecode(it) }
            .firstOrNull { it.length in 3..220 }
            ?: fallbackTitle.ifBlank { host }

        // Prefer <article> or <main> if present
        val mainRegion = Regex("""<article[^>]*>(.*?)</article>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(rawHtml)?.groupValues?.getOrNull(1)
            ?: Regex("""<main[^>]*>(.*?)</main>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(rawHtml)?.groupValues?.getOrNull(1)
            ?: rawHtml

        // Strip noisy tags
        val cleaned = mainRegion
            .replace(Regex("""<(script|style|noscript|svg|nav|footer|header|aside|form|iframe|button)[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")

        val blockRegex = Regex(
            """<(h2|h3|p|blockquote|li)\b[^>]*>(.*?)</\1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val extractedBlocks = mutableListOf<ReaderBlock>()
        var totalWords = 0

        for (match in blockRegex.findAll(cleaned)) {
            val tag = match.groupValues[1].lowercase()
            val text = stripTagsAndDecode(match.groupValues[2])
            if (text.isBlank()) continue

            val type = when (tag) {
                "h2", "h3" -> ReaderBlock.BlockType.HEADING
                "blockquote" -> ReaderBlock.BlockType.QUOTE
                "li" -> ReaderBlock.BlockType.LIST_ITEM
                else -> ReaderBlock.BlockType.PARAGRAPH
            }

            // Filter out tiny menu crumbs or boilerplate
            val minLen = when (type) {
                ReaderBlock.BlockType.HEADING -> 4
                ReaderBlock.BlockType.LIST_ITEM -> 18
                else -> 32
            }
            if (text.length >= minLen && !extractedBlocks.any { it.text == text }) {
                extractedBlocks.add(ReaderBlock(type = type, text = text))
                totalWords += text.split(Regex("\\s+")).size
            }
        }

        if (extractedBlocks.size < 2 || totalWords < 35) {
            return ReaderModeResult.Incompatible(
                "Cette page ne contient pas d'article structuré compatible avec le Mode Lecture."
            )
        }

        val minutes = max(1, totalWords / 200)
        return ReaderModeResult.Success(
            ReaderModeArticle(
                url = url,
                title = resolvedTitle,
                siteName = host,
                estimatedMinutes = minutes,
                blocks = extractedBlocks
            )
        )
    }

    private fun stripTagsAndDecode(input: String): String {
        return input
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&rsquo;", "’")
            .replace("&lsquo;", "‘")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace(Regex("""&#(\d+);""")) { m ->
                m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: ""
            }
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
