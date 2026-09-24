package com.vinaooo.revenger.viewmodels.menu

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.Direction
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/** Configures the NavigationController/KeyboardInputAdapter and wires ControllerInput's menu callbacks. */
interface MenuNavigationCallbackWiringFacade {
    fun setupMenuCallback(activity: FragmentActivity)
}

/**
 * Implementation of [MenuNavigationCallbackWiringFacade]. [controllerInput], [navigationController]
 * and [retroMenu3Fragment] are read via provider lambdas rather than captured, since all three are
 * mutated on the owning ViewModel after construction.
 */
class MenuNavigationCallbackWiring(
        private val controllerInput: () -> ControllerInput,
        private val navigationController: () -> NavigationController?,
        private val isAnyMenuActive: () -> Boolean,
        private val isDismissingAllMenus: () -> Boolean,
        private val retroMenu3Fragment: () -> RetroMenu3Fragment?,
        private val initializeNavigationController: (FragmentActivity) -> Unit
) : MenuNavigationCallbackWiringFacade {

    override fun setupMenuCallback(activity: FragmentActivity) {
        initializeNavigationController(activity)
        wireControllerInputMenuCallbacks()
    }

    /**
     * Wires the 12 `ControllerInput` callback/predicate properties that [setupMenuCallback]
     * re-runs unconditionally on every call (unlike the one-time NavigationController setup).
     * Collapsed into a single `copy()` of [com.vinaooo.revenger.input.ControllerInputCallbacks] so
     * every other field -- the 4 set in `GameActivityViewModel.init {}` -- is preserved as-is.
     */
    private fun wireControllerInputMenuCallbacks() {
        controllerInput().callbacks =
                controllerInput().callbacks.copy(
                        gamepadMenuButtonCallback = ::handleGamepadMenuButtonCallback,
                        menuNavigateUpCallback = menuNavigateCallback(Direction.UP),
                        menuNavigateDownCallback = menuNavigateCallback(Direction.DOWN),
                        menuNavigateLeftCallback = menuNavigateCallback(Direction.LEFT),
                        menuNavigateRightCallback = menuNavigateCallback(Direction.RIGHT),
                        menuConfirmCallback = {
                            navigationController()
                                    ?.handleNavigationEvent(
                                            NavigationEvent.ActivateSelected(
                                                    inputSource = InputSource.PHYSICAL_GAMEPAD
                                            )
                                    )
                        },
                        menuBackCallback = {
                            navigationController()
                                    ?.handleNavigationEvent(
                                            NavigationEvent.NavigateBack(
                                                    keyCode =
                                                            android.view.KeyEvent
                                                                    .KEYCODE_BUTTON_B,
                                                    inputSource = InputSource.PHYSICAL_GAMEPAD
                                            )
                                    )
                        },

                        // CRITICAL: DO NOT check isDismissingAllMenus() here! We need to keep
                        // intercepting buttons even during closing to prevent ACTION_UP from
                        // leaking into the game.
                        shouldInterceptDpadForMenu = { isAnyMenuActive() },

                        // Only when RetroMenu3 or SettingsMenu is REALLY open.
                        shouldHandleStartButton = {
                            isAnyMenuActive() && !isDismissingAllMenus()
                        },
                        shouldBlockAllGamepadInput = { isAnyMenuActive() },
                        isRetroMenu3Open = { isAnyMenuActive() },
                        isMenuOperationSafe = {
                            !isDismissingAllMenus() &&
                                    retroMenu3Fragment()?.isDismissingMenu() != true
                        }
                )
    }

    /** Toggle the menu open/closed in response to the physical gamepad's menu button. */
    private fun handleGamepadMenuButtonCallback() {
        if (isAnyMenuActive()) {
            Log.d(
                    "GameActivityViewModel",
                    "[MENU_BUTTON] Closing ALL menus directly with NavigationController"
            )
            navigationController()
                    ?.handleNavigationEvent(
                            NavigationEvent.CloseAllMenus(
                                    inputSource = InputSource.PHYSICAL_GAMEPAD
                            )
                    )
        } else {
            navigationController()
                    ?.handleNavigationEvent(
                            NavigationEvent.OpenMenu(inputSource = InputSource.PHYSICAL_GAMEPAD)
                    )
        }
    }

    /**
     * Builds a menu-navigate callback for [direction]; the four `menuNavigateXCallback` fields
     * used to carry four verbatim copies of this same dispatch with only the direction changed.
     */
    private fun menuNavigateCallback(direction: Direction): () -> Unit = {
        navigationController()
                ?.handleNavigationEvent(
                        NavigationEvent.Navigate(
                                direction = direction,
                                inputSource = InputSource.PHYSICAL_GAMEPAD
                        )
                )
    }
}
