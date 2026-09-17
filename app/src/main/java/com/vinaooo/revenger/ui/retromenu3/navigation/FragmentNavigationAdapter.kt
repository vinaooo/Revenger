package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment

/**
 * ID of the container where menu fragments are displayed. This is the FrameLayout defined in
 * activity_game.xml. Shared by [MenuFragmentPresenter]/[SimpleMenuShower] (which show fragments
 * into it) and [FragmentNavigationAdapter] (which looks up the currently visible one in
 * [FragmentNavigationAdapter.hideMenu]).
 */
private val menuContainerId = R.id.menu_container

/** Shared log tag for every class in this file that performs a Fragment transaction. */
private const val TAG = "FragmentNavigationAdapter"

/**
 * Shows the menus whose transaction is a uniform replace + addToBackStack + commit (everything
 * except MAIN, which uses add()/commitNow() to guarantee synchronous availability, and PROGRESS,
 * which first clears any grid submenus already on the back stack). Split out of
 * [MenuFragmentPresenter] purely to stay under the project's function-count threshold -- these
 * eight menus don't otherwise share any logic beyond the identical transaction shape. Only
 * [show] is called from outside this class, so it is injected directly into
 * [MenuFragmentPresenter] rather than exposed via interface delegation.
 */
class SimpleMenuShower(private val fragmentManager: FragmentManager) {

    /** Displays one of the "simple" (uniform replace-transaction) menus. */
    fun show(menuType: MenuType) {
        when (menuType) {
            MenuType.SETTINGS -> showSettingsMenu()
            MenuType.ABOUT -> showAboutMenu()
            MenuType.EXIT -> showExitMenu()
            MenuType.CORE_VARIABLES -> showCoreVariablesMenu()
            MenuType.SAVE_SLOTS -> showSaveSlotsMenu()
            MenuType.LOAD_SLOTS -> showLoadSlotsMenu()
            MenuType.MANAGE_SAVES -> showManageSavesMenu()
            MenuType.EXIT_SAVE_SLOTS -> showExitSaveSlotsMenu()
            MenuType.MAIN, MenuType.PROGRESS ->
                    Log.w(TAG, "[SHOW] $menuType is not a simple menu, ignoring")
        }
    }

    private fun showSettingsMenu() {
        Log.d(TAG, "[SHOW] Settings menu")

        val settingsFragment = SettingsMenuFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, settingsFragment, TAG_SETTINGS_MENU)
                .addToBackStack(TAG_SETTINGS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Settings menu added successfully")
    }

    private fun showAboutMenu() {
        Log.d(TAG, "[SHOW] About menu")

        val aboutFragment = AboutFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, aboutFragment, TAG_ABOUT_MENU)
                .addToBackStack(TAG_ABOUT_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] About menu added successfully")
    }

    private fun showExitMenu() {
        Log.d(TAG, "[SHOW] Exit menu")

        val exitFragment = ExitFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, exitFragment, TAG_EXIT_MENU)
                .addToBackStack(TAG_EXIT_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Exit menu added successfully")
    }

    private fun showCoreVariablesMenu() {
        Log.d(TAG, "[SHOW] Core Variables menu")

        val coreVariablesFragment = CoreVariablesFragment()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, coreVariablesFragment, TAG_CORE_VARIABLES_MENU)
                .addToBackStack(TAG_CORE_VARIABLES_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Core Variables menu added successfully")
    }

    private fun showSaveSlotsMenu() {
        Log.d(TAG, "[SHOW] Save Slots menu")

        val saveSlotsFragment = SaveSlotsFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, saveSlotsFragment, TAG_SAVE_SLOTS_MENU)
                .addToBackStack(TAG_SAVE_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Save Slots menu added successfully")
    }

    private fun showLoadSlotsMenu() {
        Log.d(TAG, "[SHOW] Load Slots menu")

        val loadSlotsFragment = LoadSlotsFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, loadSlotsFragment, TAG_LOAD_SLOTS_MENU)
                .addToBackStack(TAG_LOAD_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Load Slots menu added successfully")
    }

    private fun showManageSavesMenu() {
        Log.d(TAG, "[SHOW] Manage Saves menu")

        val manageSavesFragment = ManageSavesFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, manageSavesFragment, TAG_MANAGE_SAVES_MENU)
                .addToBackStack(TAG_MANAGE_SAVES_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Manage Saves menu added successfully")
    }

    private fun showExitSaveSlotsMenu() {
        Log.d(TAG, "[SHOW] Exit Save Slots menu")

        val exitSaveGridFragment = ExitSaveGridFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, exitSaveGridFragment, TAG_EXIT_SAVE_SLOTS_MENU)
                .addToBackStack(TAG_EXIT_SAVE_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Exit Save Slots menu added successfully")
    }

    companion object {
        /** Tags para identificar fragments no FragmentManager */
        private const val TAG_SETTINGS_MENU = "SettingsMenuFragment"
        private const val TAG_ABOUT_MENU = "AboutFragment"
        private const val TAG_EXIT_MENU = "ExitFragment"
        private const val TAG_CORE_VARIABLES_MENU = "CoreVariablesFragment"
        private const val TAG_SAVE_SLOTS_MENU = "SaveSlotsFragment"
        private const val TAG_LOAD_SLOTS_MENU = "LoadSlotsFragment"
        private const val TAG_MANAGE_SAVES_MENU = "ManageSavesFragment"
        private const val TAG_EXIT_SAVE_SLOTS_MENU = "ExitSaveGridFragment"
    }
}

