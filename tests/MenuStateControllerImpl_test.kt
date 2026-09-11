package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MenuStateControllerImpl gerencia seleção e navegação do menu principal. Um commit anterior do
 * projeto (`eddb8d9`) corrigiu uma duplicação de inicialização (`init {}` chamando
 * `initializeMenuItems()` duas vezes) mas seus testes nunca chegaram a ser versionados — `tests/`
 * estava gitignored na época. Este arquivo cobre a classe do zero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuStateControllerImpl_test {

    private lateinit var fragment: MenuFragmentBase
    private lateinit var animationController: MenuAnimationController
    private lateinit var menuViews: MenuViews
    private lateinit var controller: MenuStateControllerImpl

    private var selectedIndex = 0

    private val items =
        listOf(
            MenuItem(id = "continue", title = "Continue"),
            MenuItem(id = "reset", title = "Reset"),
            MenuItem(id = "progress", title = "Progress"),
            MenuItem(id = "settings", title = "Settings"),
            MenuItem(id = "about", title = "About"),
            MenuItem(id = "exit", title = "Exit"),
        )

    @Before
    fun setUp() {
        selectedIndex = 0

        fragment = mockk(relaxed = true)
        every { fragment.getMenuItems() } returns items
        every { fragment.getCurrentSelectedIndex() } answers { selectedIndex }
        every { fragment.setSelectedIndex(any()) } answers { selectedIndex = firstArg() }

        animationController = mockk(relaxed = true)

        menuViews =
            MenuViews(
                menuContainer = mockk<LinearLayout>(relaxed = true),
                continueMenu = mockk(relaxed = true),
                resetMenu = mockk(relaxed = true),
                progressMenu = mockk(relaxed = true),
                settingsMenu = mockk(relaxed = true),
                aboutMenu = mockk(relaxed = true),
                exitMenu = mockk(relaxed = true),
                menuItems = emptyList(),
                continueTitle = mockk<TextView>(relaxed = true),
                resetTitle = mockk<TextView>(relaxed = true),
                progressTitle = mockk<TextView>(relaxed = true),
                settingsTitle = mockk<TextView>(relaxed = true),
                aboutTitle = mockk<TextView>(relaxed = true),
                exitTitle = mockk<TextView>(relaxed = true),
                selectionArrowContinue = mockk<TextView>(relaxed = true),
                selectionArrowReset = mockk<TextView>(relaxed = true),
                selectionArrowProgress = mockk<TextView>(relaxed = true),
                selectionArrowSettings = mockk<TextView>(relaxed = true),
                selectionArrowAbout = mockk<TextView>(relaxed = true),
                selectionArrowExit = mockk<TextView>(relaxed = true),
                titleTextView = mockk<TextView>(relaxed = true),
            )

        controller = MenuStateControllerImpl(fragment, animationController)
    }

    // --- Seleção atual ---

    @Test
    fun `getCurrentSelection retorna o item correspondente ao indice atual do fragment`() {
        selectedIndex = 2

        assertEquals(items[2], controller.getCurrentSelection())
    }

    @Test
    fun `getCurrentSelection cai no primeiro item quando o indice esta fora dos limites`() {
        selectedIndex = 99

        assertEquals(items.first(), controller.getCurrentSelection())
    }

    @Test
    fun `getSelectedIndex e getTotalItems delegam ao fragment e a lista de itens`() {
        selectedIndex = 3

        assertEquals(3, controller.getSelectedIndex())
        assertEquals(items.size, controller.getTotalItems())
    }

    // --- Navegação sequencial (com wrap-around) ---

    @Test
    fun `selectNextItem avanca para o proximo indice`() {
        selectedIndex = 1

        val handled = controller.selectNextItem()

        assertTrue(handled)
        assertEquals(2, selectedIndex)
    }

    @Test
    fun `selectNextItem da a volta para o inicio ao passar do ultimo item`() {
        selectedIndex = items.size - 1

        val handled = controller.selectNextItem()

        assertTrue(handled)
        assertEquals(0, selectedIndex)
    }

    @Test
    fun `selectPreviousItem retrocede para o indice anterior`() {
        selectedIndex = 2

        val handled = controller.selectPreviousItem()

        assertTrue(handled)
        assertEquals(1, selectedIndex)
    }

    @Test
    fun `selectPreviousItem da a volta para o final ao retroceder do primeiro item`() {
        selectedIndex = 0

        val handled = controller.selectPreviousItem()

        assertTrue(handled)
        assertEquals(items.size - 1, selectedIndex)
    }

    // --- selectItemAt: validação de bordas ---

    @Test
    fun `selectItemAt com indice valido atualiza a selecao e retorna true`() {
        val handled = controller.selectItemAt(4)

        assertTrue(handled)
        assertEquals(4, selectedIndex)
    }

    @Test
    fun `selectItemAt com indice negativo nao atualiza a selecao e retorna false`() {
        selectedIndex = 2

        val handled = controller.selectItemAt(-1)

        assertFalse(handled)
        assertEquals(2, selectedIndex)
    }

    @Test
    fun `selectItemAt com indice alem do total de itens nao atualiza a selecao e retorna false`() {
        selectedIndex = 2

        val handled = controller.selectItemAt(items.size)

        assertFalse(handled)
        assertEquals(2, selectedIndex)
    }

    // --- Visual da seleção ---

    @Test
    fun `updateSelectionVisuals mostra apenas a seta do item selecionado e delega ao animation controller`() {
        selectedIndex = MenuStateControllerImpl.MENU_ITEM_SETTINGS
        controller.initializeState(menuViews)

        verify { menuViews.selectionArrowSettings.visibility = View.VISIBLE }
        verify { menuViews.selectionArrowContinue.visibility = View.GONE }
        verify { menuViews.selectionArrowReset.visibility = View.GONE }
        verify { menuViews.selectionArrowProgress.visibility = View.GONE }
        verify { menuViews.selectionArrowAbout.visibility = View.GONE }
        verify { menuViews.selectionArrowExit.visibility = View.GONE }
        verify { animationController.updateSelectionVisual(MenuStateControllerImpl.MENU_ITEM_SETTINGS) }
    }

    @Test
    fun `updateSelectionVisuals nao lanca e nao delega quando menuViews ainda nao foi inicializado`() {
        controller.updateSelectionVisuals()

        verify(exactly = 0) { animationController.updateSelectionVisual(any()) }
    }
}
