package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.View
import com.vinaooo.revenger.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The main menu driven through its public contract, with a mocked ViewModel and [MenuManager]
 * ([MenuFragmentHost]). The fragment sits in `R.id.menu_container`, the container its submenus
 * replace it in, so opening and closing a submenu runs the real back stack round trip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetroMenu3FragmentActions_test {

    private var menuState = MenuState.MAIN_MENU
    private val menuManager: MenuManager = mockk(relaxed = true)

    private lateinit var host: MenuFragmentHost<RetroMenu3Fragment>

    @After
    fun tearDown() {
        host.destroy()
    }

    @Before
    fun setUp() {
        every { menuManager.getCurrentState() } answers { menuState }
        host = MenuFragmentHost(RetroMenu3Fragment.newInstance(), R.id.menu_container) {
            every { viewModel.getMenuManager() } returns menuManager
        }
    }

    private val fragment get() = host.fragment
    private val fragmentManager get() = host.activity.supportFragmentManager

    /** The inner container of the menu layout, whose alpha and visibility the menu toggles. */
    private fun menuContainer(): View =
            fragment.requireView().findViewById(R.id.menu_container)

    private fun arrow(id: Int): View = fragment.requireView().findViewById(id)

    /** Selects the main menu item at [index] and confirms it, as the gamepad does. */
    private fun openSubmenuThroughConfirm(index: Int) {
        fragment.setSelectedIndex(index)
        fragment.onConfirm()
    }

    @Test
    fun `registra o fragment no NavigationController com os 6 itens do menu`() {
        verify { host.navigationController.registerFragment(fragment, 6) }
    }

    @Test
    fun `navegacao circula pelos 6 itens e move a seta de selecao`() {
        fragment.onNavigateUp()
        assertEquals(5, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_exit).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_continue).visibility)

        fragment.onNavigateDown()
        assertEquals(0, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_continue).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_exit).visibility)
    }

    @Test
    fun `Continue fecha o menu e avisa o NavigationController`() {
        fragment.setSelectedIndex(0)
        fragment.onConfirm()
        host.idle()

        assertFalse(fragment.isAdded)
        verify { host.navigationController.closeMenuExternal() }
    }

    @Test
    fun `Reset volta a velocidade normal antes de fechar o menu`() {
        fragment.setSelectedIndex(1)
        fragment.onConfirm()
        host.idle()

        verify { host.viewModel.setGameSpeed(1) }
        assertFalse(fragment.isAdded)
    }

    @Test
    fun `abrir e fechar um submenu restaura o item selecionado do menu principal`() {
        fragment.setSelectedIndex(3)
        fragment.onConfirm()
        fragmentManager.executePendingTransactions()
        assertEquals(1, fragmentManager.backStackEntryCount)
        verify { menuManager.navigateToState(MenuState.SETTINGS_MENU) }

        menuState = MenuState.SETTINGS_MENU
        fragment.hideMainMenu()
        fragmentManager.popBackStack()
        fragmentManager.executePendingTransactions()
        host.advance(SETTLE_MS)

        assertEquals(0, fragmentManager.backStackEntryCount)
        verify { menuManager.navigateToState(MenuState.MAIN_MENU) }
        verify { host.viewModel.unregisterSettingsMenuFragment() }
        assertEquals(3, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, menuContainer().visibility)
    }

    @Test
    fun `onBack com submenu aberto fecha o submenu`() {
        openSubmenuThroughConfirm(4) // about
        fragmentManager.executePendingTransactions()

        assertTrue(fragment.onBack())
        fragmentManager.executePendingTransactions()

        assertEquals(0, fragmentManager.backStackEntryCount)
    }

    @Test
    fun `onBack sem submenu devolve false`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `onAboutBackToMainMenu fecha o submenu aberto`() {
        openSubmenuThroughConfirm(5) // exit
        fragmentManager.executePendingTransactions()
        assertEquals(1, fragmentManager.backStackEntryCount)

        fragment.onAboutBackToMainMenu()
        fragmentManager.executePendingTransactions()
        host.idle()

        assertEquals(0, fragmentManager.backStackEntryCount)
    }

    @Test
    fun `dimMainMenu e restoreMainMenu mudam a opacidade do menu`() {
        fragment.dimMainMenu()
        assertTrue(menuContainer().alpha < 1f)

        fragment.restoreMainMenu()
        assertEquals(1f, menuContainer().alpha)
    }

    @Test
    fun `showMainMenu sem preservar volta a selecao para o primeiro item`() {
        fragment.setSelectedIndex(4)
        fragment.hideMainMenu()
        assertEquals(View.INVISIBLE, menuContainer().visibility)

        fragment.showMainMenu()

        assertEquals(View.VISIBLE, menuContainer().visibility)
        assertEquals(0, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_continue).visibility)
    }

    @Test
    fun `showMainMenu preservando a selecao mantem o item atual`() {
        fragment.setSelectedIndex(4)
        fragment.hideMainMenu()

        fragment.showMainMenu(preserveSelection = true)

        assertEquals(View.VISIBLE, menuContainer().visibility)
        assertEquals(4, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `onSaveInstanceState guarda o estado atual do MenuManager`() {
        menuState = MenuState.ABOUT_MENU
        val outState = Bundle()

        fragment.onSaveInstanceState(outState)

        assertEquals("ABOUT_MENU", outState.getString("SUBMENU_STATE"))
    }

    @Test
    fun `onViewStateRestored reabre o submenu salvo depois do atraso`() {
        fragment.onViewStateRestored(Bundle().apply { putString("SUBMENU_STATE", "SETTINGS_MENU") })
        host.idle()
        assertEquals(0, fragmentManager.backStackEntryCount)

        host.advance(SETTLE_MS)
        fragmentManager.executePendingTransactions()

        assertNotNull(fragmentManager.findFragmentByTag("SettingsMenuFragment"))
        verify { menuManager.navigateToState(MenuState.SETTINGS_MENU) }
    }

    @Test
    fun `onViewStateRestored com o menu principal salvo nao abre submenu`() {
        fragment.onViewStateRestored(Bundle().apply { putString("SUBMENU_STATE", "MAIN_MENU") })
        fragment.onViewStateRestored(Bundle())
        fragment.onViewStateRestored(null)
        host.advance(SETTLE_MS)

        assertEquals(0, fragmentManager.backStackEntryCount)
        verify(exactly = 0) { menuManager.navigateToState(any()) }
    }

    @Test
    fun `dismissMenuPublic remove o fragment e chama o callback uma vez`() {
        var calls = 0

        fragment.dismissMenuPublic { calls++ }
        host.idle()

        assertFalse(fragment.isAdded)
        assertFalse(fragment.isDismissingMenu())
        assertEquals(1, calls)
    }

    // Regression: a second dismiss during the close animation used to start another animation,
    // which replaced the first one's listener, so the first caller's callback (for example
    // Reset's resetGameCentralized) was never called.
    @Test
    fun `dois dismiss seguidos chamam os dois callbacks uma vez cada`() {
        var first = 0
        var second = 0

        fragment.dismissMenuPublic { first++ }
        fragment.dismissMenuPublic { second++ }
        host.advance(SETTLE_MS)

        assertEquals(1, first)
        assertEquals(1, second)
        assertFalse(fragment.isAdded)
    }

    @Test
    fun `isDismissingMenu fica verdadeiro ate o fim da animacao de fechar`() {
        fragment.dismissMenuPublic()

        assertTrue(fragment.isDismissingMenu())

        host.advance(SETTLE_MS)

        assertFalse(fragment.isDismissingMenu())
        assertFalse(fragment.isAdded)
    }

    @Test
    fun `fragment removido antes do fim da animacao ainda chama o callback`() {
        var calls = 0

        fragment.dismissMenuPublic { calls++ }
        fragmentManager.beginTransaction().remove(fragment).commitNow()
        host.advance(SETTLE_MS)

        assertEquals(1, calls)
    }

    @Test
    fun `callback de dismiss que falha e chamado de novo pelo tratamento de erro`() {
        var calls = 0

        fragment.dismissMenuPublic {
            calls++
            if (calls == 1) error("callback failure")
        }
        host.idle()

        assertEquals(2, calls)
    }

    @Test
    fun `onResume no menu principal registra o fragment no ViewModel e foca o primeiro item`() {
        host.controller.pause().resume()
        host.idle()

        verify { host.viewModel.updateRetroMenu3FragmentReference(fragment) }
        assertSame(
                fragment.requireView().findViewById<View>(R.id.menu_continue),
                host.activity.currentFocus ?: fragment.requireView().findFocus()
        )
    }

    @Test
    fun `onResume com um submenu ativo nao registra o fragment de novo`() {
        menuState = MenuState.SETTINGS_MENU

        host.controller.pause().resume()
        host.idle()

        verify(exactly = 0) { host.viewModel.updateRetroMenu3FragmentReference(any()) }
    }

    @Test
    fun `remover o fragment com restauracao pendente nao dispara callbacks sem view`() {
        openSubmenuThroughConfirm(2) // progress
        fragmentManager.executePendingTransactions()
        fragmentManager.popBackStack()
        fragmentManager.executePendingTransactions()

        fragmentManager.beginTransaction().remove(fragment).commitNow()
        host.idle()

        assertFalse(fragment.isAdded)
        assertNull(fragment.view)
    }

    @Test
    fun `falha no onViewCreated e propagada`() {
        assertThrows(IllegalStateException::class.java) {
            MenuFragmentHost(RetroMenu3Fragment.newInstance(), R.id.menu_container) {
                every { viewModel.getMenuManager() } returns menuManager
                every { viewModel.navigationController } throws IllegalStateException("setup failure")
            }
        }
    }

    private companion object {
        /** Longer than every delay the menu posts (submenu reopen, restore steps, animations). */
        const val SETTLE_MS = 1_000L
    }
}
