package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuViewModel_test {

    private lateinit var viewModel: MenuViewModel
    private lateinit var activity: FragmentActivity

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MenuViewModel(app)
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    }

    @Test
    fun `estado inicial nao tem nenhum menu aberto`() {
        assertFalse(viewModel.isRetroMenu3Open)
        assertFalse(viewModel.isDismissingAllMenus)
        assertFalse(viewModel.isSettingsMenuOpen())
        assertEquals(MenuState.MAIN_MENU, viewModel.menuState.value.currentState)
    }

    @Test
    fun `showRetroMenu3 emite o evento e marca o menu como aberto`() {
        viewModel.showRetroMenu3(activity)

        assertTrue(viewModel.isRetroMenu3Open)
        val event = viewModel.eventFlow.value
        assertTrue(event is MenuViewModel.MenuEvent.ShowRetroMenu3)
        assertEquals(activity, (event as MenuViewModel.MenuEvent.ShowRetroMenu3).activity)
    }

    @Test
    fun `dismissRetroMenu3 fecha o menu e emite o evento`() {
        viewModel.showRetroMenu3(activity)

        viewModel.dismissRetroMenu3()

        assertFalse(viewModel.isRetroMenu3Open)
        assertTrue(viewModel.eventFlow.value is MenuViewModel.MenuEvent.DismissRetroMenu3)
    }

    @Test
    fun `dismissAllMenus marca dismissingAllMenus e emite o evento`() {
        viewModel.dismissAllMenus()

        assertTrue(viewModel.isDismissingAllMenus)
        assertTrue(viewModel.eventFlow.value is MenuViewModel.MenuEvent.DismissAllMenus)
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

    @Test
    fun `clearControllerInputState emite o evento correspondente`() {
        viewModel.clearControllerInputState()

        assertTrue(viewModel.eventFlow.value is MenuViewModel.MenuEvent.ClearControllerInputState)
    }
}
