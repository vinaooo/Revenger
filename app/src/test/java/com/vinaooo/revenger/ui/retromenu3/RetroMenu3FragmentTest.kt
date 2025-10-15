package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.*
import org.junit.Test

class RetroMenu3FragmentTest {

    @Test
    fun `test fragment instantiation without context`() {
        val fragment = RetroMenu3Fragment.newInstance()
        assertNotNull(fragment)
        assertTrue(fragment is MenuFragmentBase)
        assertTrue(fragment is MenuFragment)
    }

    @Test
    fun `test getMenuItems returns correct structure`() {
        val fragment = RetroMenu3Fragment.newInstance()
        val menuItems = fragment.getMenuItems()

        assertEquals(5, menuItems.size)

        // Test item IDs
        val expectedIds = listOf("continue", "reset", "progress", "settings", "exit")
        menuItems.forEachIndexed { index, item ->
            assertEquals(expectedIds[index], item.id)
            assertFalse(item.title.isEmpty())
            assertNotNull(item.action)
        }
    }

    @Test
    fun `test menu item actions are properly defined`() {
        val fragment = RetroMenu3Fragment.newInstance()
        val menuItems = fragment.getMenuItems()

        // Verify all items have non-empty properties
        menuItems.forEach { item ->
            assertFalse(item.id.isEmpty())
            assertFalse(item.title.isEmpty())
            assertNotNull(item.action)
        }
    }

    @Test
    fun `test fragment creation with default configuration`() {
        // Create fragment with default configuration
        val fragment = RetroMenu3Fragment.newInstance()

        // Verify fragment is created successfully
        assertNotNull(fragment)
    }

    @Test
    fun `test menu configuration builder creates valid configuration`() {
        val config = MenuConfigurationBuilder.createMainMenu().build()

        assertEquals("main_menu", config.menuId)
        assertEquals("RetroMenu3", config.title)
        assertEquals(6, config.items.size)
        assertTrue(config.isValid())
        assertEquals(0, config.defaultSelectedIndex)
    }

    @Test
    fun `test menu configuration validation works correctly`() {
        // Valid configuration
        val validConfig =
                MenuConfiguration(
                        menuId = "test",
                        title = "Test",
                        items = listOf(MenuItem("item1", "Item 1", action = MenuAction.CONTINUE))
                )
        assertTrue(validConfig.isValid())

        // Invalid configuration - empty menuId
        val invalidConfig =
                MenuConfiguration(
                        menuId = "",
                        title = "Test",
                        items = listOf(MenuItem("item1", "Item 1", action = MenuAction.CONTINUE))
                )
        assertFalse(invalidConfig.isValid())
    }
}
