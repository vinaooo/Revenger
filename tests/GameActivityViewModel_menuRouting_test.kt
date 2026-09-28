package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.viewmodels.menu.PlaybackStateController
import com.vinaooo.revenger.viewmodels.menu.ScreenshotPreviewController
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Where [GameActivityViewModel] sends menu input: the [MenuEvent]s the menu system raises
 * (navigation to [MenuManager], a full close to the [NavigationController]), the SELECT+START and
 * START callbacks it wires into [ControllerInput], the submenus' "back to main menu", and the
 * load-preview overlay.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_menuRouting_test {

    private lateinit var viewModel: GameActivityViewModel
    private lateinit var navigationController: NavigationController

    private fun setRevengerAppConfig(config: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, config)
    }

    private fun <T> getPrivateField(name: String): T {
        val field = GameActivityViewModel::class.java.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(viewModel) as T
    }

    private inline fun <reified T : Any> replacePrivateField(name: String): T {
        val mock = mockk<T>(relaxed = true)
        val field = GameActivityViewModel::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(viewModel, mock)
        return mock
    }

    private fun sentNavigationEvent(): NavigationEvent {
        val event = slot<NavigationEvent>()
        verify(exactly = 1) { navigationController.handleNavigationEvent(capture(event)) }
        return event.captured
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        viewModel = GameActivityViewModel(ApplicationProvider.getApplicationContext<Application>())
        navigationController = mockk(relaxed = true)
        viewModel.navigationController = navigationController
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // --- onMenuEvent ---

    @Test
    fun `eventos de navegacao vao para o MenuManager`() {
        val menuManager = replacePrivateField<MenuManager>("menuManager")

        viewModel.onMenuEvent(MenuEvent.NavigateUp)
        viewModel.onMenuEvent(MenuEvent.NavigateDown)
        viewModel.onMenuEvent(MenuEvent.Confirm)
        viewModel.onMenuEvent(MenuEvent.Back)

        verify(exactly = 1) { menuManager.navigateUp() }
        verify(exactly = 1) { menuManager.navigateDown() }
        verify(exactly = 1) { menuManager.confirm() }
        verify(exactly = 1) { menuManager.back() }
    }

    @Test
    fun `MenuClosed fecha todos os menus pelo NavigationController`() {
        viewModel.onMenuEvent(MenuEvent.MenuClosed)

        val event = sentNavigationEvent()
        assertTrue(event is NavigationEvent.CloseAllMenus)
        assertEquals(null, (event as NavigationEvent.CloseAllMenus).keyCode)
        assertEquals(InputSource.PHYSICAL_GAMEPAD, event.inputSource)
    }

    @Test
    fun `MenuClosed sem NavigationController nao falha`() {
        viewModel.navigationController = null

        viewModel.onMenuEvent(MenuEvent.MenuClosed)
    }

    // --- callbacks wired into ControllerInput ---

    @Test
    fun `SELECT+START abre o menu pelo NavigationController`() {
        getPrivateField<ControllerInput>("controllerInput").selectStartComboCallback()

        val event = sentNavigationEvent()
        assertTrue(event is NavigationEvent.OpenMenu)
        assertEquals(InputSource.PHYSICAL_GAMEPAD, event.inputSource)
    }

    @Test
    fun `START fecha todos os menus dizendo que foi o START`() {
        getPrivateField<ControllerInput>("controllerInput").startButtonCallback()

        val event = sentNavigationEvent() as NavigationEvent.CloseAllMenus
        assertEquals(KeyEvent.KEYCODE_BUTTON_START, event.keyCode)
        assertEquals(InputSource.PHYSICAL_GAMEPAD, event.inputSource)
    }

    @Test
    fun `SELECT+START e START sem NavigationController nao falham`() {
        viewModel.navigationController = null
        val controllerInput = getPrivateField<ControllerInput>("controllerInput")

        controllerInput.selectStartComboCallback()
        controllerInput.startButtonCallback()
    }

    // --- load preview ---

    @Test
    fun `showLoadPreview mostra a imagem no overlay de preview`() {
        val preview = replacePrivateField<ScreenshotPreviewController>("screenshotPreviewController")
        val bitmap = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)

        viewModel.showLoadPreview(bitmap)

        verify(exactly = 1) { preview.showLoadPreview(bitmap) }
    }

    @Test
    fun `trocar o shader pelas configuracoes devolve o shader novo`() {
        val playback = replacePrivateField<PlaybackStateController>("playbackStateController")
        every { playback.onToggleShader() } returns "crt"

        assertEquals("crt", viewModel.onToggleShader())
    }
}
