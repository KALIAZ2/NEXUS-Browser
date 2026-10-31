package com.nexus.browser.browser.toolbar

import com.nexus.browser.core.common.SearchEngine
import com.nexus.browser.core.database.BookmarkEntity
import com.nexus.browser.core.database.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressSuggestionProviderTest {

    private val provider = AddressSuggestionProvider()

    @Test
    fun `empty query returns recent searches and top bookmarks`() {
        val bookmarks = listOf(
            BookmarkEntity(id = 1, title = "GitHub", url = "https://github.com")
        )
        val history = emptyList<HistoryEntity>()
        val recentSearches = listOf("android kotlin", "geckoview docs")

        val suggestions = provider.computeSuggestions(
            query = "",
            bookmarks = bookmarks,
            history = history,
            recentSearches = recentSearches,
            searchEngine = SearchEngine.GOOGLE
        )

        assertEquals(3, suggestions.size)
        assertEquals(SuggestionSource.RECENT_SEARCH, suggestions[0].source)
        assertEquals("android kotlin", suggestions[0].title)
        assertEquals(SuggestionSource.RECENT_SEARCH, suggestions[1].source)
        assertEquals(SuggestionSource.BOOKMARK, suggestions[2].source)
        assertEquals("GitHub", suggestions[2].title)
    }

    @Test
    fun `query matching bookmark prioritizes bookmark suggestion`() {
        val bookmarks = listOf(
            BookmarkEntity(id = 1, title = "Wikipédia France", url = "https://fr.wikipedia.org"),
            BookmarkEntity(id = 2, title = "GitHub", url = "https://github.com")
        )

        val suggestions = provider.computeSuggestions(
            query = "wiki",
            bookmarks = bookmarks,
            history = emptyList(),
            recentSearches = emptyList(),
            searchEngine = SearchEngine.DUCKDUCKGO
        )

        assertTrue(suggestions.any { it.source == SuggestionSource.BOOKMARK && it.title == "Wikipédia France" })
    }

    @Test
    fun `query matching history returns history suggestion`() {
        val history = listOf(
            HistoryEntity(id = 1, title = "Mozilla Developer Network", url = "https://developer.mozilla.org", lastVisitedAt = 1000L)
        )

        val suggestions = provider.computeSuggestions(
            query = "developer",
            bookmarks = emptyList(),
            history = history,
            recentSearches = emptyList(),
            searchEngine = SearchEngine.GOOGLE
        )

        assertTrue(suggestions.any { it.source == SuggestionSource.HISTORY && it.title == "Mozilla Developer Network" })
    }

    @Test
    fun `arbitrary query includes search engine query suggestion`() {
        val suggestions = provider.getSuggestions(
            rawQuery = "recette tarte tatin",
            bookmarks = emptyList(),
            history = emptyList(),
            recentSearches = emptyList(),
            searchEngine = SearchEngine.QWANT
        )

        assertTrue(suggestions.any { it.source == SuggestionSource.SEARCH_ENGINE })
    }
}
