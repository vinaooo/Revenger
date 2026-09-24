package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState

/**
 * Handles [MenuEvent.StateChanged]: activates the destination submenu's
 * [MenuSystemState.MenuType] and deactivates the source submenu's, for whichever of the four
 * submenus (Settings/Progress/About/Exit) is involved. [isAnyMenuActive] and [isRetroMenu3Open]
 * are read via provider lambdas rather than captured, since they reflect state mutated on the
 * owning ViewModel after construction; both are used here only for diagnostic logging.
 */
class MenuStateChangeHandler(
        private val menuStateManager: MenuStateManager,
        private val isAnyMenuActive: () -> Boolean,
        private val isRetroMenu3Open: () -> Boolean
) {

    fun handleStateChanged(event: MenuEvent.StateChanged) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 ========== MENU STATE CHANGED =========="
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 From: ${event.from} -> To: ${event.to}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 isAnyMenuActive before=${isAnyMenuActive()}"
        )

        // Activate/deactivate menus based on state changes
        when (event.to) {
            MenuState.MAIN_MENU -> {
                android.util.Log.d(
                        "GameActivityViewModel",
                        "[STATE_CHANGE] 🎮 State changed to MAIN_MENU - retroMenu3Open=${isRetroMenu3Open()}"
                )
                // Main menu is always active when RetroMenu3 is open
                // No need to activate/deactivate here
            }
            MenuState.SETTINGS_MENU ->
                    menuStateManager.activateMenu(MenuSystemState.MenuType.SETTINGS_MENU)
            MenuState.PROGRESS_MENU ->
                    menuStateManager.activateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
            MenuState.ABOUT_MENU ->
                    menuStateManager.activateMenu(MenuSystemState.MenuType.ABOUT_MENU)
            MenuState.EXIT_MENU ->
                    menuStateManager.activateMenu(MenuSystemState.MenuType.EXIT_MENU)
            else -> {}
        }

        // Deactivate previous menu if it was a submenu
        when (event.from) {
            MenuState.SETTINGS_MENU ->
                    menuStateManager.deactivateMenu(MenuSystemState.MenuType.SETTINGS_MENU)
            MenuState.PROGRESS_MENU ->
                    menuStateManager.deactivateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
            MenuState.ABOUT_MENU ->
                    menuStateManager.deactivateMenu(MenuSystemState.MenuType.ABOUT_MENU)
            MenuState.EXIT_MENU ->
                    menuStateManager.deactivateMenu(MenuSystemState.MenuType.EXIT_MENU)
            else -> {
                // No deactivation needed for MAIN_MENU or other states
            }
        }

        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 isAnyMenuActive after=${isAnyMenuActive()}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 ========== MENU STATE CHANGED END =========="
        )
    }
}
