package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.onMenuEvent]'s StateChanged handling of the
 * non-Settings submenus (`GameActivityViewModel_test.kt` covers Settings).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_menuEventHandling_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    private fun isMenuTypeActive(menuType: MenuSystemState.MenuType): Boolean {
        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")
        return menuStateManager.isMenuActive(menuType)
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // --- StateChanged for the non-Settings submenus ---

    @Test
    fun `StateChanged para PROGRESS_MENU ativa o menu de progresso`() {
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.PROGRESS_MENU))

        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `StateChanged saindo de PROGRESS_MENU desativa o menu de progresso`() {
        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.PROGRESS_MENU))
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.PROGRESS_MENU, MenuState.MAIN_MENU))

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `StateChanged para ABOUT_MENU ativa o menu sobre`() {
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.ABOUT_MENU))

        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `StateChanged saindo de ABOUT_MENU desativa o menu sobre`() {
        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.ABOUT_MENU))
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.ABOUT_MENU, MenuState.MAIN_MENU))

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `StateChanged para EXIT_MENU ativa o menu de saida`() {
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.EXIT_MENU))

        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))
    }

    @Test
    fun `StateChanged saindo de EXIT_MENU desativa o menu de saida`() {
        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.EXIT_MENU))
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.EXIT_MENU, MenuState.MAIN_MENU))

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))
    }

    @Test
    fun `StateChanged para MAIN_MENU nao ativa nenhum submenu`() {
        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.SETTINGS_MENU, MenuState.MAIN_MENU))

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))
    }
}
