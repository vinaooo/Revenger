package com.vinaooo.revenger.views.menu

import androidx.fragment.app.Fragment
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment

/**
 * Decides which [MenuState] a rotation-triggered menu recreation should rebuild, given what was
 * visible before the rotation.
 *
 * Pure: no I/O, no Activity/Fragment mutation, no logging. Extracted verbatim (behaviour-wise) from
 * `GameActivity.onConfigurationChanged`'s `effectiveState` computation.
 *
 * The rules, in priority order:
 * 1. **No backstack wins over everything.** If the backstack is empty the menu hierarchy is just
 *    the main menu, so [MenuState.MAIN_MENU] is returned regardless of which fragment happens to
 *    be visible and regardless of [currentState]. The visible fragment can be momentarily stale
 *    right after a BACK operation, which is exactly why the backstack is trusted first.
 * 2. **Otherwise the visible fragment decides**, when it is one of the eight known submenu types.
 * 3. **Otherwise fall back to [currentState]** — the menu manager's own idea of where it is.
 */
object RotationMenuStateResolver {

    /**
     * @param visibleFragment fragment currently occupying the menu container, captured *before* the
     *   rotation settle delay.
     * @param hasBackStack whether the fragment manager had any backstack entries.
     * @param currentState the menu manager's captured state, used only as a fallback.
     * @return the [MenuState] the rotation recreation should rebuild.
     */
    fun resolve(
            visibleFragment: Fragment?,
            hasBackStack: Boolean,
            currentState: MenuState
    ): MenuState {
        if (!hasBackStack) {
            // Backstack empty: ALWAYS use MAIN_MENU.
            return MenuState.MAIN_MENU
        }

        val submenuState =
                when (visibleFragment) {
                    is SettingsMenuFragment -> MenuState.SETTINGS_MENU
                    is ProgressFragment -> MenuState.PROGRESS_MENU
                    is AboutFragment -> MenuState.ABOUT_MENU
                    is ExitFragment -> MenuState.EXIT_MENU
                    is SaveSlotsFragment -> MenuState.SAVE_SLOTS_MENU
                    is LoadSlotsFragment -> MenuState.LOAD_SLOTS_MENU
                    is ManageSavesFragment -> MenuState.MANAGE_SAVES_MENU
                    is ExitSaveGridFragment -> MenuState.EXIT_SAVE_SLOTS_MENU
                    else -> null
                }

        return submenuState ?: currentState
    }
}
