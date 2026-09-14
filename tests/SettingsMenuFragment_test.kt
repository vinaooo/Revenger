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

/**
 * Robolectric tests for SettingsMenuFragment, following the pattern proven by MenuIntegration_test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsMenuFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: SettingsMenuFragment

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = SettingsMenuFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "settings")
                .commitNow()
    }

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment inherits from MenuFragmentBase`() {
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `menu de Settings tem 4 items`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(4, menuItems.size)
    }

    @Test
    fun `menu items tem IDs e acoes corretos`() {
        val menuItems = fragment.getMenuItems()
        val expected =
                listOf(
                        "sound" to MenuAction.TOGGLE_AUDIO,
                        "shader" to MenuAction.TOGGLE_SHADER,
                        "speed" to MenuAction.TOGGLE_SPEED,
                        "back" to MenuAction.BACK
                )
        menuItems.forEachIndexed { index, item ->
            assertEquals(expected[index], item.id to item.action)
        }
    }

    @Test
    fun `menu items tem titulos nao vazios`() {
        fragment.getMenuItems().forEach { item -> assertFalse(item.title.isEmpty()) }
    }

    @Test
    fun `selecionar item back nao lanca excecao`() {
        val backItem = fragment.getMenuItems().first { it.id == "back" }
        try {
            fragment.onMenuItemSelected(backItem)
            assertTrue(true)
        } catch (e: Exception) {
            fail("onMenuItemSelected(back) should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `fragment pode ser destruido sem lancar excecao`() {
        try {
            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            assertTrue(true)
        } catch (e: Exception) {
            fail("Destroying the fragment should not throw exception: ${e.message}")
        }
    }
}
