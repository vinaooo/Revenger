package com.vinaooo.revenger.viewmodels.menu

import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.Direction
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuNavigationCallbackWiring]'s dependencies are read through provider lambdas backed by local
 * vars here, standing in for `GameActivityViewModel`'s own fields. [ControllerInput] is a real
 * (untouched) instance, matching how `GameActivityViewModel` wires it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuNavigationCallbackWiring_test {

    private lateinit var controllerInput: ControllerInput
    private var currentNavigationController: NavigationController? = null
    private var anyMenuActiveResult = false
    private var dismissingAllMenusResult = false
    private var currentFragment: RetroMenu3Fragment? = null
    private val initializedActivities = mutableListOf<FragmentActivity>()
    private lateinit var wiring: MenuNavigationCallbackWiring

    @Before
    fun setUp() {
        controllerInput = ControllerInput()
        currentNavigationController = null
        anyMenuActiveResult = false
        dismissingAllMenusResult = false
        currentFragment = null
        initializedActivities.clear()
        wiring =
                MenuNavigationCallbackWiring(
                        controllerInput = { controllerInput },
                        navigationController = { currentNavigationController },
                        isAnyMenuActive = { anyMenuActiveResult },
                        isDismissingAllMenus = { dismissingAllMenusResult },
                        retroMenu3Fragment = { currentFragment },
                        initializeNavigationController = { activity ->
                            initializedActivities.add(activity)
                        }
                )
    }

    @Test
    fun `setupMenuCallback chama initializeNavigationController com a activity`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        wiring.setupMenuCallback(activity)

        assertEquals(listOf(activity), initializedActivities)
    }

    @Test
    fun `gamepadMenuButtonCallback fecha todos os menus quando ha um menu ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        currentNavigationController = navController
        anyMenuActiveResult = true
        wiring.setupMenuCallback(mockk(relaxed = true))

        controllerInput.callbacks.gamepadMenuButtonCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.CloseAllMenus })
        }
    }

    @Test
    fun `gamepadMenuButtonCallback abre o menu quando nenhum menu esta ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        currentNavigationController = navController
        anyMenuActiveResult = false
        wiring.setupMenuCallback(mockk(relaxed = true))

        controllerInput.callbacks.gamepadMenuButtonCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.OpenMenu })
        }
    }

    @Test
    fun `menuNavigateUpCallback envia Navigate com direction UP`() {
        val navController = mockk<NavigationController>(relaxed = true)
        currentNavigationController = navController
        wiring.setupMenuCallback(mockk(relaxed = true))

        controllerInput.callbacks.menuNavigateUpCallback.invoke()

        verify(exactly = 1) {
            navController.handleNavigationEvent(
                    match { it is NavigationEvent.Navigate && it.direction == Direction.UP }
            )
        }
    }

    @Test
    fun `isMenuOperationSafe retorna false quando esta dismissing all menus`() {
        wiring.setupMenuCallback(mockk(relaxed = true))
        dismissingAllMenusResult = true

        assertFalse(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }

    @Test
    fun `isMenuOperationSafe retorna false quando o fragmento esta dismissing`() {
        wiring.setupMenuCallback(mockk(relaxed = true))
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isDismissingMenu() } returns true
        currentFragment = fragment

        assertFalse(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }

    @Test
    fun `isMenuOperationSafe retorna true quando nada esta dismissing`() {
        wiring.setupMenuCallback(mockk(relaxed = true))

        assertTrue(controllerInput.callbacks.isMenuOperationSafe.invoke())
    }
}
