package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MainMenuVisibilityController] was extracted out of [MenuViewManager] (show/hide/dim of the
 * main menu container), which had zero direct coverage of this behavior before
 * [MenuViewManager_test]. This file covers the controller in isolation, off plain Android views
 * rather than the full `retro_menu3.xml` layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MainMenuVisibilityController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var container: LinearLayout
    private lateinit var title: TextView
    private lateinit var itemViews: List<MenuItemView>
    private lateinit var controller: MainMenuVisibilityController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()
        val context = fragment.requireContext()

        container = LinearLayout(context)
        title = TextView(context)
        itemViews =
                listOf(
                        MenuItemView(TextView(context), TextView(context), RetroCardView(context)),
                        MenuItemView(TextView(context), TextView(context), RetroCardView(context))
                )
        controller = MainMenuVisibilityController()
    }

    // --- guards: nothing attached yet ---

    @Test
    fun `metodos nao lancam quando nada foi anexado ainda`() {
        controller.dimMainMenu()
        controller.restoreMainMenu()
        controller.hideMainMenu()
        controller.showMainMenu()
        controller.hideMainMenuCompletely()
        controller.hideMainMenuTexts()
        controller.showMainMenuTexts()
    }

    // --- dim / restore ---

    @Test
    fun `dimMainMenu reduz o alpha e restoreMainMenu o restaura para 1_0`() {
        controller.attachContainer(container, itemViews)

        controller.dimMainMenu()
        assertEquals(0.3f, container.alpha, 0.001f)

        controller.restoreMainMenu()
        assertEquals(1.0f, container.alpha, 0.001f)
    }

    // --- hide / show (whole container) ---

    @Test
    fun `hideMainMenu torna o container INVISIBLE e showMainMenu o torna VISIBLE com alpha 1_0`() {
        controller.attachContainer(container, itemViews)

        controller.hideMainMenu()
        assertEquals(View.INVISIBLE, container.visibility)

        controller.showMainMenu()
        assertEquals(View.VISIBLE, container.visibility)
        assertEquals(1.0f, container.alpha, 0.001f)
    }

    @Test
    fun `hideMainMenuCompletely oculta o container, todos os textos e setas, e o titulo`() {
        controller.attachContainer(container, itemViews)
        controller.attachTitle(title)

        controller.hideMainMenuCompletely()

        assertEquals(View.INVISIBLE, container.visibility)
        itemViews.forEach {
            assertEquals(View.INVISIBLE, it.titleTextView.visibility)
            assertEquals(View.INVISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.INVISIBLE, title.visibility)
    }

    // --- hide/show apenas dos textos (submenu aberto) ---

    @Test
    fun `hideMainMenuTexts oculta textos, setas e titulo sem alterar o container`() {
        controller.attachContainer(container, itemViews)
        controller.attachTitle(title)
        container.visibility = View.VISIBLE

        controller.hideMainMenuTexts()

        assertEquals(View.VISIBLE, container.visibility)
        itemViews.forEach {
            assertEquals(View.INVISIBLE, it.titleTextView.visibility)
            assertEquals(View.INVISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.INVISIBLE, title.visibility)
    }

    @Test
    fun `showMainMenuTexts reverte hideMainMenuTexts`() {
        controller.attachContainer(container, itemViews)
        controller.attachTitle(title)
        controller.hideMainMenuTexts()

        controller.showMainMenuTexts()

        itemViews.forEach {
            assertEquals(View.VISIBLE, it.titleTextView.visibility)
            assertEquals(View.VISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.VISIBLE, title.visibility)
    }

    @Test
    fun `attachTitle pode ser chamado independentemente e depois de attachContainer`() {
        controller.attachContainer(container, itemViews)
        controller.attachTitle(null)
        controller.attachTitle(title)

        controller.hideMainMenuCompletely()

        assertEquals(View.INVISIBLE, title.visibility)
    }
}
