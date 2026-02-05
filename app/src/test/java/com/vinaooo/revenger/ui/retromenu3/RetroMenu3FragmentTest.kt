package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetroMenu3FragmentTest {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: RetroMenu3Fragment

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java)
                        .create()
                        .start()
                        .resume()
                        .get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = RetroMenu3Fragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "test_fragment")
                .commitNow()
    }

    @Test
    fun `test fragment instantiation without context`() {
        assertNotNull(fragment)
        assertTrue(fragment is MenuFragmentBase)
        assertTrue(fragment is MenuFragment)
    }

    @Test
    fun `test getMenuItems returns correct structure`() {
        val menuItems = fragment.getMenuItems()

        assertEquals(6, menuItems.size)

        // Test item IDs (including About menu)
        val expectedIds = listOf("continue", "reset", "progress", "settings", "about", "exit")
        menuItems.forEachIndexed { index, item ->
            assertEquals(expectedIds[index], item.id)
            assertFalse(item.title.isEmpty())
            assertNotNull(item.action)
        }
    }

    @Test
    fun `test menu item actions are properly defined`() {
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
