package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState

/**
 * The dismiss/isOpen query surface for the four submenu fragments (Settings/Progress/Exit/About).
 * There is no `isAboutMenuOpen` -- the original class never had one either.
 */
interface SubmenuFragmentDismissal {
    fun isSettingsMenuOpen(): Boolean
    fun isProgressMenuOpen(): Boolean
    fun isExitMenuOpen(): Boolean
    fun dismissSettingsMenu()
    fun dismissProgress()
    fun dismissExit()
    fun dismissAboutMenu()
}

/**
 * Implementation of [SubmenuFragmentDismissal]. [menuManager] is read lazily via a provider, not
 * captured at construction time, because callers may replace that dependency by reflection (in
 * tests) after this dismisser is already built. Restoring the main menu after a dismiss is left
 * to the BackStackChangeListener in `RetroMenu3Fragment`.
 */
class SubmenuFragmentDismisser(
        private val state: SubmenuFragmentState,
        private val menuManager: () -> MenuManager,
        private val menuStateManager: MenuStateManager
) : SubmenuFragmentDismissal {

    override fun isSettingsMenuOpen(): Boolean = state.settingsMenuFragment != null

    override fun isProgressMenuOpen(): Boolean = state.progressFragment != null

    override fun isExitMenuOpen(): Boolean = state.exitFragment != null

    /** Helper method to dismiss submenu fragments with common cleanup logic */
    private fun dismissSubmenuFragment(
            fragment: androidx.fragment.app.Fragment?,
            fragmentName: String,
            activeFlagSetter: () -> Unit
    ) {
        android.util.Log.d("GameActivityViewModel", "dismiss${fragmentName}: Starting")

        // Check if fragment is still valid and added
        if (fragment == null || !fragment.isAdded) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: Fragment is null or not added, skipping dismiss"
            )
            return
        }

        // IMPORTANT: Since submenu fragments were added to the back stack,
        // we must use popBackStack() instead of manual remove()
        // This ensures FragmentManager properly manages the hierarchy

        // Check if there's anything in the back stack before trying to remove
        val activity = fragment.activity
        if (activity != null) {
            val fragmentManager = activity.supportFragmentManager
            val backStackCount = fragmentManager.backStackEntryCount

            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: backStackCount = $backStackCount"
            )

            if (backStackCount > 0) {
                // Use popBackStack to remove the fragment correctly
                android.util.Log.d(
                        "GameActivityViewModel",
                        "dismiss${fragmentName}: Calling popBackStackImmediate()"
                )
                fragmentManager.popBackStackImmediate()
            } else {
                android.util.Log.w(
                        "GameActivityViewModel",
                        "dismiss${fragmentName}: Back stack is empty, nothing to pop"
                )
            }
        }

        // Clear the fragment reference and flag
        activeFlagSetter()

        android.util.Log.d("GameActivityViewModel", "dismiss${fragmentName}: Completed")
    }

    override fun dismissSettingsMenu() {
        dismissSubmenuFragment(state.settingsMenuFragment, "SettingsMenu") {
            state.settingsMenuFragment = null
            menuStateManager.deactivateMenu(MenuSystemState.MenuType.SETTINGS_MENU)
            // Navigate back to main menu when dismissing Settings submenu
            menuManager().navigateToState(MenuState.MAIN_MENU)
        }
    }

    override fun dismissProgress() {
        dismissSubmenuFragment(state.progressFragment, "Progress") {
            state.progressFragment = null
            menuStateManager.deactivateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
            // Navigate back to main menu when dismissing Progress submenu
            menuManager().navigateToState(MenuState.MAIN_MENU)
        }
    }

    override fun dismissExit() {
        dismissSubmenuFragment(state.exitFragment, "Exit") {
            state.exitFragment = null
            menuStateManager.deactivateMenu(MenuSystemState.MenuType.EXIT_MENU)
            // Navigate back to main menu when dismissing Exit submenu
            menuManager().navigateToState(MenuState.MAIN_MENU)
        }
    }

    override fun dismissAboutMenu() {
        dismissSubmenuFragment(state.aboutFragment, "About") {
            state.aboutFragment = null
            menuStateManager.deactivateMenu(MenuSystemState.MenuType.ABOUT_MENU)
            // Navigate back to main menu when dismissing About submenu
            menuManager().navigateToState(MenuState.MAIN_MENU)
        }
    }
}
