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
 * Robolectric tests for ExitFragment, following the pattern proven by MenuIntegration_test.
 *
 * Note: the destructive SAVE_AND_EXIT/EXIT actions are intentionally not exercised here -
 * they trigger real save/process-kill side effects. These tests only assert menu structure
 * and safe wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExitFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: ExitFragment

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = ExitFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "exit")
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
    fun `menu do Exit tem 3 items`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(3, menuItems.size)
    }

    @Test
    fun `menu items tem IDs e acoes corretos`() {
        val menuItems = fragment.getMenuItems()
        assertEquals("save_exit" to MenuAction.SAVE_AND_EXIT, menuItems[0].id to menuItems[0].action)
        assertEquals("exit_no_save" to MenuAction.EXIT, menuItems[1].id to menuItems[1].action)
        assertEquals("back" to MenuAction.BACK, menuItems[2].id to menuItems[2].action)
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

    // Regression test for the narrowed IllegalStateException catch in
    // MenuFragmentBase.onPause(): after the fragment is removed from the FragmentManager,
    // requireActivity() throws IllegalStateException ("Fragment ... not attached to a context"),
    // which the catch must swallow and log instead of propagating. A mis-narrowed catch type
    // would let the exception escape and fail this test.
    @Test
    fun `onPause apos fragment ser removido nao lanca excecao`() {
        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        try {
            fragment.onPause()
        } catch (e: Exception) {
            fail("onPause() should not throw after the fragment is detached: ${e.message}")
        }
    }
}
