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

        assertEquals(6, menuItems.size)

        // Test item IDs
        val expectedIds = listOf("continue", "reset", "progress", "settings", "exit", "save_log")
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
    fun `test custom menu configuration works correctly`() {
        // Create custom menu configuration
        val customConfig =
                MenuConfigurationBuilder.create()
                        .menuId("test_menu")
                        .title("Test Menu")
                        .addItem(MenuItem("test1", "Test Item 1", action = MenuAction.CONTINUE))
                        .addItem(MenuItem("test2", "Test Item 2", action = MenuAction.RESET))
                        .defaultSelectedIndex(1)
                        .build()

        // Create fragment with custom configuration
        val fragment = RetroMenu3Fragment.newInstance(customConfig)

        // Verify configuration is applied
        val menuItems = fragment.getMenuItems()
        assertEquals(2, menuItems.size)
        assertEquals("test1", menuItems[0].id)
        assertEquals("Test Item 1", menuItems[0].title)
        assertEquals("test2", menuItems[1].id)
        assertEquals("Test Item 2", menuItems[1].title)
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
