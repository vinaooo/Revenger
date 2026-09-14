package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for AboutFragment, following the pattern proven by MenuIntegration_test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AboutFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: AboutFragment
    private lateinit var listener: AboutListener

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = AboutFragment.newInstance()
        listener = mockk<AboutListener>(relaxed = true)
        fragment.setAboutListener(listener)
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "about")
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
    fun `menu do About tem 2 items`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(2, menuItems.size)
    }

    @Test
    fun `menu items tem IDs corretos`() {
        val menuItems = fragment.getMenuItems()
        assertEquals("core_variables", menuItems[0].id)
        assertEquals("back", menuItems[1].id)
    }

    @Test
    fun `menu items tem titulos nao vazios`() {
        fragment.getMenuItems().forEach { item -> assertFalse(item.title.isEmpty()) }
    }

    @Test
    fun `selecionar item back aciona o listener`() {
        val backItem = fragment.getMenuItems().first { it.id == "back" }
        fragment.onMenuItemSelected(backItem)
        verify { listener.onAboutBackToMainMenu() }
    }
}
