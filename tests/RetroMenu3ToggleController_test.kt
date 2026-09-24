package com.vinaooo.revenger.viewmodels.menu

import android.os.Looper
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.viewmodels.InputViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [RetroMenu3ToggleController]'s [MenuStateManager] and [ControllerInput] dependencies are real
 * (untouched) instances, matching how `GameActivityViewModel` wires them; [NavigationController],
 * [RetroMenu3Fragment] and [InputViewModel] are mocked since this class only calls into them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroMenu3ToggleController_test {

    private var currentNavigationController: NavigationController? = null
    private var currentFragment: RetroMenu3Fragment? = null
    private lateinit var menuStateManager: MenuStateManager
    private lateinit var inputViewModel: InputViewModel
    private lateinit var controllerInput: ControllerInput
    private var retroMenu3OpenResult = false
    private lateinit var controller: RetroMenu3ToggleController

    @Before
    fun setUp() {
        currentNavigationController = null
        currentFragment = null
        menuStateManager = MenuStateManager()
        inputViewModel = mockk(relaxed = true)
        controllerInput = ControllerInput()
        retroMenu3OpenResult = false
        controller =
                RetroMenu3ToggleController(
                        navigationController = { currentNavigationController },
                        retroMenu3Fragment = { currentFragment },
                        menuStateManager = menuStateManager,
                        inputViewModel = inputViewModel,
                        controllerInput = { controllerInput },
                        isRetroMenu3Open = { retroMenu3OpenResult }
                )
    }

    // ---------------------------------------------------------------------------------------
    // toggleMainMenu
    // ---------------------------------------------------------------------------------------

    @Test
    fun `toggleMainMenu fecha todos os menus quando ja ha um menu ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns true
        currentNavigationController = navController

        controller.toggleMainMenu()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.CloseAllMenus })
        }
    }

    @Test
    fun `toggleMainMenu abre o menu quando nenhum menu esta ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns false
        currentNavigationController = navController

        controller.toggleMainMenu()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.OpenMenu })
        }
    }

    @Test
    fun `toggleMainMenu nao lanca quando nao ha NavigationController`() {
        controller.toggleMainMenu()
    }

    // ---------------------------------------------------------------------------------------
    // dismissRetroMenu3
    // ---------------------------------------------------------------------------------------

    @Test
    fun `dismissRetroMenu3 sincroniza o NavigationController e o menuStateManager ao completar a animacao`() {
        val navController = mockk<NavigationController>(relaxed = true)
        currentNavigationController = navController

        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.dismissMenuPublic(any()) } answers
                {
                    @Suppress("UNCHECKED_CAST")
                    (it.invocation.args[0] as? (() -> Unit))?.invoke()
                }
        currentFragment = fragment
        menuStateManager.setRetroMenu3Open(true)

        var animationEndCalled = false
        controller.dismissRetroMenu3 { animationEndCalled = true }

        verify(exactly = 1) { navController.closeMenuExternal() }
        assertFalse(menuStateManager.isRetroMenu3Open())
        assertTrue(animationEndCalled)
    }

    @Test
    fun `dismissRetroMenu3 nao invoca onAnimationEnd quando nao ha fragmento`() {
        var animationEndCalled = false

        controller.dismissRetroMenu3 { animationEndCalled = true }

        assertFalse(animationEndCalled)
    }

    // ---------------------------------------------------------------------------------------
    // clearControllerInputState
    // ---------------------------------------------------------------------------------------

    @Test
    fun `clearControllerInputState delega ao InputViewModel somente apos o delay`() {
        controller.clearControllerInputState()

        verify(exactly = 0) { inputViewModel.clearControllerInputState() }

        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250))

        verify(exactly = 1) { inputViewModel.clearControllerInputState() }
    }

    // ---------------------------------------------------------------------------------------
    // isAnyMenuActive
    // ---------------------------------------------------------------------------------------

    @Test
    fun `isAnyMenuActive retorna false quando nao ha NavigationController`() {
        assertFalse(controller.isAnyMenuActive())
    }

    @Test
    fun `isAnyMenuActive delega ao NavigationController quando presente`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns true
        currentNavigationController = navController

        assertTrue(controller.isAnyMenuActive())
    }
}