/**
 * Creates and commits the FragmentManager transaction for each [MenuType], mapping it to its
 * concrete Fragment class and tag. Split out of [FragmentNavigationAdapter] (which stays
 * responsible for hide/back/back-stack queries) so it stays under the project's function-count
 * threshold. Only [show] is called from outside this class, so it is injected directly into
 * [FragmentNavigationAdapter] rather than exposed via interface delegation.
 *
 * @see SimpleMenuShower Handles the menus whose transaction doesn't need any special-casing
 */
class MenuFragmentPresenter(private val fragmentManager: FragmentManager) {

    private val simpleMenuShower = SimpleMenuShower(fragmentManager)

    /**
     * Displays the menu specified by MenuType.
     *
     * @param menuType The type of menu to display
     */
    fun show(menuType: MenuType) {
        Log.d(TAG, "[SHOW] Menu type: $menuType")

        when (menuType) {
            MenuType.MAIN -> showMainMenu()
            MenuType.PROGRESS -> showProgressMenu()
            else -> simpleMenuShower.show(menuType)
        }
    }

    private fun showMainMenu() {
        Log.d(TAG, "[SHOW] Main menu")

        // Check if RetroMenu3Fragment already exists
        val existingFragment = fragmentManager.findFragmentByTag(TAG_MAIN_MENU)
        if (existingFragment != null && existingFragment.isAdded) {
            Log.d(TAG, "[SHOW] Main menu already visible")
            return
        }

        // Create new RetroMenu3Fragment
        val mainFragment = RetroMenu3Fragment.newInstance()

        // BUGFIX: Use commitNow() to ensure fragment is added IMMEDIATELY
        // This prevents multiple F12 presses before the fragment is active
        // commitAllowingStateLoss() is ASYNC - allows isMenuActive() to return false
        // even after "added successfully", causing pause/resume imbalance
        fragmentManager
                .beginTransaction()
                .add(menuContainerId, mainFragment, TAG_MAIN_MENU)
                .commitNow()

        // Diagnostic: confirm fragment was added
        try {
            val found = fragmentManager.findFragmentByTag(TAG_MAIN_MENU)
            Log.d(
                    TAG,
                    "[SHOW] Main menu added - fragment=${found?.javaClass?.simpleName} " +
                            "isAdded=${found?.isAdded} " +
                            "backStack=${fragmentManager.backStackEntryCount}"
            )
            // findFragmentByTag() and backStackEntryCount are pure reads with no documented
            // throwable condition; this is a diagnostic-logging safety net that must never break
            // the actual show-menu flow above it, kept via detekt's documented escape hatch.
        } catch (expectedUnreachable: Throwable) {
            Log.w(TAG, "[SHOW] failed to log fragment state after add", expectedUnreachable)
        }

        Log.d(TAG, "[SHOW] Main menu added successfully (synchronous)")
    }

