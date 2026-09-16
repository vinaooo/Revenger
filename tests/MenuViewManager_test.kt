package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuViewManager] owns initial setup and visual state of the RetroMenu3 main menu's views:
 * selection highlighting (color/arrow), dim/hide/show of the whole menu, and hiding just its
 * texts while a submenu is open. It had zero test coverage before this file.
 *
 * Uses the real `retro_menu3.xml` layout (inflated via a bare attached `Fragment`, so
 * `fragment.requireContext()`/`fragment.resources` work) rather than mocked views, so assertions
 * check real `View` state (visibility, alpha, text color) instead of just "a method was called".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuViewManager_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment
    private lateinit var view: View
    private lateinit var manager: MenuViewManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.retro_menu3, null)
        manager = MenuViewManager(fragment)
    }

    private fun selectedColor() =
            androidx.core.content.ContextCompat.getColor(activity, R.color.rm_selected_color)
    private fun normalColor() =
            androidx.core.content.ContextCompat.getColor(activity, R.color.rm_normal_color)

    // --- isViewsInitialized / setupViews ---

    @Test
    fun `isViewsInitialized e falso antes de setupViews e verdadeiro depois`() {
        assertFalse(manager.isViewsInitialized())

        manager.setupViews(view)

        assertTrue(manager.isViewsInitialized())
    }

    @Test
    fun `setupViews monta os 6 itens de menu na ordem esperada`() {
        manager.setupViews(view)

        val items = manager.getMenuItems()
        assertEquals(6, items.size)
        assertEquals(
                listOf(
                        R.id.menu_continue,
                        R.id.menu_reset,
                        R.id.menu_submenu2,
                        R.id.menu_submenu1,
                        R.id.menu_about,
                        R.id.menu_exit
                ),
                items.map { it.id }
        )
    }

    @Test
    fun `setupViews seleciona o primeiro item (Continue) por padrao`() {
        manager.setupViews(view)

        val continueItem = manager.getMenuItemViews()[0]
        val resetItem = manager.getMenuItemViews()[1]

        assertEquals(View.VISIBLE, continueItem.arrowTextView.visibility)
        assertEquals(selectedColor(), continueItem.titleTextView.currentTextColor)
        assertEquals(View.GONE, resetItem.arrowTextView.visibility)
        assertEquals(normalColor(), resetItem.titleTextView.currentTextColor)
    }

    // --- updateSelectionVisual ---

    @Test
    fun `updateSelectionVisual marca apenas o indice informado como selecionado`() {
        manager.setupViews(view)

        manager.updateSelectionVisual(3)

        manager.getMenuItemViews().forEachIndexed { index, item ->
            if (index == 3) {
                assertEquals(View.VISIBLE, item.arrowTextView.visibility)
                assertEquals(selectedColor(), item.titleTextView.currentTextColor)
            } else {
                assertEquals(View.GONE, item.arrowTextView.visibility)
                assertEquals(normalColor(), item.titleTextView.currentTextColor)
            }
        }
    }

    @Test
    fun `updateSelectionVisual movendo a selecao desmarca o item anterior`() {
        manager.setupViews(view)
        manager.updateSelectionVisual(1)

        manager.updateSelectionVisual(5)

        val previous = manager.getMenuItemViews()[1]
        val current = manager.getMenuItemViews()[5]
        assertEquals(View.GONE, previous.arrowTextView.visibility)
        assertEquals(View.VISIBLE, current.arrowTextView.visibility)
    }

    // --- dim / restore / hide / show (contêiner inteiro) ---

    @Test
    fun `dimMainMenu reduz o alpha e restoreMainMenu o restaura para 1_0`() {
        manager.setupViews(view)

        manager.dimMainMenu()
        assertEquals(0.3f, view.findViewById<View>(R.id.menu_container).alpha, 0.001f)

        manager.restoreMainMenu()
        assertEquals(1.0f, view.findViewById<View>(R.id.menu_container).alpha, 0.001f)
    }

    @Test
    fun `hideMainMenu torna o container INVISIBLE e showMainMenu o torna VISIBLE com alpha 1_0`() {
        manager.setupViews(view)
        val container = view.findViewById<View>(R.id.menu_container)

        manager.hideMainMenu()
        assertEquals(View.INVISIBLE, container.visibility)

        manager.showMainMenu()
        assertEquals(View.VISIBLE, container.visibility)
        assertEquals(1.0f, container.alpha, 0.001f)
    }

    @Test
    fun `hideMainMenuCompletely oculta o container, todos os textos e setas, e o titulo`() {
        manager.setupViews(view)
        manager.setupDynamicTitle(view)

        manager.hideMainMenuCompletely()

        val container = view.findViewById<View>(R.id.menu_container)
        assertEquals(View.INVISIBLE, container.visibility)
        manager.getMenuItemViews().forEach {
            assertEquals(View.INVISIBLE, it.titleTextView.visibility)
            assertEquals(View.INVISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.INVISIBLE, view.findViewById<View>(R.id.menu_title).visibility)
    }

    // --- hide/show apenas dos textos (submenu aberto) ---

    @Test
    fun `hideMainMenuTexts oculta textos, setas e titulo sem alterar o container`() {
        manager.setupViews(view)
        manager.setupDynamicTitle(view)
        val container = view.findViewById<View>(R.id.menu_container)

        manager.hideMainMenuTexts()

        assertEquals(View.VISIBLE, container.visibility)
        manager.getMenuItemViews().forEach {
            assertEquals(View.INVISIBLE, it.titleTextView.visibility)
            assertEquals(View.INVISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.INVISIBLE, view.findViewById<View>(R.id.menu_title).visibility)
    }

    @Test
    fun `showMainMenuTexts reverte hideMainMenuTexts`() {
        manager.setupViews(view)
        manager.setupDynamicTitle(view)
        manager.hideMainMenuTexts()

        manager.showMainMenuTexts()

        manager.getMenuItemViews().forEach {
            assertEquals(View.VISIBLE, it.titleTextView.visibility)
            assertEquals(View.VISIBLE, it.arrowTextView.visibility)
        }
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.menu_title).visibility)
    }

    // --- setupDynamicTitle ---

    @Test
    fun `setupDynamicTitle define o texto do titulo conforme o estilo configurado`() {
        manager.setupDynamicTitle(view)

        val titleStyle = fragment.resources.getInteger(R.integer.rm_title_style)
        val expected =
                if (titleStyle == 1) fragment.resources.getString(R.string.name)
                else fragment.resources.getString(R.string.rm_title)
        assertEquals(expected, view.findViewById<android.widget.TextView>(R.id.menu_title).text)
    }

    // --- guardas defensivas: chamadas antes de setupViews nao devem lancar ---

    @Test
    fun `metodos de visibilidade nao lancam quando chamados antes de setupViews`() {
        manager.dimMainMenu()
        manager.restoreMainMenu()
        manager.hideMainMenu()
        manager.showMainMenu()
        manager.hideMainMenuCompletely()
        manager.hideMainMenuTexts()
        manager.showMainMenuTexts()
    }
}
