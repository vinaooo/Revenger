package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.ShaderController
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.MenuAction
import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.viewmodels.menu.SaveLoadCentralizedController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for the branches of [GameActivityViewModel.onMenuEvent] that
 * `GameActivityViewModel_test.kt` doesn't already cover (TOGGLE_*, SAVE/LOAD/RESET routing, BACK
 * in the non-Settings submenus, the ignored actions, and StateChanged for the non-Settings
 * submenus). Written BEFORE extracting this cluster into dedicated classes under
 * `viewmodels/menu/`.
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

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    private fun mockMenuManager(): MenuManager {
        val mock = mockk<MenuManager>(relaxed = true)
        setPrivateField(viewModel, "menuManager", mock)
        return mock
    }

    private fun mockSaveLoadCentralizedController(): SaveLoadCentralizedController {
        val mock = mockk<SaveLoadCentralizedController>(relaxed = true)
        setPrivateField(viewModel, "saveLoadCentralizedController", mock)
        return mock
    }

    private fun isMenuTypeActive(menuType: MenuSystemState.MenuType): Boolean {
        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")
        return menuStateManager.isMenuActive(menuType)
    }

    private fun mockGlRetroView(): GLRetroView = mockk(relaxed = true)

    private fun mockRetroView(glRetroView: GLRetroView): RetroView {
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        return retroView
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

    // --- TOGGLE_AUDIO ---

    @Test
    fun `TOGGLE_AUDIO com retroView emite ToggleAudio no audioViewModel`() {
        val glRetroView = mockGlRetroView()
        viewModel.retroView = mockRetroView(glRetroView)

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.TOGGLE_AUDIO))

        val audioViewModel = getPrivateField<AudioViewModel>(viewModel, "audioViewModel")
        val event = audioViewModel.eventFlow.value
        assertTrue(event is AudioViewModel.AudioEvent.ToggleAudio)
        assertEquals(glRetroView, (event as AudioViewModel.AudioEvent.ToggleAudio).retroView)
    }

    @Test
    fun `TOGGLE_AUDIO sem retroView nao emite evento`() {
        viewModel.retroView = null

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.TOGGLE_AUDIO))

        val audioViewModel = getPrivateField<AudioViewModel>(viewModel, "audioViewModel")
        assertEquals(AudioViewModel.AudioEvent.Idle, audioViewModel.eventFlow.value)
    }

    // --- TOGGLE_SPEED ---

    @Test
    fun `TOGGLE_SPEED com retroView chama toggleFastForward no speedController`() {
        val glRetroView = mockGlRetroView()
        viewModel.retroView = mockRetroView(glRetroView)
        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.TOGGLE_SPEED))

        verify(exactly = 1) { speedController.toggleFastForward(glRetroView) }
    }

    @Test
    fun `TOGGLE_SPEED sem retroView nao chama speedController`() {
        viewModel.retroView = null
        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.TOGGLE_SPEED))

        verify(exactly = 0) { speedController.toggleFastForward(any()) }
    }

    // --- TOGGLE_SHADER ---

    @Test
    fun `TOGGLE_SHADER chama shaderViewModel toggleShader independente do retroView`() {
        viewModel.retroView = null
        val shaderController = mockk<ShaderController>(relaxed = true)
        every { shaderController.getCurrentShader() } returns "default"
        every { shaderController.cycleShader() } returns "crt"
        val shaderViewModel = getPrivateField<ShaderViewModel>(viewModel, "shaderViewModel")
        shaderViewModel.setShaderController(shaderController)

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.TOGGLE_SHADER))

        verify(exactly = 1) { shaderController.cycleShader() }
    }

    // --- SAVE_STATE / LOAD_STATE / RESET routing ---

    @Test
    fun `SAVE_STATE aciona saveStateCentralized sem onComplete`() {
        val controller = mockSaveLoadCentralizedController()

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.SAVE_STATE))

        verify(exactly = 1) { controller.saveStateCentralized(null, false) }
    }

    @Test
    fun `LOAD_STATE aciona loadStateCentralized sem onComplete`() {
        val controller = mockSaveLoadCentralizedController()

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.LOAD_STATE))

        verify(exactly = 1) { controller.loadStateCentralized(null) }
    }

    @Test
    fun `RESET aciona resetGameCentralized sem onComplete`() {
        val controller = mockSaveLoadCentralizedController()

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.RESET))

        verify(exactly = 1) { controller.resetGameCentralized(null) }
    }

    // --- BACK routing for the non-Settings submenus ---

    @Test
    fun `onMenuEvent com Action BACK no PROGRESS_MENU chama dismissProgress`() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        try {
            val menuManagerMock = mockMenuManager()
            every { menuManagerMock.getCurrentState() } returns MenuState.PROGRESS_MENU

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.BACK))

            verify { android.util.Log.d("GameActivityViewModel", "dismissProgress: Starting") }
        } finally {
            unmockkStatic(android.util.Log::class)
        }
    }

    @Test
    fun `onMenuEvent com Action BACK no ABOUT_MENU chama dismissAboutMenu`() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        try {
            val menuManagerMock = mockMenuManager()
            every { menuManagerMock.getCurrentState() } returns MenuState.ABOUT_MENU

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.BACK))

            verify { android.util.Log.d("GameActivityViewModel", "dismissAbout: Starting") }
        } finally {
            unmockkStatic(android.util.Log::class)
        }
    }

    @Test
    fun `onMenuEvent com Action BACK no EXIT_MENU chama dismissExit`() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        try {
            val menuManagerMock = mockMenuManager()
            every { menuManagerMock.getCurrentState() } returns MenuState.EXIT_MENU

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.BACK))

            verify { android.util.Log.d("GameActivityViewModel", "dismissExit: Starting") }
        } finally {
            unmockkStatic(android.util.Log::class)
        }
    }

    // --- Ignored actions ---

    @Test
    fun `acoes ignoradas nao disparam efeitos colaterais`() {
        val menuManagerMock = mockMenuManager()
        val controller = mockSaveLoadCentralizedController()

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.CONTINUE))
        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.MANAGE_SAVES))
        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.SAVE_LOG))
        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.NONE))

        verify(exactly = 0) { menuManagerMock.navigateToState(any()) }
        verify(exactly = 0) { controller.saveStateCentralized(any(), any()) }
        verify(exactly = 0) { controller.loadStateCentralized(any()) }
        verify(exactly = 0) { controller.resetGameCentralized(any()) }
        val audioViewModel = getPrivateField<AudioViewModel>(viewModel, "audioViewModel")
        assertEquals(AudioViewModel.AudioEvent.Idle, audioViewModel.eventFlow.value)
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
