package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuManager] is the State Machine that coordinates registered [MenuFragment]s and routes
 * [MenuAction]/[MenuEvent]s through a [MenuManager.MenuManagerListener]. It had zero test
 * coverage before this file (only exercised incidentally, and only as a relaxed mock, by
 * `GameActivityViewModel_test`).
 *
 * `navigateUp`/`navigateDown`/`confirm`/`back`/`getCurrentSelectedIndex`/`setSelectedIndex` all
 * gate on the current fragment being a real, attached `androidx.fragment.app.Fragment` (checked
 * via `isAdded`/`context`), so a plain `mockk<MenuFragment>()` can't exercise that guard --
 * [FakeMenuFragment] below is a real `Fragment` implementing [MenuFragment], attached to a real
 * `FragmentActivity` via Robolectric where the "attached" behavior needs to be verified.
 */
/**
 * NOTE for future tests in this package (`com.vinaooo.revenger.ui.retromenu3`): this is a public
 * top-level test double so `FragmentManager` can recreate it. If another test file needs a fake
 * `MenuFragment`, reuse this one or pick a distinct name -- don't redeclare `FakeMenuFragment`.
 */
class FakeMenuFragment : Fragment(), MenuFragment {
    var navigateUpResult = true
    var navigateDownResult = true
    var confirmResult = true
    var backResult = true
    var currentIndex = 0

