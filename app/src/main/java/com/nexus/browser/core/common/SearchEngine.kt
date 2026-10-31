package com.nexus.browser.core.common

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val homeUrl: String,
    val searchTemplate: String,
    val suggestionTemplate: String
) {
    GOOGLE(
        id = "google",
        displayName = "Google",
        homeUrl = "https://www.google.com",
        searchTemplate = "https://www.google.com/search?q=%s",
        suggestionTemplate = "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        homeUrl = "https://duckduckgo.com",
        searchTemplate = "https://duckduckgo.com/?q=%s",
        suggestionTemplate = "https://ac.duckduckgo.com/ac/?q=%s&type=list"
    ),
    BING(
        id = "bing",
        displayName = "Bing",
        homeUrl = "https://www.bing.com",
        searchTemplate = "https://www.bing.com/search?q=%s",
        suggestionTemplate = "https://api.bing.com/osjson.aspx?query=%s"
    ),
    QWANT(
        id = "qwant",
        displayName = "Qwant",
        homeUrl = "https://www.qwant.com",
        searchTemplate = "https://www.qwant.com/?q=%s",
        suggestionTemplate = "https://api.qwant.com/v3/suggest?q=%s"
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        homeUrl = "https://www.ecosia.org",
        searchTemplate = "https://www.ecosia.org/search?q=%s",
        suggestionTemplate = "https://ac.ecosia.org/autocomplete?q=%s&type=list"
    );

    fun buildSearchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        return String.format(searchTemplate, encoded)
    }

    companion object {
        fun fromId(id: String): SearchEngine {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE
        }
    }
}
