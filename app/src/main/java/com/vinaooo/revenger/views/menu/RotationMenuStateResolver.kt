package com.vinaooo.revenger.views.menu

import androidx.fragment.app.Fragment
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType

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
 * 2. **Otherwise the visible fragment decides**, when it is one of the nine known submenu types.
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
                    is CoreVariablesFragment -> MenuState.CORE_VARIABLES_MENU
                    is ExitFragment -> MenuState.EXIT_MENU
                    is SaveSlotsFragment -> MenuState.SAVE_SLOTS_MENU
                    is LoadSlotsFragment -> MenuState.LOAD_SLOTS_MENU
                    is ManageSavesFragment -> MenuState.MANAGE_SAVES_MENU
                    is ExitSaveGridFragment -> MenuState.EXIT_SAVE_SLOTS_MENU
                    else -> null
                }

        return submenuState ?: currentState
    }

    /**
     * Maps a rotation-recreation [MenuState] to the [MenuType] the NavigationController's state
     * should be synced to afterwards.
     */
    fun resolveNavigationMenuType(state: MenuState): MenuType =
            when (state) {
                MenuState.MAIN_MENU -> MenuType.MAIN
                MenuState.SETTINGS_MENU -> MenuType.SETTINGS
                MenuState.PROGRESS_MENU -> MenuType.PROGRESS
                MenuState.ABOUT_MENU -> MenuType.ABOUT
                MenuState.CORE_VARIABLES_MENU -> MenuType.CORE_VARIABLES
                MenuState.EXIT_MENU -> MenuType.EXIT
                MenuState.SAVE_SLOTS_MENU -> MenuType.SAVE_SLOTS
                MenuState.LOAD_SLOTS_MENU -> MenuType.LOAD_SLOTS
                MenuState.MANAGE_SAVES_MENU -> MenuType.MANAGE_SAVES
                MenuState.EXIT_SAVE_SLOTS_MENU -> MenuType.EXIT_SAVE_SLOTS
            }

    /**
     * The submenu a nested submenu opens from, which the rotation rebuild must put back underneath
     * it so Back returns there. Core Variables opens from About, the Save, Load and Manage grids
     * from Progress, and the exit save grid from Exit, all through the NavigationController, so
     * the menu manager stays on that parent while they are open. Every other state returns null.
     *
     * @param backStackCount backstack entries before the rotation. The parent is only returned
     *   when there were at least two, i.e. the parent really was underneath. The PiP "Save and
     *   Exit" path opens the exit save grid on its own (one entry), and Back from there closes the
     *   menu, so it must not get an Exit menu underneath.
     */
    fun resolveParentState(state: MenuState, backStackCount: Int): MenuState? {
        if (backStackCount < NESTED_BACK_STACK_COUNT) return null
        return when (state) {
            MenuState.CORE_VARIABLES_MENU -> MenuState.ABOUT_MENU
            MenuState.SAVE_SLOTS_MENU,
            MenuState.LOAD_SLOTS_MENU,
            MenuState.MANAGE_SAVES_MENU -> MenuState.PROGRESS_MENU
            MenuState.EXIT_SAVE_SLOTS_MENU -> MenuState.EXIT_MENU
            else -> null
        }
    }

    /** A nested submenu and the parent under it: two backstack entries. */
    private const val NESTED_BACK_STACK_COUNT = 2
}
