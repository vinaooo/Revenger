package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.ui.retromenu3.MenuState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuViewModel_test {

    private lateinit var viewModel: MenuViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MenuViewModel(app)
    }

    @Test
    fun `estado inicial nao tem nenhum menu aberto`() {
        assertFalse(viewModel.isRetroMenu3Open)
        assertFalse(viewModel.isDismissingAllMenus)
        assertFalse(viewModel.isSettingsMenuOpen())
        assertEquals(MenuState.MAIN_MENU, viewModel.menuState.value.currentState)
    }

    @Test
    fun `dismissRetroMenu3 fecha o menu`() {
        viewModel.dismissRetroMenu3()

        assertFalse(viewModel.isRetroMenu3Open)
    }

    @Test
    fun `dismissAllMenus marca dismissingAllMenus`() {
        viewModel.dismissAllMenus()

        assertTrue(viewModel.isDismissingAllMenus)
    }

    @Test
    fun `updateMenuState altera o menuState observavel`() {
        viewModel.updateMenuState(MenuState.SETTINGS_MENU)

        assertEquals(MenuState.SETTINGS_MENU, viewModel.menuState.value.currentState)
    }

    @Test
    fun `isSettingsMenuOpen permanece falso apos updateMenuState (activateMenu nao e chamado pela API publica)`() {
        // updateMenuState() só altera currentState (via MenuStateManager.changeState); ele não
        // chama activateMenu(), que é o único caminho que popularia `activeMenus` -- o Set que
        // isSettingsMenuOpen()/isProgressMenuOpen()/isExitMenuOpen() de fato consultam. Como
        // activateMenu()/deactivateMenu() são privados e não chamados por nenhum outro método
        // público do ViewModel, esses três getters ficam sempre falsos hoje. Documentado aqui
        // como característica do comportamento atual, não como afirmação de que está correto.
        viewModel.updateMenuState(MenuState.SETTINGS_MENU)

        assertFalse(viewModel.isSettingsMenuOpen())
    }
}
