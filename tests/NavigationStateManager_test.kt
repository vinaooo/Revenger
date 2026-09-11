package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * NavigationStateManager é a fonte única de verdade do estado de navegação do menu (menu atual,
 * índice selecionado, pilha de histórico, fragment ativo). [NavigationController.syncState],
 * [NavigationController.saveState] e [NavigationController.restoreState] são wrappers finos sobre
 * esta classe — a sincronização de estado após rotação de tela (um bug já corrigido no histórico
 * do projeto) depende inteiramente do round-trip de [saveState]/[restoreState] testado aqui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigationStateManager_test {

    private lateinit var stateManager: NavigationStateManager

    @Before
    fun setUp() {
        stateManager = NavigationStateManager()
    }

    // --- Estado inicial ---

    @Test
    fun `estado inicial comeca em MAIN sem fragment e pilha vazia`() {
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertEquals(0, stateManager.selectedItemIndex)
        assertNull(stateManager.currentFragment)
        assertTrue(stateManager.isStackEmpty())
        assertEquals(0, stateManager.getStackSize())
    }

    // --- updateSelectedIndex: validação de bordas ---

    @Test(expected = IllegalArgumentException::class)
    fun `updateSelectedIndex lanca excecao para indice negativo`() {
        stateManager.updateSelectedIndex(-1)
    }

    @Test
    fun `updateSelectedIndex aceita indice zero`() {
        stateManager.updateSelectedIndex(0)

        assertEquals(0, stateManager.selectedItemIndex)
    }

    // --- Pilha de navegação (push/pop) ---

    @Test
    fun `pushCurrentState e popState seguem ordem LIFO`() {
        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(2)
        stateManager.pushCurrentState()

        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.updateSelectedIndex(0)
        stateManager.pushCurrentState()

        val first = stateManager.popState()
        val second = stateManager.popState()

        assertEquals(MenuState(MenuType.SETTINGS, 0), first)
        assertEquals(MenuState(MenuType.MAIN, 2), second)
        assertTrue(stateManager.isStackEmpty())
    }

    @Test
    fun `popState em pilha vazia retorna nulo`() {
        assertNull(stateManager.popState())
    }

    @Test
    fun `clearStack esvazia a pilha`() {
        stateManager.pushCurrentState()
        stateManager.pushCurrentState()

        stateManager.clearStack()

        assertTrue(stateManager.isStackEmpty())
        assertEquals(0, stateManager.getStackSize())
    }

    // --- Registro de fragment ---

    @Test
    fun `registerFragment define o fragment atual e a contagem de itens`() {
        val fragment = mockk<MenuFragment>(relaxed = true)

        stateManager.registerFragment(fragment, itemCount = 5)

        assertEquals(fragment, stateManager.currentFragment)
        assertEquals(5, stateManager.currentMenuItemCount)
    }

    @Test
    fun `unregisterFragment limpa o fragment atual e zera a contagem`() {
        stateManager.registerFragment(mockk<MenuFragment>(relaxed = true), itemCount = 5)

        stateManager.unregisterFragment()

        assertNull(stateManager.currentFragment)
        assertEquals(0, stateManager.currentMenuItemCount)
    }

    // --- isMenuActive ---

    @Test
    fun `isMenuActive e falso quando nenhum fragment esta registrado`() {
        assertFalse(stateManager.isMenuActive())
    }

    @Test
    fun `isMenuActive e falso quando o fragment registrado nao e um androidx Fragment real`() {
        // MenuFragment é uma interface; um mock que a implementa mas não estende
        // androidx.fragment.app.Fragment falha no cast interno de isMenuActive() e o menu conta
        // como inativo. O caso "true" (fragment realmente adicionado) é coberto indiretamente
        // pelos testes de integração com RetroMenu3Fragment em MenuIntegration_test.kt.
        val fragment = mockk<MenuFragment>(relaxed = true)
        every { fragment.getCurrentSelectedIndex() } returns 0
        stateManager.registerFragment(fragment, itemCount = 1)

        assertFalse(stateManager.isMenuActive())
    }

    // --- saveState / restoreState (round-trip usado após rotação de tela) ---

    @Test
    fun `saveState e restoreState preservam menu, indice e pilha`() {
        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(1)
        stateManager.pushCurrentState()
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.updateSelectedIndex(3)

        val bundle = Bundle()
        stateManager.saveState(bundle)

        val restored = NavigationStateManager()
        restored.restoreState(bundle)

        assertEquals(MenuType.SETTINGS, restored.currentMenu)
        assertEquals(3, restored.selectedItemIndex)
        assertEquals(1, restored.getStackSize())

        val previous = restored.popState()
        assertEquals(MenuState(MenuType.MAIN, 1), previous)
    }

    @Test
    fun `restoreState com bundle nulo nao altera o estado`() {
        stateManager.updateCurrentMenu(MenuType.SETTINGS)

        stateManager.restoreState(null)

        assertEquals(MenuType.SETTINGS, stateManager.currentMenu)
    }

    @Test
    fun `restoreState com tipo de menu invalido no bundle nao altera o estado nem lanca excecao`() {
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        val bundle = Bundle()
        stateManager.saveState(bundle)
        // "nav_current_menu" espelha a KEY_CURRENT_MENU privada de NavigationStateManager.
        bundle.putString("nav_current_menu", "NOT_A_REAL_MENU_TYPE")

        stateManager.restoreState(bundle)

        assertEquals(MenuType.SETTINGS, stateManager.currentMenu)
    }
}
