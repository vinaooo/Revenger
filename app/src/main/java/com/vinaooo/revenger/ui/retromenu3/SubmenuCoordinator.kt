package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Classe especializada para coordenar abertura e fechamento de submenus. Gerencia o ciclo de vida
 * dos submenus, listeners de back stack e transição entre menu principal e submenus.
 */
class SubmenuCoordinator(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager
) {

    companion object {
        private const val TAG = "SubmenuCoordinator"
    }

    // Listener para detectar quando submenus são fechados
    private var backStackChangeListener: FragmentManager.OnBackStackChangedListener? = null

    /** Configura listener global para detectar quando submenus são fechados */
    private fun setupGlobalBackStackListener() {
        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let {
            fragment.parentFragmentManager.removeOnBackStackChangedListener(it)
        }

        // Create new listener for submenu session
        backStackChangeListener =
                FragmentManager.OnBackStackChangedListener {
                    val backStackCount = fragment.parentFragmentManager.backStackEntryCount
                    val isDismissing = viewModel.isDismissingAllMenus()
                    val hasActiveSubmenus =
                            viewModel.isSettingsMenuOpen() ||
                                    viewModel.isProgressMenuOpen() ||
                                    viewModel.isExitMenuOpen()

                    Log.d(
                            TAG,
                            "[SUBMENU] 🔍 BACK STACK LISTENER: count=$backStackCount, isDismissing=$isDismissing, hasActive=$hasActiveSubmenus, isRetroMenu3Open=${viewModel.isRetroMenu3Open()}"
                    )

                    // If the back stack is empty, it means the submenu was removed
                    if (backStackCount == 0) {
                        Log.d(TAG, "[SUBMENU] 📭 Back stack is empty - submenu was removed")

                        // CRITICAL FIX: During cascade dismissal (START button), never restore main
                        // menu
                        // The dismissAllMenus() method will handle everything in the correct order
                        if (isDismissing || !viewModel.isRetroMenu3Open()) {
                            Log.d(
                                    TAG,
                                    "[SUBMENU] 🚫 Cascade dismissal in progress or main menu closed - NOT restoring main menu"
                            )
                            return@OnBackStackChangedListener
                        }

                        // Only restore main menu for normal navigation (back button, individual
                        // submenu close)
                        // In normal navigation, submenus close one at a time, so hasActiveSubmenus
                        // should be false
                        // when the last submenu is closed
                        if (!hasActiveSubmenus) {
                            Log.d(TAG, "[SUBMENU] ✅ Normal navigation - restoring main menu")
                            restoreMainMenu()
                        } else {
                            Log.d(
                                    TAG,
                                    "[SUBMENU] ⏳ Other submenus still active - NOT restoring main menu"
                            )
                        }

                        // Always remove the listener after use
                        backStackChangeListener?.let { listener ->
                            Log.d(TAG, "[SUBMENU] 🗑️ Removing back stack listener")
                            fragment.parentFragmentManager.removeOnBackStackChangedListener(
                                    listener
                            )
                            backStackChangeListener = null
                        }
                    } else {
                        Log.d(TAG, "[SUBMENU] 📚 Back stack not empty, ignoring change")
                    }
                }

        // Add the listener to detect when back stack changes (submenu is removed)
        fragment.parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener!!)
    }

    /** Abre um submenu específico */
    fun openSubmenu(menuState: MenuState) {
        Log.d(TAG, "[SUBMENU] Opening submenu: $menuState")

        // Setup global back stack listener if not already set
        setupGlobalBackStackListener()

        when (menuState) {
            MenuState.PROGRESS_MENU -> openProgressSubmenu()
            MenuState.SETTINGS_MENU -> openSettingsSubmenu()
            MenuState.EXIT_MENU -> openExitSubmenu()
            else -> Log.w(TAG, "[SUBMENU] Unknown submenu state: $menuState")
        }
    }

    /** Substitui o conteúdo do menu principal pelo submenu */
    private fun replaceMainMenuWithSubmenu(
            submenuFragment: androidx.fragment.app.Fragment,
            tag: String,
            menuState: MenuState
    ) {
        Log.d(TAG, "[SUBMENU] Replacing main menu with submenu: $tag")

        // CRITICAL FIX: Instead of using replace() which causes visibility issues,
        // let's manage fragments manually to avoid FragmentManager state restoration glitches

        // Hide main menu first
        (fragment as? RetroMenu3Fragment)?.hideMainMenu()
        Log.d(TAG, "[SUBMENU] Main menu hidden")

        // Add submenu on top without replacing (to avoid back stack visibility issues)
        val containerId = R.id.menu_container
        fragment.parentFragmentManager
                .beginTransaction()
                .add(containerId, submenuFragment, tag)
                .addToBackStack(tag)
                .commit()

        // Update MenuManager state
        viewModel.updateMenuState(menuState)
        Log.d(TAG, "[SUBMENU] Submenu $tag opened successfully (manual management)")
    }

    /** Restaura o menu principal */
    private fun restoreMainMenu() {
        // CRITICAL: Never restore main menu if we're in the middle of dismissing all menus
        // Check if the main RetroMenu3 is still supposed to be open
        if (viewModel.isDismissingAllMenus() || !viewModel.isRetroMenu3Open()) {
            Log.d(
                    TAG,
                    "[SUBMENU] 🚫 restoreMainMenu: BLOCKED - isDismissing=${viewModel.isDismissingAllMenus()}, isRetroMenu3Open=${viewModel.isRetroMenu3Open()}"
            )
            return
        }

        Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Starting restoration")

        // With manual fragment management (.add() instead of .replace()),
        // the main menu is still in the container, just hidden
        val fragmentManager = fragment.parentFragmentManager

        if (fragmentManager.backStackEntryCount > 0) {
            Log.d(
                    TAG,
                    "[SUBMENU] 🔄 restoreMainMenu: Popping back stack (count=${fragmentManager.backStackEntryCount})"
            )
            fragmentManager.popBackStackImmediate()
            // Main menu is already in container, just need to show it
            (fragment as? RetroMenu3Fragment)?.showMainMenu()
        } else {
            Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Back stack empty, showing main menu")
            // If back stack is empty, just show the main menu
            (fragment as? RetroMenu3Fragment)?.showMainMenu()
        }

        // Update MenuManager state
        viewModel.updateMenuState(MenuState.MAIN_MENU)
        Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Main menu restored successfully")
    }

    /** Abre submenu de progresso */
    private fun openProgressSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening progress submenu")

        // Create and show ProgressFragment
        val progressFragment = ProgressFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerProgressFragment(progressFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(progressFragment, "ProgressFragment", MenuState.PROGRESS_MENU)
    }

    /** Abre submenu de configurações */
    private fun openSettingsSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening settings submenu")

        // Create and show SettingsMenuFragment with visual identical to RetroMenu3
        val settingsFragment =
                SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

        // Register the fragment in ViewModel so navigation works
        viewModel.registerSettingsMenuFragment(settingsFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(
                settingsFragment,
                "SettingsMenuFragment",
                MenuState.SETTINGS_MENU
        )
    }

    /** Abre submenu de saída */
    private fun openExitSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening exit submenu")

        // Create and show ExitFragment
        val exitFragment = ExitFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerExitFragment(exitFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(exitFragment, "ExitFragment", MenuState.EXIT_MENU)
    }

    /** Cleanup quando o fragment é destruído */
    fun onDestroy() {
        // Clean up back stack change listener to prevent memory leaks
        backStackChangeListener?.let { listener ->
            fragment.parentFragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangeListener = null
        }
    }
}
