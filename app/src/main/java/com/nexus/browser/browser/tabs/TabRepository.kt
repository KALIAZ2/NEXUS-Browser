package com.nexus.browser.browser.tabs

import com.nexus.browser.core.database.BrowserWindowEntity
import com.nexus.browser.core.database.TabDao
import com.nexus.browser.core.database.TabEntity
import com.nexus.browser.core.database.TabGroupEntity
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

    private val defaultMainWindow = BrowserWindow(
        id = BrowserWindow.MAIN_WINDOW_ID,
        name = "Fenêtre 1",
        isPrivate = false,
        createdAt = 1L
    )
    private val defaultPrivateWindow = BrowserWindow(
        id = BrowserWindow.PRIVATE_WINDOW_ID,
        name = "Navigation privée",
        isPrivate = true,
        createdAt = 2L
    )

    private val _windows = MutableStateFlow(listOf(defaultMainWindow, defaultPrivateWindow))
    val windows: StateFlow<List<BrowserWindow>> = _windows.asStateFlow()

    private val _activeWindowId = MutableStateFlow(BrowserWindow.MAIN_WINDOW_ID)
    val activeWindowId: StateFlow<String> = _activeWindowId.asStateFlow()

    private val _tabGroups = MutableStateFlow<List<TabGroup>>(emptyList())
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _recentlyClosedTabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<BrowserTab>> = _recentlyClosedTabs.asStateFlow()

    private val _closedTabGroups = MutableStateFlow<List<ClosedTabGroup>>(emptyList())
    val closedTabGroups: StateFlow<List<ClosedTabGroup>> = _closedTabGroups.asStateFlow()

    private val _closedWindows = MutableStateFlow<List<ClosedBrowserWindow>>(emptyList())
    val closedWindows: StateFlow<List<ClosedBrowserWindow>> = _closedWindows.asStateFlow()

    suspend fun initializeTabs(restoreOnStartup: Boolean, defaultHomeUrl: String) {
        if (restoreOnStartup) {
            val savedWindows = tabDao.getAllWindows().map {
                BrowserWindow(
                    id = it.id,
                    name = it.name,
                    isPrivate = it.isPrivate,
                    createdAt = it.createdAt
                )
            }
            val mergedWindows = buildList {
                add(savedWindows.firstOrNull { it.id == BrowserWindow.MAIN_WINDOW_ID } ?: defaultMainWindow)
                savedWindows.filter { it.id != BrowserWindow.MAIN_WINDOW_ID && it.id != BrowserWindow.PRIVATE_WINDOW_ID && !it.isPrivate }
                    .forEach { add(it) }
                add(defaultPrivateWindow)
            }
            _windows.value = mergedWindows

            val savedGroups = tabDao.getAllTabGroups().map {
                TabGroup(
                    id = it.id,
                    name = it.name,
                    colorHex = it.colorHex,
                    windowId = it.windowId,
                    createdAt = it.createdAt
                )
            }
            _tabGroups.value = savedGroups

            val saved = tabDao.getPersistedTabs()
            if (saved.isNotEmpty()) {
                val validWindowIds = mergedWindows.map { it.id }.toSet()
                val validGroupIds = savedGroups.map { it.id }.toSet()
                val restored = saved.map { entity ->
                    val wId = if (entity.windowId in validWindowIds) entity.windowId else BrowserWindow.MAIN_WINDOW_ID
                    val gId = entity.groupId?.takeIf { it in validGroupIds }
                    BrowserTab(
                        id = entity.id,
                        url = entity.url,
                        title = entity.title,
                        faviconUrl = entity.faviconUrl,
                        isPrivate = false,
                        isActive = entity.isActive,
                        position = entity.position,
                        lastAccessedAt = entity.lastAccessedAt,
                        sessionStateJson = entity.sessionStateJson,
                        windowId = wId,
                        groupId = gId,
                        previewPath = entity.previewPath,
                        isPinned = entity.isPinned
                    )
                }
                val active = restored.firstOrNull { it.isActive } ?: restored.first()
                _tabs.value = restored.map { it.copy(isActive = it.id == active.id) }
                _activeTabId.value = active.id
                _activeWindowId.value = active.windowId
                return
            }
        } else {
            tabDao.clearAll()
            tabDao.clearAllTabGroups()
        }
        _windows.value = listOf(defaultMainWindow, defaultPrivateWindow)
        _activeWindowId.value = BrowserWindow.MAIN_WINDOW_ID
        createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = BrowserWindow.MAIN_WINDOW_ID)
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
        isDesktopMode: Boolean = false,
        windowId: String? = null,
        groupId: String? = null
    ): BrowserTab {
        val newId = UUID.randomUUID().toString()
        val resolvedWindowId = when {
            isPrivate -> BrowserWindow.PRIVATE_WINDOW_ID
            !windowId.isNullOrBlank() -> windowId
            _activeWindowId.value != BrowserWindow.PRIVATE_WINDOW_ID -> _activeWindowId.value
            else -> BrowserWindow.MAIN_WINDOW_ID
        }
        val position = _tabs.value.size
        val newTab = BrowserTab(
            id = newId,
            url = url,
            title = if (url == "nexus://home") "Nouvel onglet" else title,
            isPrivate = isPrivate,
            isActive = select,
            isDesktopMode = isDesktopMode,
            position = position,
            lastAccessedAt = System.currentTimeMillis(),
            windowId = resolvedWindowId,
            groupId = if (isPrivate) null else groupId
        )

        _tabs.update { current ->
            val updated = if (select) current.map { it.copy(isActive = false) } else current
            sortTabsStable(updated + newTab)
        }

        if (select) {
            _activeTabId.value = newId
            _activeWindowId.value = resolvedWindowId
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
            isDesktopMode = source.isDesktopMode,
            windowId = source.windowId,
            groupId = source.groupId
        )
    }

    fun selectTab(tabId: String) {
        val now = System.currentTimeMillis()
        var targetWindowId: String? = null
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    targetWindowId = tab.windowId
                    tab.copy(isActive = true, lastAccessedAt = now)
                } else {
                    tab.copy(isActive = false)
                }
            }
        }
        _activeTabId.value = tabId
        targetWindowId?.let { _activeWindowId.value = it }
        persistNormalTabsAsync()
    }

    /**
     * Permet de basculer vers l'onglet précédent (-1) ou suivant (+1) de la fenêtre active
     * lors d'un glissement horizontal sur la barre d'adresse ou de navigation.
     */
    fun switchAdjacentTab(offset: Int): BrowserTab? {
        val currentActive = getActiveTab() ?: return null
        val sameWindowTabs = _tabs.value.filter { it.windowId == currentActive.windowId }
        if (sameWindowTabs.size <= 1) return null
        val currentIndex = sameWindowTabs.indexOfFirst { it.id == currentActive.id }
        if (currentIndex == -1) return null
        val nextIndex = (currentIndex + offset).coerceIn(0, sameWindowTabs.lastIndex)
        if (nextIndex == currentIndex) return null
        val target = sameWindowTabs[nextIndex]
        selectTab(target.id)
        return getTabById(target.id)
    }

    fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) transform(tab) else tab
            }
        }
        persistNormalTabsAsync()
    }

    fun togglePinTab(tabId: String): Boolean {
        var isNowPinned = false
        _tabs.update { list ->
            val updated = list.map { tab ->
                if (tab.id == tabId) {
                    val newState = !tab.isPinned
                    isNowPinned = newState
                    tab.copy(isPinned = newState)
                } else tab
            }
            sortTabsStable(updated)
        }
        persistNormalTabsAsync()
        return isNowPinned
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

    // =========================================================================
    // Gestion des fenêtres de navigation (Workspaces)
    // =========================================================================

    fun createWindow(
        customName: String? = null,
        defaultHomeUrl: String = "nexus://home",
        select: Boolean = true
    ): BrowserWindow {
        val normalCount = _windows.value.count { !it.isPrivate }
        val windowName = customName?.trim()?.takeIf { it.isNotEmpty() } ?: "Fenêtre ${normalCount + 1}"
        val newWindow = BrowserWindow(
            id = "window_${UUID.randomUUID().toString().take(8)}",
            name = windowName,
            isPrivate = false,
            createdAt = System.currentTimeMillis()
        )
        _windows.update { current ->
            val normals = current.filter { !it.isPrivate } + newWindow
            val privates = current.filter { it.isPrivate }
            normals + privates
        }
        scope.launch {
            tabDao.upsertWindow(
                BrowserWindowEntity(
                    id = newWindow.id,
                    name = newWindow.name,
                    isPrivate = false,
                    createdAt = newWindow.createdAt
                )
            )
        }
        createTab(
            url = defaultHomeUrl,
            isPrivate = false,
            select = select,
            windowId = newWindow.id
        )
        return newWindow
    }

    fun renameWindow(windowId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || windowId == BrowserWindow.PRIVATE_WINDOW_ID) return
        _windows.update { list ->
            list.map { win ->
                if (win.id == windowId) win.copy(name = clean) else win
            }
        }
        val updated = _windows.value.firstOrNull { it.id == windowId } ?: return
        scope.launch {
            tabDao.upsertWindow(
                BrowserWindowEntity(
                    id = updated.id,
                    name = updated.name,
                    isPrivate = updated.isPrivate,
                    createdAt = updated.createdAt
                )
            )
        }
    }

    fun selectWindow(windowId: String, defaultHomeUrl: String = "nexus://home"): BrowserTab {
        _activeWindowId.value = windowId
        val isPrivateWindow = windowId == BrowserWindow.PRIVATE_WINDOW_ID
        val windowTabs = _tabs.value.filter { it.windowId == windowId }
        if (windowTabs.isNotEmpty()) {
            val mostRecent = windowTabs.maxByOrNull { it.lastAccessedAt } ?: windowTabs.first()
            selectTab(mostRecent.id)
            return getTabById(mostRecent.id) ?: mostRecent
        }
        return createTab(
            url = defaultHomeUrl,
            isPrivate = isPrivateWindow,
            select = true,
            windowId = windowId
        )
    }

    fun moveTabToWindow(tabId: String, targetWindowId: String) {
        if (targetWindowId == BrowserWindow.PRIVATE_WINDOW_ID) return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId && !tab.isPrivate) {
                    tab.copy(windowId = targetWindowId, groupId = null)
                } else {
                    tab
                }
            }
        }
        persistNormalTabsAsync()
    }

    fun closeWindow(windowId: String, defaultHomeUrl: String = "nexus://home"): String? {
        if (windowId == BrowserWindow.PRIVATE_WINDOW_ID) {
            closeAllTabs(onlyPrivate = true, defaultHomeUrl = defaultHomeUrl)
            return _activeTabId.value
        }

        val normalWindows = _windows.value.filter { !it.isPrivate }
        val closingWindow = normalWindows.firstOrNull { it.id == windowId } ?: return _activeTabId.value
        val windowTabs = _tabs.value.filter { it.windowId == windowId && !it.isPrivate }

        if (windowTabs.any { !it.isHomeTab }) {
            _closedWindows.update { current ->
                (listOf(ClosedBrowserWindow(window = closingWindow, tabs = windowTabs)) + current).take(5)
            }
            windowTabs.filter { !it.isHomeTab }.forEach { pushRecentlyClosed(it) }
        }

        // Si c'est la seule fenêtre normale, on réinitialise simplement ses onglets
        if (normalWindows.size <= 1) {
            scope.launch {
                tabDao.deleteTabsByWindowId(windowId)
                tabDao.deleteTabGroupsByWindowId(windowId)
            }
            _tabGroups.update { it.filterNot { g -> g.windowId == windowId } }
            _tabs.update { it.filterNot { t -> t.windowId == windowId } }
            val replacement = createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = windowId)
            return replacement.id
        }

        // Supprimer la fenêtre et basculer sur une autre fenêtre normale
        _windows.update { it.filterNot { w -> w.id == windowId } }
        _tabGroups.update { it.filterNot { g -> g.windowId == windowId } }
        val remainingTabs = _tabs.value.filterNot { it.windowId == windowId }
        scope.launch {
            tabDao.deleteWindowById(windowId)
            tabDao.deleteTabsByWindowId(windowId)
            tabDao.deleteTabGroupsByWindowId(windowId)
        }

        val fallbackWindow = _windows.value.firstOrNull { !it.isPrivate } ?: defaultMainWindow
        _activeWindowId.value = fallbackWindow.id

        val fallbackTab = remainingTabs.filter { it.windowId == fallbackWindow.id }
            .maxByOrNull { it.lastAccessedAt }
            ?: remainingTabs.firstOrNull { !it.isPrivate }

        if (fallbackTab == null) {
            _tabs.value = remainingTabs
            val created = createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = fallbackWindow.id)
            return created.id
        }

        _tabs.value = remainingTabs.mapIndexed { idx, tab ->
            tab.copy(position = idx, isActive = tab.id == fallbackTab.id)
        }
        _activeTabId.value = fallbackTab.id
        persistNormalTabsAsync()
        return fallbackTab.id
    }

    fun restoreClosedWindow(windowId: String? = null): BrowserWindow? {
        val currentClosed = _closedWindows.value
        if (currentClosed.isEmpty()) return null
        val target = if (windowId != null) {
            currentClosed.firstOrNull { it.window.id == windowId } ?: return null
        } else {
            currentClosed.first()
        }
        _closedWindows.value = currentClosed.filterNot { it.window.id == target.window.id }

        val restoredWin = target.window.copy(createdAt = System.currentTimeMillis())
        _windows.update { current ->
            val exists = current.any { it.id == restoredWin.id }
            if (exists) current else {
                val normals = current.filter { !it.isPrivate } + restoredWin
                val privates = current.filter { it.isPrivate }
                normals + privates
            }
        }
        scope.launch {
            tabDao.upsertWindow(
                BrowserWindowEntity(
                    id = restoredWin.id,
                    name = restoredWin.name,
                    isPrivate = false,
                    createdAt = restoredWin.createdAt
                )
            )
        }

        var lastTab: BrowserTab? = null
        target.tabs.forEachIndexed { idx, closedTab ->
            lastTab = createTab(
                url = closedTab.url,
                title = closedTab.title,
                isPrivate = false,
                select = idx == target.tabs.lastIndex,
                isDesktopMode = closedTab.isDesktopMode,
                windowId = restoredWin.id
            )
        }
        lastTab?.let { selectTab(it.id) }
        return restoredWin
    }

    // =========================================================================
    // Gestion des groupes d'onglets (Tab Groups)
    // =========================================================================

    fun createTabGroup(
        name: String,
        colorHex: String = "#00E5FF",
        windowId: String = _activeWindowId.value.takeIf { it != BrowserWindow.PRIVATE_WINDOW_ID } ?: BrowserWindow.MAIN_WINDOW_ID,
        initialTabIds: List<String> = emptyList()
    ): TabGroup {
        val cleanName = name.trim().ifEmpty { "Groupe ${_tabGroups.value.size + 1}" }
        val group = TabGroup(
            id = "group_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            colorHex = colorHex,
            windowId = windowId,
            createdAt = System.currentTimeMillis()
        )
        _tabGroups.update { it + group }
        scope.launch {
            tabDao.upsertTabGroup(
                TabGroupEntity(
                    id = group.id,
                    name = group.name,
                    colorHex = group.colorHex,
                    windowId = group.windowId,
                    createdAt = group.createdAt
                )
            )
        }
        if (initialTabIds.isNotEmpty()) {
            val idSet = initialTabIds.toSet()
            _tabs.update { list ->
                list.map { tab ->
                    if (tab.id in idSet && !tab.isPrivate) {
                        tab.copy(groupId = group.id, windowId = windowId)
                    } else {
                        tab
                    }
                }
            }
            persistNormalTabsAsync()
        }
        return group
    }

    fun updateTabGroup(groupId: String, name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        _tabGroups.update { list ->
            list.map { group ->
                if (group.id == groupId) group.copy(name = cleanName, colorHex = colorHex) else group
            }
        }
        val updated = _tabGroups.value.firstOrNull { it.id == groupId } ?: return
        scope.launch {
            tabDao.upsertTabGroup(
                TabGroupEntity(
                    id = updated.id,
                    name = updated.name,
                    colorHex = updated.colorHex,
                    windowId = updated.windowId,
                    createdAt = updated.createdAt
                )
            )
        }
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        val group = groupId?.let { id -> _tabGroups.value.firstOrNull { it.id == id } }
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId && !tab.isPrivate) {
                    tab.copy(
                        groupId = group?.id,
                        windowId = group?.windowId ?: tab.windowId
                    )
                } else {
                    tab
                }
            }
        }
        persistNormalTabsAsync()
    }

    fun closeTabGroup(groupId: String, defaultHomeUrl: String = "nexus://home"): List<String> {
        val group = _tabGroups.value.firstOrNull { it.id == groupId } ?: return emptyList()
        val groupTabs = _tabs.value.filter { it.groupId == groupId }
        val closedTabIds = groupTabs.map { it.id }

        if (groupTabs.isNotEmpty()) {
            _closedTabGroups.update { current ->
                (listOf(ClosedTabGroup(group = group, tabs = groupTabs)) + current).take(8)
            }
            groupTabs.filter { !it.isHomeTab }.forEach { pushRecentlyClosed(it) }
        }

        _tabGroups.update { it.filterNot { g -> g.id == groupId } }
        val remaining = _tabs.value.filterNot { it.groupId == groupId }

        scope.launch {
            tabDao.deleteTabGroupById(groupId)
            closedTabIds.forEach { id -> tabDao.deleteTabById(id) }
        }

        if (remaining.none { it.windowId == group.windowId }) {
            _tabs.value = remaining
            createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = group.windowId)
        } else {
            val activeStillPresent = remaining.any { it.id == _activeTabId.value }
            val nextActive = if (activeStillPresent) {
                _activeTabId.value ?: remaining.first().id
            } else {
                remaining.filter { it.windowId == group.windowId }
                    .maxByOrNull { it.lastAccessedAt }?.id
                    ?: remaining.first().id
            }
            _tabs.value = remaining.mapIndexed { idx, tab ->
                tab.copy(position = idx, isActive = tab.id == nextActive)
            }
            _activeTabId.value = nextActive
            persistNormalTabsAsync()
        }
        return closedTabIds
    }

    fun restoreClosedTabGroup(groupId: String? = null): TabGroup? {
        val currentClosed = _closedTabGroups.value
        if (currentClosed.isEmpty()) return null
        val target = if (groupId != null) {
            currentClosed.firstOrNull { it.group.id == groupId } ?: return null
        } else {
            currentClosed.first()
        }
        _closedTabGroups.value = currentClosed.filterNot { it.group.id == target.group.id }

        val validWindowId = if (_windows.value.any { it.id == target.group.windowId }) {
            target.group.windowId
        } else {
            BrowserWindow.MAIN_WINDOW_ID
        }
        val restoredGroup = target.group.copy(windowId = validWindowId)
        _tabGroups.update { current ->
            if (current.any { it.id == restoredGroup.id }) current else current + restoredGroup
        }
        scope.launch {
            tabDao.upsertTabGroup(
                TabGroupEntity(
                    id = restoredGroup.id,
                    name = restoredGroup.name,
                    colorHex = restoredGroup.colorHex,
                    windowId = restoredGroup.windowId,
                    createdAt = restoredGroup.createdAt
                )
            )
        }

        var lastRestored: BrowserTab? = null
        target.tabs.forEachIndexed { idx, tab ->
            lastRestored = createTab(
                url = tab.url,
                title = tab.title,
                isPrivate = false,
                select = idx == target.tabs.lastIndex,
                isDesktopMode = tab.isDesktopMode,
                windowId = validWindowId,
                groupId = restoredGroup.id
            )
        }
        lastRestored?.let { selectTab(it.id) }
        return restoredGroup
    }

    // =========================================================================
    // Fermeture et restauration d'onglets individuels
    // =========================================================================

    fun closeTab(tabId: String, defaultHomeUrl: String = "nexus://home"): String? {
        val current = _tabs.value
        val closingTab = current.firstOrNull { it.id == tabId }
        val closingWindowId = closingTab?.windowId ?: BrowserWindow.MAIN_WINDOW_ID
        if (closingTab != null && !closingTab.isPrivate && closingTab.url != "nexus://home") {
            pushRecentlyClosed(closingTab)
        }

        val remaining = current.filterNot { it.id == tabId }
        scope.launch { tabDao.deleteTabById(tabId) }

        val remainingInSameWindow = remaining.filter { it.windowId == closingWindowId }
        if (remainingInSameWindow.isEmpty() && closingWindowId != BrowserWindow.PRIVATE_WINDOW_ID) {
            _tabs.value = remaining
            val replacement = createTab(
                url = defaultHomeUrl,
                isPrivate = false,
                select = true,
                windowId = closingWindowId
            )
            return replacement.id
        }

        if (remaining.isEmpty()) {
            val replacement = createTab(url = defaultHomeUrl, isPrivate = false, select = true)
            return replacement.id
        }

        val wasActive = _activeTabId.value == tabId
        val nextActiveId = if (wasActive) {
            remainingInSameWindow.maxByOrNull { it.lastAccessedAt }?.id
                ?: remaining.maxByOrNull { it.lastAccessedAt }?.id
                ?: remaining.last().id
        } else {
            _activeTabId.value ?: remaining.last().id
        }

        _tabs.value = sortTabsStable(remaining).mapIndexed { idx, tab ->
            tab.copy(position = idx, isActive = tab.id == nextActiveId)
        }
        _activeTabId.value = nextActiveId
        _tabs.value.firstOrNull { it.id == nextActiveId }?.let {
            _activeWindowId.value = it.windowId
        }
        persistNormalTabsAsync()
        return nextActiveId
    }

    fun closeAllTabs(
        onlyPrivate: Boolean = false,
        defaultHomeUrl: String = "nexus://home",
        windowId: String? = null
    ) {
        if (onlyPrivate) {
            val remaining = _tabs.value.filterNot { it.isPrivate }
            if (remaining.isEmpty()) {
                _tabs.value = emptyList()
                createTab(url = defaultHomeUrl, isPrivate = false, select = true)
            } else {
                val active = remaining.maxByOrNull { it.lastAccessedAt } ?: remaining.first()
                _tabs.value = remaining.mapIndexed { index, tab ->
                    tab.copy(position = index, isActive = tab.id == active.id)
                }
                _activeTabId.value = active.id
                _activeWindowId.value = active.windowId
            }
        } else if (windowId != null) {
            val targetTabs = _tabs.value.filter { it.windowId == windowId && !it.isPrivate }
            targetTabs.filter { !it.isHomeTab }.forEach { pushRecentlyClosed(it) }
            val remaining = _tabs.value.filterNot { it.windowId == windowId && !it.isPrivate }
            scope.launch { tabDao.deleteTabsByWindowId(windowId) }
            _tabs.value = remaining
            createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = windowId)
        } else {
            val normalNonHome = _tabs.value.filter { !it.isPrivate && it.url != "nexus://home" }
            normalNonHome.forEach { pushRecentlyClosed(it) }
            scope.launch { tabDao.clearAll() }
            _tabs.value = emptyList()
            createTab(url = defaultHomeUrl, isPrivate = false, select = true, windowId = BrowserWindow.MAIN_WINDOW_ID)
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
        val targetWindowId = if (_windows.value.any { it.id == target.windowId && !it.isPrivate }) {
            target.windowId
        } else {
            BrowserWindow.MAIN_WINDOW_ID
        }
        val targetGroupId = target.groupId?.takeIf { gId -> _tabGroups.value.any { it.id == gId } }
        return createTab(
            url = target.url,
            title = target.title,
            isPrivate = false,
            select = true,
            isDesktopMode = target.isDesktopMode,
            windowId = targetWindowId,
            groupId = targetGroupId
        )
    }

    fun clearRecentlyClosedTabs() {
        _recentlyClosedTabs.value = emptyList()
        _closedTabGroups.value = emptyList()
        _closedWindows.value = emptyList()
    }

    private fun pushRecentlyClosed(tab: BrowserTab) {
        _recentlyClosedTabs.update { current ->
            val deduplicated = current.filterNot { it.url == tab.url }
            (listOf(tab.copy(isActive = false, isFullScreen = false)) + deduplicated).take(20)
        }
    }

    private fun sortTabsStable(list: List<BrowserTab>): List<BrowserTab> {
        val pinned = list.filter { it.isPinned }
        val unpinned = list.filterNot { it.isPinned }
        return (pinned + unpinned).mapIndexed { index, tab -> tab.copy(position = index) }
    }

    private fun persistNormalTabsAsync() {
        val snapshot = _tabs.value.filterNot { it.isPrivate }
        val windowsSnapshot = _windows.value.filterNot { it.isPrivate }
        scope.launch {
            val windowEntities = windowsSnapshot.map { win ->
                BrowserWindowEntity(
                    id = win.id,
                    name = win.name,
                    isPrivate = false,
                    createdAt = win.createdAt
                )
            }
            tabDao.upsertWindows(windowEntities)

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
                    sessionStateJson = tab.sessionStateJson,
                    windowId = tab.windowId,
                    groupId = tab.groupId,
                    previewPath = tab.previewPath,
                    isPinned = tab.isPinned
                )
            }
            tabDao.upsertTabs(entities)
        }
    }
}
