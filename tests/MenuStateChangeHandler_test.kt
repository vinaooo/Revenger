package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuStateChangeHandler] is exercised against a REAL [MenuStateManager], since that's the
 * simplest way to observe which submenu ended up active/inactive -- there is no simpler
 * observable side effect to mock. [isAnyMenuActive]/[isRetroMenu3Open] are only used for
 * diagnostic logging, so they're stubbed to a fixed value here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuStateChangeHandler_test {

    private lateinit var menuStateManager: MenuStateManager
    private lateinit var handler: MenuStateChangeHandler

    @Before
    fun setUp() {
        menuStateManager = MenuStateManager()
        handler =
                MenuStateChangeHandler(
                        menuStateManager = menuStateManager,
                        isAnyMenuActive = { false },
                        isRetroMenu3Open = { false }
                )
    }

    @Test
    fun `StateChanged para SETTINGS_MENU ativa o menu de configuracoes`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU))

        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `StateChanged saindo de SETTINGS_MENU desativa o menu de configuracoes`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU))

        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.SETTINGS_MENU, MenuState.MAIN_MENU))

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `StateChanged para PROGRESS_MENU ativa o menu de progresso`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.PROGRESS_MENU))

        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `StateChanged para ABOUT_MENU ativa o menu sobre`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.ABOUT_MENU))

        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `StateChanged para EXIT_MENU ativa o menu de saida`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.EXIT_MENU))

        assertTrue(menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU))
    }

    @Test
    fun `StateChanged saindo de PROGRESS_MENU desativa o menu de progresso`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.PROGRESS_MENU))

        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.PROGRESS_MENU, MenuState.MAIN_MENU))

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `StateChanged saindo de ABOUT_MENU desativa o menu sobre`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.ABOUT_MENU))

        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.ABOUT_MENU, MenuState.MAIN_MENU))

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `StateChanged saindo de EXIT_MENU desativa o menu de saida`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.EXIT_MENU))

        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.EXIT_MENU, MenuState.MAIN_MENU))

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU))
    }

    @Test
    fun `StateChanged para MAIN_MENU nao ativa nenhum submenu`() {
        handler.handleStateChanged(MenuEvent.StateChanged(MenuState.SETTINGS_MENU, MenuState.MAIN_MENU))

        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
        assertFalse(menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU))
    }
}
