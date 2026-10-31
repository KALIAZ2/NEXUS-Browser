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
    RECENT_SEARCH,
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
        "https://www.ecosia.org",
        "https://www.reddit.com"
    )

    fun computeSuggestions(
        query: String,
        bookmarks: List<BookmarkEntity>,
        history: List<HistoryEntity>,
        recentSearches: List<String> = emptyList(),
        searchEngine: SearchEngine = SearchEngine.GOOGLE,
        maxResults: Int = 7
    ): List<AddressSuggestion> {
        val trimmed = query.trim().lowercase()
        val results = mutableListOf<AddressSuggestion>()

        if (trimmed.isEmpty()) {
            // Quand l'utilisateur ouvre la barre d'adresse vide : afficher ses recherches récentes et favoris rapides
            recentSearches.take(4).forEach { recent ->
                results.add(
                    AddressSuggestion(
                        title = recent,
                        url = UrlUtils.normalizeInputToUrlOrSearch(recent, searchEngine),
                        source = SuggestionSource.RECENT_SEARCH,
                        subtitle = "Recherche récente • ${searchEngine.displayName}",
                        urlOrQuery = recent
                    )
                )
            }
            bookmarks.take(3).forEach { bm ->
                if (results.none { it.url == bm.url }) {
                    results.add(
                        AddressSuggestion(
                            title = bm.title,
                            url = bm.url,
                            source = SuggestionSource.BOOKMARK,
                            subtitle = bm.url,
                            urlOrQuery = bm.url
                        )
                    )
                }
            }
            return results.take(maxResults)
        }

        // 1. Recherches récentes correspondantes
        recentSearches.filter { it.lowercase().contains(trimmed) }.take(2).forEach { recent ->
            results.add(
                AddressSuggestion(
                    title = recent,
                    url = UrlUtils.normalizeInputToUrlOrSearch(recent, searchEngine),
                    source = SuggestionSource.RECENT_SEARCH,
                    subtitle = "Recherche récente",
                    urlOrQuery = recent
                )
            )
        }

        // 2. Favoris correspondants
        bookmarks.filter {
            it.title.lowercase().contains(trimmed) || it.url.lowercase().contains(trimmed)
        }.take(3).forEach { bm ->
            if (results.none { it.url == bm.url }) {
                results.add(
                    AddressSuggestion(
                        title = bm.title,
                        url = bm.url,
                        source = SuggestionSource.BOOKMARK,
                        subtitle = bm.url,
                        urlOrQuery = bm.url
                    )
                )
            }
        }

        // 3. Historique correspondant
        history.filter { h ->
            (h.title.lowercase().contains(trimmed) || h.url.lowercase().contains(trimmed)) &&
                results.none { it.url == h.url }
        }.take(3).forEach { h ->
            results.add(
                AddressSuggestion(
                    title = h.title,
                    url = h.url,
                    source = SuggestionSource.HISTORY,
                    subtitle = h.url,
                    urlOrQuery = h.url
                )
            )
        }

        // 4. Autocomplétion de domaine
        commonDomains.filter { d ->
            d.contains(trimmed) && results.none { it.url == d }
        }.take(2).forEach { domain ->
            results.add(
                AddressSuggestion(
                    title = domain.removePrefix("https://").removePrefix("www."),
                    url = domain,
                    source = SuggestionSource.DOMAIN_AUTOCOMPLETE,
                    subtitle = domain,
                    urlOrQuery = domain
                )
            )
        }

        return results.take(maxResults)
    }

    fun getSuggestions(
        rawQuery: String,
        searchEngine: SearchEngine = SearchEngine.GOOGLE,
        bookmarks: List<BookmarkEntity> = emptyList(),
        history: List<HistoryEntity> = emptyList(),
        recentSearches: List<String> = emptyList()
    ): List<AddressSuggestion> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            return computeSuggestions(
                query = "",
                bookmarks = bookmarks,
                history = history,
                recentSearches = recentSearches,
                searchEngine = searchEngine,
                maxResults = 6
            )
        }

        val list = computeSuggestions(
            query = trimmed,
            bookmarks = bookmarks,
            history = history,
            recentSearches = recentSearches,
            searchEngine = searchEngine,
            maxResults = 5
        ).toMutableList()

        if (UrlUtils.isLikelyUrl(trimmed)) {
            val normalized = UrlUtils.normalizeInputToUrlOrSearch(trimmed, searchEngine)
            if (list.none { it.url == normalized }) {
                list.add(
                    0,
                    AddressSuggestion(
                        title = "Aller sur $normalized",
                        url = normalized,
                        source = SuggestionSource.DIRECT_URL,
                        subtitle = "Adresse Web directe",
                        urlOrQuery = normalized
                    )
                )
            }
        }
        list.add(
            AddressSuggestion(
                title = "Rechercher « $trimmed »",
                url = UrlUtils.normalizeInputToUrlOrSearch(trimmed, searchEngine),
                source = SuggestionSource.SEARCH_ENGINE,
                subtitle = "Moteur ${searchEngine.displayName}",
                urlOrQuery = trimmed
            )
        )
        return list.take(7)
    }
}