    override fun getMenuItems(): List<MenuItem> = emptyList()
    override fun onMenuItemSelected(item: MenuItem) {}
    override fun onNavigateUp(): Boolean = navigateUpResult
    override fun onNavigateDown(): Boolean = navigateDownResult
    override fun onConfirm(): Boolean = confirmResult
    override fun onBack(): Boolean = backResult
    override fun getCurrentSelectedIndex(): Int = currentIndex
    override fun setSelectedIndex(index: Int) {
        currentIndex = index
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuManager_test {

    private lateinit var listener: MenuManager.MenuManagerListener
    private lateinit var stateManager: MenuStateManager
    private lateinit var menuManager: MenuManager
    private lateinit var activity: FragmentActivity

    @Before
    fun setUp() {
        listener = mockk(relaxed = true)
        stateManager = MenuStateManager()
        menuManager = MenuManager(listener, stateManager)
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    }

    private fun attachedFragment(): FakeMenuFragment {
        val fragment = FakeMenuFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "fake").commitNow()
        return fragment
    }

    // --- registro/consulta de fragments ---

    @Test
    fun `registerFragment associa o fragment ao estado e getCurrentFragment o retorna`() {
        val fragment = attachedFragment()

        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        assertEquals(fragment, menuManager.getCurrentFragment())
    }

    @Test
    fun `unregisterFragment remove a associacao`() {
        val fragment = attachedFragment()
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        menuManager.unregisterFragment(MenuState.MAIN_MENU)

        assertNull(menuManager.getCurrentFragment())
    }

    @Test
    fun `getCurrentFragment retorna null quando nenhum fragment foi registrado para o estado atual`() {
        assertNull(menuManager.getCurrentFragment())
    }

    // --- navegacao entre estados ---

    @Test
    fun `navigateToState atualiza o estado e emite StateChanged com origem e destino corretos`() {
        menuManager.navigateToState(MenuState.SETTINGS_MENU)

        assertEquals(MenuState.SETTINGS_MENU, menuManager.getCurrentState())
        verify {
            listener.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU))
        }
    }

    // --- handleAction ---

    @Test
    fun `handleAction com NAVIGATE navega ate o menu alvo`() {
        menuManager.handleAction(MenuAction.NAVIGATE(MenuState.ABOUT_MENU))

        assertEquals(MenuState.ABOUT_MENU, menuManager.getCurrentState())
        verify { listener.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.ABOUT_MENU)) }
    }

    @Test
    fun `handleAction com BACK fora do menu principal volta para MAIN_MENU`() {
        menuManager.navigateToState(MenuState.SETTINGS_MENU)

        menuManager.handleAction(MenuAction.BACK)

        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
        verify {
            listener.onMenuEvent(MenuEvent.StateChanged(MenuState.SETTINGS_MENU, MenuState.MAIN_MENU))
        }
    }

    @Test
    fun `handleAction com BACK ja no menu principal emite MenuClosed sem trocar de estado`() {
        menuManager.handleAction(MenuAction.BACK)

        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
        verify { listener.onMenuEvent(MenuEvent.MenuClosed) }
    }

    @Test
    fun `handleAction com outras acoes emite um evento Action encapsulando a acao`() {
        menuManager.handleAction(MenuAction.CONTINUE)

        verify { listener.onMenuEvent(MenuEvent.Action(MenuAction.CONTINUE)) }
    }

    // --- navigateUp / navigateDown / confirm / back: guarda de fragment anexado ---

    @Test
    fun `navigateUp delega ao fragment quando ele esta anexado`() {
        val fragment = attachedFragment().apply { navigateUpResult = true }
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        assertTrue(menuManager.navigateUp())
    }

    @Test
    fun `navigateUp retorna false e nao delega quando nenhum fragment esta registrado`() {
        assertFalse(menuManager.navigateUp())
    }

    @Test
    fun `navigateUp retorna false quando o fragment registrado nao esta anexado`() {
        // Fragment real, mas nunca adicionado a um FragmentManager (isAdded == false).
        menuManager.registerFragment(MenuState.MAIN_MENU, FakeMenuFragment().apply { navigateUpResult = true })

        assertFalse(menuManager.navigateUp())
    }

    @Test
    fun `navigateDown delega ao fragment quando ele esta anexado`() {
        val fragment = attachedFragment().apply { navigateDownResult = false }
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        assertFalse(menuManager.navigateDown())
    }

    @Test
    fun `confirm delega ao fragment quando ele esta anexado`() {
        val fragment = attachedFragment().apply { confirmResult = true }
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        assertTrue(menuManager.confirm())
    }

    @Test
    fun `confirm retorna false quando o fragment nao esta anexado`() {
        menuManager.registerFragment(MenuState.MAIN_MENU, FakeMenuFragment().apply { confirmResult = true })

        assertFalse(menuManager.confirm())
    }

    @Test
    fun `back delega ao fragment quando ele esta anexado e trata o resultado como handled`() {
        val fragment = attachedFragment().apply { backResult = true }
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        assertTrue(menuManager.back())
    }

    @Test
    fun `back no menu principal fecha o menu quando o fragment nao trata o evento`() {
        val fragment = attachedFragment().apply { backResult = false }
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        val handled = menuManager.back()

        assertTrue(handled)
        verify { listener.onMenuEvent(MenuEvent.MenuClosed) }
    }

    @Test
    fun `back fora do menu principal nao fecha o menu quando o fragment nao trata o evento`() {
        menuManager.navigateToState(MenuState.SETTINGS_MENU)
        val fragment = attachedFragment().apply { backResult = false }
        menuManager.registerFragment(MenuState.SETTINGS_MENU, fragment)

        val handled = menuManager.back()

        assertFalse(handled)
        verify(inverse = true) { listener.onMenuEvent(MenuEvent.MenuClosed) }
    }

    // --- indice selecionado ---

    @Test
    fun `getCurrentSelectedIndex e setSelectedIndex delegam ao fragment anexado`() {
        val fragment = attachedFragment()
        menuManager.registerFragment(MenuState.MAIN_MENU, fragment)

        menuManager.setSelectedIndex(3)

        assertEquals(3, menuManager.getCurrentSelectedIndex())
    }

    @Test
    fun `getCurrentSelectedIndex retorna 0 quando nenhum fragment esta disponivel`() {
        assertEquals(0, menuManager.getCurrentSelectedIndex())
    }

    @Test
    fun `setSelectedIndex nao lanca quando nenhum fragment esta disponivel`() {
        menuManager.setSelectedIndex(5)
    }

    // --- eventos de navegacao unificados ---

    @Test
    fun `sendNavigateUp, sendNavigateDown, sendConfirm, sendBack e sendAction emitem os eventos correspondentes`() {
        menuManager.sendNavigateUp()
        menuManager.sendNavigateDown()
        menuManager.sendConfirm()
        menuManager.sendBack()
        menuManager.sendAction(MenuAction.RESET)

        verify { listener.onMenuEvent(MenuEvent.NavigateUp) }
        verify { listener.onMenuEvent(MenuEvent.NavigateDown) }
        verify { listener.onMenuEvent(MenuEvent.Confirm) }
        verify { listener.onMenuEvent(MenuEvent.Back) }
        verify { listener.onMenuEvent(MenuEvent.Action(MenuAction.RESET)) }
    }
}
