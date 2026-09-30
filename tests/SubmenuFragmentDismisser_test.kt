package com.vinaooo.revenger.viewmodels.menu

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SubmenuFragmentDismisser_test {

    private lateinit var state: SubmenuFragmentState
    private lateinit var menuStateManager: MenuStateManager
    private lateinit var menuManagerMock: MenuManager
    private lateinit var dismisser: SubmenuFragmentDismisser

    @Before
    fun setUp() {
        state = SubmenuFragmentState()
        menuStateManager = MenuStateManager()
        menuManagerMock = mockk(relaxed = true)
        dismisser =
                SubmenuFragmentDismisser(
                        state = state,
                        menuManager = { menuManagerMock },
                        menuStateManager = menuStateManager
                )
    }

    @Test
    fun `isSettingsMenuOpen reflete o estado do campo compartilhado`() {
        assertFalse(dismisser.isSettingsMenuOpen())

        state.settingsMenuFragment = mockk(relaxed = true)

        assertTrue(dismisser.isSettingsMenuOpen())
    }

    @Test
    fun `dismissSettingsMenu nao faz nada quando o fragmento nao esta added`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns false
        state.settingsMenuFragment = fragment
        menuStateManager.activateMenu(MenuSystemState.MenuType.SETTINGS_MENU)

        dismisser.dismissSettingsMenu()

        assertSame(fragment, state.settingsMenuFragment)
        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify { menuManagerMock wasNot Called }
    }

    @Test
    fun `dismissSettingsMenu limpa apenas o proprio campo, deixando Progress intacto`() {
        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        every { settingsFragment.activity } returns null
        state.settingsMenuFragment = settingsFragment
        menuStateManager.activateMenu(MenuSystemState.MenuType.SETTINGS_MENU)

        val progressFragment = mockk<ProgressFragment>(relaxed = true)
        state.progressFragment = progressFragment
        menuStateManager.activateMenu(MenuSystemState.MenuType.PROGRESS_MENU)

        dismisser.dismissSettingsMenu()

        assertTrue(state.settingsMenuFragment == null)
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertSame(progressFragment, state.progressFragment)
        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `fechar Progress, Exit ou About limpa o campo, desativa o menu e volta ao menu principal`() {
        val progress = mockk<ProgressFragment>(relaxed = true) { every { isAdded } returns true }
        val exit = mockk<ExitFragment>(relaxed = true) { every { isAdded } returns true }
        val about = mockk<AboutFragment>(relaxed = true) { every { isAdded } returns true }
        state.progressFragment = progress
        state.exitFragment = exit
        state.aboutFragment = about
        menuStateManager.activateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
        menuStateManager.activateMenu(MenuSystemState.MenuType.EXIT_MENU)
        menuStateManager.activateMenu(MenuSystemState.MenuType.ABOUT_MENU)

        dismisser.dismissProgress()
        assertNull(state.progressFragment)
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
        verify(exactly = 1) { menuManagerMock.navigateToState(MenuState.MAIN_MENU) }

        dismisser.dismissExit()
        assertNull(state.exitFragment)
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU))
        verify(exactly = 2) { menuManagerMock.navigateToState(MenuState.MAIN_MENU) }

        dismisser.dismissAboutMenu()
        assertNull(state.aboutFragment)
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
        verify(exactly = 3) { menuManagerMock.navigateToState(MenuState.MAIN_MENU) }
    }

    /**
     * [menuManager] is a provider read at call time, not a value captured when the dismisser is
     * built -- this pins that design, since `GameActivityViewModel`'s tests depend on it (they
     * swap `menuManager` by reflection after construction).
     */
    @Test
    fun `menuManager e lido no momento da chamada, nao capturado na construcao`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        every { fragment.activity } returns null
        val localState = SubmenuFragmentState().apply { settingsMenuFragment = fragment }
        var currentMenuManager = mockk<MenuManager>(relaxed = true)
        val initialMenuManager = currentMenuManager
        val localDismisser =
                SubmenuFragmentDismisser(
                        state = localState,
                        menuManager = { currentMenuManager },
                        menuStateManager = MenuStateManager()
                )
        val laterMenuManager = mockk<MenuManager>(relaxed = true)
        currentMenuManager = laterMenuManager

        localDismisser.dismissSettingsMenu()

        verify(exactly = 1) { laterMenuManager.navigateToState(any()) }
        verify { initialMenuManager wasNot Called }
    }

    private fun addedSettingsWithBackStack(backStackCount: Int): FragmentManager {
        val fragmentManager = mockk<FragmentManager>(relaxed = true)
        every { fragmentManager.backStackEntryCount } returns backStackCount
        val activity = mockk<FragmentActivity>()
        every { activity.supportFragmentManager } returns fragmentManager
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        every { fragment.activity } returns activity
        state.settingsMenuFragment = fragment
        return fragmentManager
    }

    @Test
    fun `dismiss com back stack tira o submenu da pilha`() {
        val fragmentManager = addedSettingsWithBackStack(backStackCount = 1)

        dismisser.dismissSettingsMenu()

        verify(exactly = 1) { fragmentManager.popBackStackImmediate() }
        assertNull(state.settingsMenuFragment)
    }

    @Test
    fun `dismiss com back stack vazia nao tira nada da pilha mas limpa o campo`() {
        val fragmentManager = addedSettingsWithBackStack(backStackCount = 0)

        dismisser.dismissSettingsMenu()

        verify(exactly = 0) { fragmentManager.popBackStackImmediate() }
        assertNull(state.settingsMenuFragment)
        verify { menuManagerMock.navigateToState(MenuState.MAIN_MENU) }
    }

    @Test
    fun `dismiss sem fragment aberto nao faz nada`() {
        dismisser.dismissSettingsMenu()
        dismisser.dismissProgress()
        dismisser.dismissExit()
        dismisser.dismissAboutMenu()

        verify { menuManagerMock wasNot Called }
    }
}
