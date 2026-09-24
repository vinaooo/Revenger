package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
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
                        menuStateManager = menuStateManager,
                        isRetroMenu3Open = { false },
                        isDismissingAllMenus = { false }
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
                        menuStateManager = MenuStateManager(),
                        isRetroMenu3Open = { false },
                        isDismissingAllMenus = { false }
                )
        val laterMenuManager = mockk<MenuManager>(relaxed = true)
        currentMenuManager = laterMenuManager

        localDismisser.dismissSettingsMenu()

        verify(exactly = 1) { laterMenuManager.navigateToState(any()) }
        verify { initialMenuManager wasNot Called }
    }
}
