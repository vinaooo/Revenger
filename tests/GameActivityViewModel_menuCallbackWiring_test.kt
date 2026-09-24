package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Characterization tests for the `setupMenuCallback` cluster of [GameActivityViewModel]: the
 * one-time NavigationController/KeyboardInputAdapter init, the floating-button and FrameRendered-
 * wait branches of the open/close callbacks, and the real dispatch logic behind the callbacks
 * wired by `wireControllerInputMenuCallbacks` (previously only identity-tested). Written BEFORE
 * extracting this cluster into dedicated classes under viewmodels/menu/.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_menuCallbackWiring_test {

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

    // ---------------------------------------------------------------------------------------
    // initializeNavigationControllerIfNeeded guard
    // ---------------------------------------------------------------------------------------

    @Test
    fun `setupMenuCallback so cria o NavigationController na primeira chamada`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        viewModel.setupMenuCallback(activity)
        val firstNavController = viewModel.navigationController
        assertNotNull(firstNavController)
        assertNotNull(viewModel.keyboardInputAdapter)

        viewModel.setupMenuCallback(activity)

        assertSame(firstNavController, viewModel.navigationController)
    }

    // ---------------------------------------------------------------------------------------
    // Floating button visibility on menu open/close
    // ---------------------------------------------------------------------------------------

    @Test
    fun `abrir o menu restaura a visibilidade do botao flutuante`() {
        val activity =
                mockk<FragmentActivity>(
                        relaxed = true,
                        moreInterfaces = arrayOf(FloatingButtonVisibilityHost::class)
                )
        viewModel.setupMenuCallback(activity)

        viewModel.navigationController?.onMenuOpenedCallback?.invoke()

        verify(exactly = 1) {
            (activity as FloatingButtonVisibilityHost).restoreFloatingButtonVisibility()
        }
    }

    @Test
    fun `fechar o menu esmaece o botao flutuante imediatamente`() {
        val activity =
                mockk<FragmentActivity>(
                        relaxed = true,
                        moreInterfaces = arrayOf(FloatingButtonVisibilityHost::class)
                )
        viewModel.setupMenuCallback(activity)

        viewModel.navigationController?.onMenuClosedCallback?.invoke(null)

        verify(exactly = 1) {
            (activity as FloatingButtonVisibilityHost).fadeFloatingButtonImmediately()
        }
    }

    // ---------------------------------------------------------------------------------------
    // handleMenuClosed: FrameRendered wait branch (the existing test only covers retroView == null)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `fechar o menu so esconde o preview depois do FrameRendered quando ha retroView`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val eventsFlow = MutableSharedFlow<GLRetroView.GLRetroEvents>(extraBufferCapacity = 1)
        every { glRetroView.getGLRetroEvents() } returns eventsFlow
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel.retroView = retroView

        val sentinel = mockk<Bitmap>(relaxed = true)
        var lastPreviewBitmap: Bitmap? = sentinel
        viewModel.loadPreviewCallback = { lastPreviewBitmap = it }

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        viewModel.navigationController?.onMenuClosedCallback?.invoke(null)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame("preview must stay visible until FrameRendered fires", sentinel, lastPreviewBitmap)

        eventsFlow.tryEmit(GLRetroView.GLRetroEvents.FrameRendered)
        shadowOf(Looper.getMainLooper()).idle()

        assertNull("preview must be hidden once FrameRendered fires", lastPreviewBitmap)
    }

    // ---------------------------------------------------------------------------------------
    // gamepadMenuButtonCallback
    // ---------------------------------------------------------------------------------------

    @Test
    fun `gamepadMenuButtonCallback fecha todos os menus quando ha um menu ativo`() {
        val navController = setupWithMockNavController()
        every { navController.isMenuActive() } returns true

        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")
        controllerInput.callbacks.gamepadMenuButtonCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.CloseAllMenus })
        }
    }

    @Test
    fun `gamepadMenuButtonCallback abre o menu quando nenhum menu esta ativo`() {
        val navController = setupWithMockNavController()
        every { navController.isMenuActive() } returns false

        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")
        controllerInput.callbacks.gamepadMenuButtonCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.OpenMenu })
        }
    }

    // ---------------------------------------------------------------------------------------
    // menuNavigateXCallback direction correctness
    // ---------------------------------------------------------------------------------------

    private fun setupWithMockNavController(): NavigationController {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)
        val navController = mockk<NavigationController>(relaxed = true)
        viewModel.navigationController = navController
        return navController
    }

    @Test
    fun `menuNavigateUpCallback envia Navigate com direction UP`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuNavigateUpCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match {
                        it is NavigationEvent.Navigate &&
                                it.direction ==
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction.UP
                    }
            )
        }
    }

    @Test
    fun `menuNavigateDownCallback envia Navigate com direction DOWN`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuNavigateDownCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match {
                        it is NavigationEvent.Navigate &&
                                it.direction ==
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .DOWN
                    }
            )
        }
    }

    @Test
    fun `menuNavigateLeftCallback envia Navigate com direction LEFT`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuNavigateLeftCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match {
                        it is NavigationEvent.Navigate &&
                                it.direction ==
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .LEFT
                    }
            )
        }
    }

    @Test
    fun `menuNavigateRightCallback envia Navigate com direction RIGHT`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuNavigateRightCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match {
                        it is NavigationEvent.Navigate &&
                                it.direction ==
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .RIGHT
                    }
            )
        }
    }

    @Test
    fun `menuConfirmCallback envia ActivateSelected`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuConfirmCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.ActivateSelected })
        }
    }

    @Test
    fun `menuBackCallback envia NavigateBack com KEYCODE_BUTTON_B`() {
        val navController = setupWithMockNavController()
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        controllerInput.callbacks.menuBackCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match {
                        it is NavigationEvent.NavigateBack &&
                                it.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_B
                    }
            )
        }
    }

    // ---------------------------------------------------------------------------------------
    // isMenuOperationSafe / shouldHandleStartButton
    // ---------------------------------------------------------------------------------------

    @Test
    fun `isMenuOperationSafe retorna false quando esta dismissing all menus`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")
        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")
        menuStateManager.setDismissingAllMenus(true)

        assertFalse(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }

    @Test
    fun `isMenuOperationSafe retorna false quando o fragmento esta dismissing`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isDismissingMenu() } returns true
        viewModel.updateRetroMenu3FragmentReference(fragment)

        assertFalse(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }

    @Test
    fun `isMenuOperationSafe retorna true quando nada esta dismissing`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")

        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isDismissingMenu() } returns false
        viewModel.updateRetroMenu3FragmentReference(fragment)

        assertTrue(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }

    @Test
    fun `shouldHandleStartButton so retorna true quando o menu esta ativo e nao esta dismissing`() {
        val navController = setupWithMockNavController()
        every { navController.isMenuActive() } returns true
        val controllerInput = getPrivateField<com.vinaooo.revenger.input.ControllerInput>(viewModel, "controllerInput")
        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")

        assertTrue(controllerInput.callbacks.shouldHandleStartButton.invoke())

        menuStateManager.setDismissingAllMenus(true)
        assertFalse(controllerInput.callbacks.shouldHandleStartButton.invoke())
    }
}
