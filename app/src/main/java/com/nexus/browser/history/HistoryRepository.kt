package com.nexus.browser.history

import com.nexus.browser.core.common.UrlUtils
import com.nexus.browser.core.database.HistoryDao
import com.nexus.browser.core.database.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    val historyFlow: Flow<List<HistoryEntity>> = historyDao.observeHistory()
    val mostVisitedFlow: Flow<List<HistoryEntity>> = historyDao.observeMostVisited(8)

    fun searchHistory(query: String, sinceTimestamp: Long = 0L): Flow<List<HistoryEntity>> {
        return historyDao.searchHistory(query.trim(), sinceTimestamp)
    }

    /**
     * Enregistre une visite uniquement si la session n'est PAS en mode privé
     * et qu'il s'agit d'une véritable URL web.
     */
    suspend fun recordVisit(url: String, title: String, isPrivate: Boolean) {
        if (isPrivate) return
        if (UrlUtils.isHomeUrl(url)) return
        if (url.startsWith("data:") || url.startsWith("about:")) return

        val existing = historyDao.findByUrl(url)
        if (existing != null) {
            historyDao.insertOrReplace(
                existing.copy(
                    title = title.ifBlank { existing.title },
                    visitCount = existing.visitCount + 1,
                    lastVisitedAt = System.currentTimeMillis()
                )
            )
        } else {
            historyDao.insertOrReplace(
                HistoryEntity(
                    url = url,
                    title = title.ifBlank { UrlUtils.extractHost(url) },
                    visitCount = 1,
                    lastVisitedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteItem(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearSince(sinceTimestamp: Long) {
        historyDao.deleteSince(sinceTimestamp)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
