package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
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

    /** Mirrors AboutFragment's private capitalization logic, as an independent oracle. */
    private fun expectedCapitalization(style: Int, original: String): String =
            when (style) {
                1 ->
                        if (original.isNotEmpty()) {
                            original.substring(0, 1).uppercase() + original.substring(1)
                        } else {
                            original
                        }
                2 -> original.uppercase()
                else -> original
            }

    @Test
    fun `titulo e textos informativos respeitam a capitalizacao configurada`() {
        val style = activity.resources.getInteger(R.integer.rm_text_capitalization)
        val rawTitle = activity.resources.getString(R.string.about_menu_title)

        val title = fragment.requireView().findViewById<TextView>(R.id.about_title).text.toString()
        val projectInfo =
                fragment.requireView()
                        .findViewById<TextView>(R.id.project_name_info)
                        .text
                        .toString()

        // The static title (from a string resource never touched by populateAboutInfo()) and the
        // dynamically populated info text (already capitalized once inside populateAboutInfo())
        // must both reflect the configured style - proving the dedup onto the shared
        // applyConfiguredCapitalization() helper preserved both call sites' behavior. Reverting
        // the aboutTitle call site (the only line this test depends on to distinguish old vs new
        // wiring) makes the title assertion fail, since about_menu_title ("About") is not already
        // uppercase/first-letter-capitalized by default.
        assertEquals(expectedCapitalization(style, rawTitle), title)
        assertEquals(expectedCapitalization(style, projectInfo), projectInfo)
    }
}