    private fun showProgressMenu() {
        Log.d(TAG, "[SHOW] Progress menu")

        // Clear any existing grid submenus from backstack first
        while (fragmentManager.backStackEntryCount > 1) {
            fragmentManager.popBackStackImmediate()
        }

        val progressFragment = ProgressFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(menuContainerId, progressFragment, TAG_PROGRESS_MENU)
                .addToBackStack(TAG_PROGRESS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Progress menu added successfully")
    }

    companion object {
        /** Tags para identificar fragments no FragmentManager */
        private const val TAG_MAIN_MENU = "RetroMenu3Fragment"
        private const val TAG_PROGRESS_MENU = "ProgressFragment"
    }
}

/**
 * Adapter that isolates Fragment transaction logic from the NavigationController.
 *
 * This class encapsulates all FragmentManager operations (show, hide, add, remove), allowing the
 * NavigationController to work with high-level concepts (MenuType) instead of dealing directly
 * with Fragments and transactions.
 *
 * RESPONSIBILITIES:
 * - Manage fragment transactions (add/remove/show/hide)
 * - Map MenuType to Fragment classes
 * - Maintain references to active fragments
 * - Ensure atomic transactions without crashes
 *
 * IMPORTANT: This class does NOT contain navigation or state logic. It only performs the UI
 * operations requested by the NavigationController.
 *
 * @see MenuFragmentPresenter Owns the actual per-[MenuType] fragment transactions for [showMenu]
 */
class FragmentNavigationAdapter(private val activity: FragmentActivity) {

    private val fragmentManager: FragmentManager = activity.supportFragmentManager
    private val presenter = MenuFragmentPresenter(fragmentManager)

    /**
     * Displays the menu specified by MenuType.
     *
     * @param menuType The type of menu to display
     * @see MenuFragmentPresenter.show
     */
    fun showMenu(menuType: MenuType) {
        presenter.show(menuType)
    }

    /**
     * Hides the current menu.
     *
     * This operation:
     * 1. Removes the fragment from the container
     * 2. Commits the transaction with commitAllowingStateLoss()
     *
     * IMPORTANT: Does not destroy the fragment, only removes it from the screen. This preserves state for
     * possible future restoration.
     */
    fun hideMenu() {
        Log.d(TAG, "[HIDE] Hiding current menu")

        // Encontrar fragment atual no container
        val currentFragment = fragmentManager.findFragmentById(menuContainerId)

        if (currentFragment != null && currentFragment.isAdded) {
            try {
                fragmentManager.beginTransaction().remove(currentFragment).commitAllowingStateLoss()
                Log.d(TAG, "[HIDE] Menu remove requested for fragment=${currentFragment.javaClass.simpleName}")
            } catch (e: IllegalStateException) {
                // commitAllowingStateLoss() can still throw IllegalStateException (e.g. the
                // FragmentManager has already been destroyed), even though it suppresses the
                // "state loss" case.
                Log.e(TAG, "[HIDE] Exception while removing fragment", e)
            }
        } else {
            Log.w(TAG, "[HIDE] No menu to hide (currentFragment=null or not added)")
        }

        // Catch the case where backstack wasn't cleared
        if (fragmentManager.backStackEntryCount > 0) {
            try {
                Log.d(TAG, "[HIDE] Clearing remaining backstack items: ${fragmentManager.backStackEntryCount}")
                fragmentManager.popBackStackImmediate(
                        null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            } catch (e: IllegalStateException) {
                // popBackStackImmediate() throws IllegalStateException if called after the
                // FragmentManager's state has already been saved.
                Log.e(TAG, "[HIDE] Exception while clearing backstack", e)
            }
        }
    }

    /**
     * Navigate back using the FragmentManager back stack.
     *
     * This method:
     * 1. Checks if there are fragments on the back stack
     * 2. Pops the top fragment
     * 3. Shows the main menu again
     * 4. Commits the transaction with commitAllowingStateLoss()
     *
     * @return true if navigated back, false if already at root menu
     */
    fun navigateBack(): Boolean {
        Log.d(TAG, "[BACK] Navigating back")

        // Check if there is an active submenu (back stack not empty)
        if (fragmentManager.backStackEntryCount > 0) {
            // Fazer pop da back stack (volta ao menu anterior). popBackStackImmediate() can
            // return false (e.g. the FragmentManager's state changed between the count check
            // above and this call) -- propagate that instead of always reporting success, so
            // callers can tell a real desync between the logical nav state and what's on
            // screen from an actual successful pop.
            val popped = fragmentManager.popBackStackImmediate()
            Log.d(TAG, "[BACK] popBackStackImmediate() returned: $popped")
            return popped
        } else {
            Log.d(TAG, "[BACK] Already at root menu")
            return false
        }
    }

    /** Retorna o número de fragments na back stack. 0 = menu principal, >0 = em submenu */
    fun getBackStackCount(): Int {
        return fragmentManager.backStackEntryCount
    }
}
