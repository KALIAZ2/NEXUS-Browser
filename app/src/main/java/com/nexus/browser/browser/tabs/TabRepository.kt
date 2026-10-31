package com.nexus.browser.browser.tabs

import com.nexus.browser.core.database.TabDao
import com.nexus.browser.core.database.TabEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabRepository @Inject constructor(
    private val tabDao: TabDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _recentlyClosedTabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<BrowserTab>> = _recentlyClosedTabs.asStateFlow()

    suspend fun initializeTabs(restoreOnStartup: Boolean, defaultHomeUrl: String) {
        if (restoreOnStartup) {
            val saved = tabDao.getPersistedTabs()
            if (saved.isNotEmpty()) {
                val restored = saved.map { entity ->
                    BrowserTab(
                        id = entity.id,
                        url = entity.url,
                        title = entity.title,
                        faviconUrl = entity.faviconUrl,
                        isPrivate = false,
                        isActive = entity.isActive,
                        position = entity.position,
                        lastAccessedAt = entity.lastAccessedAt,
                        sessionStateJson = entity.sessionStateJson
                    )
                }
                val active = restored.firstOrNull { it.isActive } ?: restored.first()
                _tabs.value = restored.map { it.copy(isActive = it.id == active.id) }
                _activeTabId.value = active.id
                return
            }
        } else {
            tabDao.clearAll()
        }
        createTab(url = defaultHomeUrl, isPrivate = false, select = true)
    }

    fun getTabById(tabId: String): BrowserTab? = _tabs.value.firstOrNull { it.id == tabId }

    fun getActiveTab(): BrowserTab? {
        val activeId = _activeTabId.value
        return _tabs.value.firstOrNull { it.id == activeId } ?: _tabs.value.firstOrNull()
    }

    fun createTab(
        url: String = "nexus://home",
        title: String = "Nouvel onglet",
        isPrivate: Boolean = false,
        select: Boolean = true,
        isDesktopMode: Boolean = false
    ): BrowserTab {
        val newId = UUID.randomUUID().toString()
        val position = _tabs.value.size
        val newTab = BrowserTab(
            id = newId,
            url = url,
            title = if (url == "nexus://home") "Nouvel onglet" else title,
            isPrivate = isPrivate,
            isActive = select,
            isDesktopMode = isDesktopMode,
            position = position,
            lastAccessedAt = System.currentTimeMillis()
        )

        _tabs.update { current ->
            val updated = if (select) current.map { it.copy(isActive = false) } else current
            updated + newTab
        }

        if (select) {
            _activeTabId.value = newId
        }

        if (!isPrivate) {
            persistNormalTabsAsync()
        }
        return newTab
    }

    fun duplicateTab(tabId: String): BrowserTab? {
        val source = getTabById(tabId) ?: return null
        return createTab(
            url = source.url,
            title = source.title,
            isPrivate = source.isPrivate,
            select = true,
            isDesktopMode = source.isDesktopMode
        )
    }

    fun selectTab(tabId: String) {
        val now = System.currentTimeMillis()
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(isActive = true, lastAccessedAt = now)
                } else {
                    tab.copy(isActive = false)
                }
            }
        }
        _activeTabId.value = tabId
        persistNormalTabsAsync()
    }

    fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) transform(tab) else tab
            }
        }
        persistNormalTabsAsync()
    }

    fun reorderTab(tabId: String, moveUp: Boolean) {
        _tabs.update { list ->
            val idx = list.indexOfFirst { it.id == tabId }
            if (idx == -1) return@update list
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (targetIdx !in list.indices) return@update list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(targetIdx, item)
            mutable.mapIndexed { index, browserTab -> browserTab.copy(position = index) }
        }
        persistNormalTabsAsync()
    }

    fun closeTab(tabId: String, defaultHomeUrl: String = "nexus://home"): String? {
        val current = _tabs.value
        val closingTab = current.firstOrNull { it.id == tabId }
        if (closingTab != null && !closingTab.isPrivate && closingTab.url != "nexus://home") {
            pushRecentlyClosed(closingTab)
        }

        val remaining = current.filterNot { it.id == tabId }
        scope.launch { tabDao.deleteTabById(tabId) }

        if (remaining.isEmpty()) {
            val replacement = createTab(url = defaultHomeUrl, isPrivate = false, select = true)
            return replacement.id
        }

        val wasActive = _activeTabId.value == tabId
        val nextActiveId = if (wasActive) {
            remaining.maxByOrNull { it.lastAccessedAt }?.id ?: remaining.last().id
        } else {
            _activeTabId.value ?: remaining.last().id
        }

        _tabs.value = remaining.mapIndexed { idx, tab ->
            tab.copy(position = idx, isActive = tab.id == nextActiveId)
        }
        _activeTabId.value = nextActiveId
        persistNormalTabsAsync()
        return nextActiveId
    }

    fun closeAllTabs(onlyPrivate: Boolean = false, defaultHomeUrl: String = "nexus://home") {
        if (onlyPrivate) {
            val remaining = _tabs.value.filterNot { it.isPrivate }
            if (remaining.isEmpty()) {
                _tabs.value = emptyList()
                createTab(url = defaultHomeUrl, isPrivate = false, select = true)
            } else {
                val active = remaining.first()
                _tabs.value = remaining.mapIndexed { index, tab ->
                    tab.copy(position = index, isActive = tab.id == active.id)
                }
                _activeTabId.value = active.id
            }
        } else {
            val normalNonHome = _tabs.value.filter { !it.isPrivate && it.url != "nexus://home" }
            normalNonHome.forEach { pushRecentlyClosed(it) }
            scope.launch { tabDao.clearAll() }
            _tabs.value = emptyList()
            createTab(url = defaultHomeUrl, isPrivate = false, select = true)
        }
    }

    fun restoreRecentlyClosedTab(tabId: String? = null): BrowserTab? {
        val currentClosed = _recentlyClosedTabs.value
        if (currentClosed.isEmpty()) return null
        val target = if (tabId != null) {
            currentClosed.firstOrNull { it.id == tabId } ?: return null
        } else {
            currentClosed.first()
        }
        _recentlyClosedTabs.value = currentClosed.filterNot { it.id == target.id }
        return createTab(
            url = target.url,
            title = target.title,
            isPrivate = false,
            select = true,
            isDesktopMode = target.isDesktopMode
        )
    }

    fun clearRecentlyClosedTabs() {
        _recentlyClosedTabs.value = emptyList()
    }

    private fun pushRecentlyClosed(tab: BrowserTab) {
        _recentlyClosedTabs.update { current ->
            val deduplicated = current.filterNot { it.url == tab.url }
            (listOf(tab.copy(isActive = false, isFullScreen = false)) + deduplicated).take(15)
        }
    }

    private fun persistNormalTabsAsync() {
        val snapshot = _tabs.value.filterNot { it.isPrivate }
        scope.launch {
            val entities = snapshot.mapIndexed { idx, tab ->
                TabEntity(
                    id = tab.id,
                    url = tab.url,
                    title = tab.title,
                    faviconUrl = tab.faviconUrl,
                    isPrivate = false,
                    isActive = tab.isActive,
                    position = idx,
                    lastAccessedAt = tab.lastAccessedAt,
                    sessionStateJson = tab.sessionStateJson
                )
            }
            tabDao.upsertTabs(entities)
        }
    }
}
