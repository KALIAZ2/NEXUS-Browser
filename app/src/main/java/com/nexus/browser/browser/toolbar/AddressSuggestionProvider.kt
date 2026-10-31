package com.nexus.browser.browser.toolbar

import com.nexus.browser.core.common.SearchEngine
import com.nexus.browser.core.common.UrlUtils
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.HistoryEntity
import javax.inject.Inject
import javax.inject.Singleton

data class AddressSuggestion(
    val title: String,
    val url: String,
    val source: SuggestionSource,
    val subtitle: String = url,
    val urlOrQuery: String = url
)

enum class SuggestionSource {
    BOOKMARK,
    HISTORY,
    DOMAIN_AUTOCOMPLETE,
    DIRECT_URL,
    SEARCH_ENGINE
}

@Singleton
class AddressSuggestionProvider @Inject constructor() {

    private val commonDomains = listOf(
        "https://www.google.com",
        "https://duckduckgo.com",
        "https://fr.wikipedia.org",
        "https://github.com",
        "https://addons.mozilla.org/fr/android/",
        "https://www.youtube.com",
        "https://www.lemonde.fr",
        "https://www.qwant.com",
        "https://www.ecosia.org"
    )

    fun computeSuggestions(
        query: String,
        bookmarks: List<BookmarkEntity>,
        history: List<HistoryEntity>,
        maxResults: Int = 6
    ): List<AddressSuggestion> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<AddressSuggestion>()

        bookmarks.filter {
            it.title.lowercase().contains(trimmed) || it.url.lowercase().contains(trimmed)
        }.take(3).forEach { bm ->
            results.add(AddressSuggestion(bm.title, bm.url, SuggestionSource.BOOKMARK))
        }

        history.filter { h ->
            (h.title.lowercase().contains(trimmed) || h.url.lowercase().contains(trimmed)) &&
                results.none { it.url == h.url }
        }.take(3).forEach { h ->
            results.add(AddressSuggestion(h.title, h.url, SuggestionSource.HISTORY))
        }

        commonDomains.filter { d ->
            d.contains(trimmed) && results.none { it.url == d }
        }.take(2).forEach { domain ->
            results.add(
                AddressSuggestion(
                    title = domain.removePrefix("https://").removePrefix("www."),
                    url = domain,
                    source = SuggestionSource.DOMAIN_AUTOCOMPLETE
                )
            )
        }

        return results.take(maxResults)
    }

    fun getSuggestions(
        rawQuery: String,
        searchEngine: SearchEngine = SearchEngine.GOOGLE,
        bookmarks: List<BookmarkEntity> = emptyList(),
        history: List<HistoryEntity> = emptyList()
    ): List<AddressSuggestion> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        val list = computeSuggestions(trimmed, bookmarks, history, maxResults = 5).toMutableList()
        if (UrlUtils.isLikelyUrl(trimmed)) {
            val normalized = UrlUtils.normalizeInputToUrlOrSearch(trimmed, searchEngine)
            if (list.none { it.url == normalized }) {
                list.add(
                    0,
                    AddressSuggestion(
                        title = "Ouvrir $normalized",
                        url = normalized,
                        source = SuggestionSource.DIRECT_URL,
                        subtitle = normalized,
                        urlOrQuery = normalized
                    )
                )
            }
        }
        list.add(
            AddressSuggestion(
                title = "Rechercher \"$trimmed\" avec ${searchEngine.displayName}",
                url = UrlUtils.normalizeInputToUrlOrSearch(trimmed, searchEngine),
                source = SuggestionSource.SEARCH_ENGINE,
                subtitle = searchEngine.displayName,
                urlOrQuery = trimmed
            )
        )
        return list.take(6)
    }
}
