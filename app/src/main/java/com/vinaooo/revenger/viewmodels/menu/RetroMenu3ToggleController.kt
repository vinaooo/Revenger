package com.vinaooo.revenger.viewmodels.menu

import android.util.Log
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/** Open/close the RetroMenu3 overlay and report whether any menu is currently active. */
interface RetroMenu3ToggleFacade {
    fun toggleMainMenu()
    fun dismissRetroMenu3(onAnimationEnd: (() -> Unit)? = null)
    fun isAnyMenuActive(): Boolean
}

/**
 * Implementation of [RetroMenu3ToggleFacade]. [navigationController] and [retroMenu3Fragment] are
 * read via lambdas rather than captured, since both are mutated on the owning ViewModel after
 * construction.
 */
class RetroMenu3ToggleController(
        private val navigationController: () -> NavigationController?,
        private val retroMenu3Fragment: () -> RetroMenu3Fragment?,
        private val menuStateManager: MenuStateManager,
        private val isRetroMenu3Open: () -> Boolean
) : RetroMenu3ToggleFacade {

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

        Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Completed")
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
