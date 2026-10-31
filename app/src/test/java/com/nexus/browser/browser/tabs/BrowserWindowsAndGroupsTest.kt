package com.nexus.browser.browser.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserWindowsAndGroupsTest {

    @Test
    fun `browser tab default properties support window, group and pinning`() {
        val tab = BrowserTab(
            id = "tab-default",
            url = "https://fr.wikipedia.org",
            title = "Wikipédia"
        )

        assertEquals(BrowserWindow.MAIN_WINDOW_ID, tab.windowId)
        assertNull(tab.groupId)
        assertFalse(tab.isPinned)
        assertNull(tab.previewPath)
    }

    @Test
    fun `browser tab can be assigned to custom window, group and pinned`() {
        val tab = BrowserTab(
            id = "tab-custom",
            url = "https://github.com",
            title = "GitHub",
            windowId = "window-work",
            groupId = "group_42",
            isPinned = true,
            previewPath = "/data/user/0/com.nexus.browser/cache/previews/tab-custom.webp"
        )

        assertEquals("window-work", tab.windowId)
        assertEquals("group_42", tab.groupId)
        assertTrue(tab.isPinned)
        assertEquals("/data/user/0/com.nexus.browser/cache/previews/tab-custom.webp", tab.previewPath)
    }

    @Test
    fun `browser window model supports naming and privacy`() {
        val standardWindow = BrowserWindow(
            id = "win-1",
            name = "Recherche personnelle",
            isPrivate = false
        )
        val privateWindow = BrowserWindow(
            id = "win-2",
            name = "Session sécurisée",
            isPrivate = true
        )

        assertEquals("win-1", standardWindow.id)
        assertEquals("Recherche personnelle", standardWindow.name)
        assertFalse(standardWindow.isPrivate)

        assertEquals("win-2", privateWindow.id)
        assertEquals("Session sécurisée", privateWindow.name)
        assertTrue(privateWindow.isPrivate)
    }

    @Test
    fun `tab group model associates custom name and color badge`() {
        val devGroup = TabGroup(
            id = "group_101",
            name = "Développement",
            colorHex = "#2979FF"
        )

        assertEquals("group_101", devGroup.id)
        assertEquals("Développement", devGroup.name)
        assertEquals("#2979FF", devGroup.colorHex)
    }

    @Test
    fun `closed tab group preserves tabs snapshot for undo restoration`() {
        val group = TabGroup(id = "group_10", name = "Projets", colorHex = "#10B981")
        val tab1 = BrowserTab(id = "t1", url = "https://nexus.dev", title = "NEXUS", groupId = "group_10")
        val tab2 = BrowserTab(id = "t2", url = "https://kotlinlang.org", title = "Kotlin", groupId = "group_10")

        val closedGroup = ClosedTabGroup(
            group = group,
            tabs = listOf(tab1, tab2)
        )

        assertEquals("group_10", closedGroup.id)
        assertEquals("Projets", closedGroup.name)
        assertEquals(2, closedGroup.tabsCount)
        assertEquals("https://nexus.dev", closedGroup.tabs[0].url)
    }
}
