package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuManager] tracks the current menu state, keeps the registered [MenuFragment]s, and tells its
 * [MenuManager.MenuManagerListener] about each state change. [MenuManagerFakeFragment] is a real
 * `Fragment` implementing [MenuFragment], attached to a real `FragmentActivity` via Robolectric.
 */
class MenuManagerFakeFragment : Fragment(), MenuFragment {
    override fun getMenuItems(): List<MenuItem> = emptyList()
    override fun onMenuItemSelected(item: MenuItem) {}
    override fun onNavigateUp(): Boolean = true
    override fun onNavigateDown(): Boolean = true
    override fun onConfirm(): Boolean = true
    override fun onBack(): Boolean = true
    override fun getCurrentSelectedIndex(): Int = 0
    override fun setSelectedIndex(index: Int) {}
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

    private fun attachedFragment(): MenuManagerFakeFragment {
        val fragment = MenuManagerFakeFragment()
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

    // --- MenuFragment defaults ---

    @Test
    fun `por padrao um MenuFragment nao trata esquerda e direita`() {
        val fragment = MenuManagerFakeFragment()

        assertFalse(fragment.onNavigateLeft())
        assertFalse(fragment.onNavigateRight())
    }
}
