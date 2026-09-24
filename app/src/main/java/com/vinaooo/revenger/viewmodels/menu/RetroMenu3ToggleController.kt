package com.vinaooo.revenger.viewmodels.menu

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.viewmodels.InputViewModel

/** Open/close the RetroMenu3 overlay and report whether any menu is currently active. */
interface RetroMenu3ToggleFacade {
    fun toggleMainMenu()
    fun dismissRetroMenu3(onAnimationEnd: (() -> Unit)? = null)
    fun clearControllerInputState()
    fun isAnyMenuActive(): Boolean
}

/**
 * Implementation of [RetroMenu3ToggleFacade]. [navigationController], [retroMenu3Fragment] and
 * [controllerInput] are read via lambdas rather than captured, since all three are mutated on the
 * owning ViewModel after construction.
 */
class RetroMenu3ToggleController(
        private val navigationController: () -> NavigationController?,
        private val retroMenu3Fragment: () -> RetroMenu3Fragment?,
        private val menuStateManager: MenuStateManager,
        private val inputViewModel: InputViewModel,
        private val controllerInput: () -> ControllerInput,
        private val isRetroMenu3Open: () -> Boolean
) : RetroMenu3ToggleFacade {

    companion object {
        // Delay before clearing state after dismissing the RetroMenu3 fragment, to let the
        // pending fragment removal complete first.
        private const val RETRO_MENU3_FRAGMENT_REMOVAL_SETTLE_DELAY_MS = 200L

        // Delay before clearing controller input state, to let the pending fragment
        // destruction complete first.
        private const val CONTROLLER_STATE_CLEAR_FRAGMENT_DESTROY_SETTLE_DELAY_MS = 200L
    }

    override fun toggleMainMenu() {
        if (isAnyMenuActive()) {
            Log.d(
                    "GameActivityViewModel",
                    "[MENU_TOGGLE] Closing ALL menus directly with NavigationController via FloatingButton"
            )
            navigationController()
                    ?.handleNavigationEvent(
                            NavigationEvent.CloseAllMenus(
                                    inputSource = InputSource.PHYSICAL_GAMEPAD // Treat floating
                                    // button like a physical button for behavior
                                    )
                    )
        } else {
            Log.d("GameActivityViewModel", "[MENU_TOGGLE] Opening menu via FloatingButton")
            navigationController()
                    ?.handleNavigationEvent(
                            NavigationEvent.OpenMenu(
                                    inputSource = InputSource.PHYSICAL_GAMEPAD // Treat floating
                                    // button like a physical button for behavior
                                    )
                    )
        }
    }

    override fun dismissRetroMenu3(onAnimationEnd: (() -> Unit)?) {
        Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Starting")
        Log.d(
                "GameActivityViewModel",
                "[DISMISS_MAIN] dismissRetroMenu3: isRetroMenu3Open before dismiss: " +
                        "${isRetroMenu3Open()}"
        )

        retroMenu3Fragment()?.dismissMenuPublic {
            // Ensure NavigationController is synchronized and its state is reset
            navigationController()?.closeMenuExternal()

            // Explicitly tell MenuManager that we are closed
            menuStateManager.setRetroMenu3Open(false)

            onAnimationEnd?.invoke()
        }

        // CRITICAL: Add small delay before clearing keyLog to ensure fragment is fully removed
        // This prevents comboAlreadyTriggered from staying true when menu closes
        Handler(Looper.getMainLooper())
                .postDelayed(
                        {
                            Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: DELAYED - " +
                                            "isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
                            )
                            Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: DELAYED - clearing keyLog now"
                            )

                            Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: Menu dismissed"
                            )
                        },
                        RETRO_MENU3_FRAGMENT_REMOVAL_SETTLE_DELAY_MS
                ) // Delay to ensure fragment removal is complete

        Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Completed")
    }

    override fun clearControllerInputState() {
        Log.d("GameActivityViewModel", "[CLEAR_STATE] clearControllerInputState: STARTING")
        Log.d(
                "GameActivityViewModel",
                "[CLEAR_STATE] clearControllerInputState: comboAlreadyTriggered before: " +
                        "${controllerInput().comboTracker.getComboAlreadyTriggered()}"
        )
        Log.d(
                "GameActivityViewModel",
                "[CLEAR_STATE] clearControllerInputState: isRetroMenu3Open: ${isRetroMenu3Open()}"
        )

        // Add small delay to ensure fragment is fully destroyed before clearing combo state
        Handler(Looper.getMainLooper())
                .postDelayed(
                        {
                            Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: DELAYED - clearing now"
                            )
                            Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: " +
                                            "isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
                            )
                            inputViewModel.clearControllerInputState()
                            Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: " +
                                            "comboAlreadyTriggered after: " +
                                            "${controllerInput().comboTracker.getComboAlreadyTriggered()}"
                            )
                            Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: COMPLETED"
                            )
                        },
                        CONTROLLER_STATE_CLEAR_FRAGMENT_DESTROY_SETTLE_DELAY_MS
                ) // Delay to ensure fragment destruction is complete
    }

    override fun isAnyMenuActive(): Boolean {
        Log.d(
                "GameActivityViewModel",
                "[ACTIVE] 🔍 isAnyMenuActive: ========== CHECKING MENU ACTIVITY =========="
        )

        // PHASE 3: Use NavigationController for menu detection (permanently enabled)
        val navController = navigationController()
        if (navController != null) {
            val navControllerActive = navController.isMenuActive()
            Log.d(
                    "GameActivityViewModel",
                    "[ACTIVE] ✅ Using NavigationController: isMenuActive=$navControllerActive"
            )
            return navControllerActive
        }

        // navigationController is set once, at the end of onCreate() (setupMenuCallback()), and
        // never cleared afterward -- every real caller of isAnyMenuActive() already runs after
        // that. Confirmed on-device (menu open/navigate/close/background/foreground) that this
        // branch is never reached; it's a safe default for the narrow window before that.
        return false
    }
}
