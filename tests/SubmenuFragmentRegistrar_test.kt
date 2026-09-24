package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.viewmodels.MenuViewModel
import io.mockk.Called
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
class SubmenuFragmentRegistrar_test {

    private lateinit var state: SubmenuFragmentState
    private lateinit var menuStateManager: MenuStateManager
    private lateinit var menuManagerMock: MenuManager
    private lateinit var menuViewModelMock: MenuViewModel
    private lateinit var registrar: SubmenuFragmentRegistrar

    @Before
    fun setUp() {
        state = SubmenuFragmentState()
        menuStateManager = MenuStateManager()
        menuManagerMock = mockk(relaxed = true)
        menuViewModelMock = mockk(relaxed = true)
        registrar =
                SubmenuFragmentRegistrar(
                        state = state,
                        menuManager = { menuManagerMock },
                        menuStateManager = menuStateManager,
                        menuViewModel = { menuViewModelMock },
                        isAnyMenuActive = { false }
                )
    }

    @Test
    fun `registerSettingsMenuFragment define o fragmento, ativa o menu e notifica o menuViewModel`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)

        registrar.registerSettingsMenuFragment(fragment)

        assertSame(fragment, state.settingsMenuFragment)
        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.SETTINGS_MENU, fragment) }
        verify(exactly = 1) { menuViewModelMock.registerSettingsMenuFragment(fragment) }
    }

    @Test
    fun `registerSettingsMenuFragmentForRotation define o fragmento mas NAO ativa o menu nem notifica o menuViewModel`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)

        registrar.registerSettingsMenuFragmentForRotation(fragment)

        assertSame(fragment, state.settingsMenuFragment)
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify { menuViewModelMock wasNot Called }
    }

    @Test
    fun `unregisterSettingsMenuFragment limpa o fragmento e desativa o menu`() {
        registrar.registerSettingsMenuFragment(mockk(relaxed = true))

        registrar.unregisterSettingsMenuFragment()

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertTrue(state.settingsMenuFragment == null)
        verify(exactly = 1) { menuManagerMock.unregisterFragment(MenuState.SETTINGS_MENU) }
    }

    /**
     * [menuManager] is a provider read at call time, not a value captured when the registrar is
     * built -- this pins that design, since `GameActivityViewModel`'s tests depend on it (they
     * swap `menuManager` by reflection after construction).
     */
    @Test
    fun `menuManager e lido no momento da chamada, nao capturado na construcao`() {
        var currentMenuManager = mockk<MenuManager>(relaxed = true)
        val initialMenuManager = currentMenuManager
        val localRegistrar =
                SubmenuFragmentRegistrar(
                        state = SubmenuFragmentState(),
                        menuManager = { currentMenuManager },
                        menuStateManager = MenuStateManager(),
                        menuViewModel = { menuViewModelMock },
                        isAnyMenuActive = { false }
                )
        val laterMenuManager = mockk<MenuManager>(relaxed = true)
        currentMenuManager = laterMenuManager

        localRegistrar.registerSettingsMenuFragment(mockk(relaxed = true))

        verify(exactly = 1) { laterMenuManager.registerFragment(any(), any()) }
        verify { initialMenuManager wasNot Called }
    }
}
