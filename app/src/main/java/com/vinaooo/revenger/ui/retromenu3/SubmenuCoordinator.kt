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
                    // If the back stack is empty, it means the submenu was removed
                    if (fragment.parentFragmentManager.backStackEntryCount == 0) {
                        // Only show main menu if we're not dismissing all menus at once
                        if (!viewModel.isDismissingAllMenus()) {
                            Log.d(TAG, "[SUBMENU] Submenu closed, restoring main menu")
                            restoreMainMenu()
                        }

                        // Remove the listener after use
                        backStackChangeListener?.let { listener ->
                            fragment.parentFragmentManager.removeOnBackStackChangedListener(
                                    listener
                            )
                            backStackChangeListener = null
                        }
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

        // Use the menu container for the submenu (same as main menu)
        val containerId = R.id.menu_container

        fragment.parentFragmentManager
                .beginTransaction()
                .replace(containerId, submenuFragment, tag)
                .addToBackStack(tag)
                .commit()

        // Update MenuManager state
        viewModel.updateMenuState(menuState)
        Log.d(TAG, "[SUBMENU] Submenu $tag opened successfully")
    }

    /** Restaura o menu principal */
    private fun restoreMainMenu() {
        Log.d(TAG, "[SUBMENU] Restoring main menu")

        // Instead of creating a new instance, use popBackStack to restore the original main menu
        // The original RetroMenu3Fragment should still be in the back stack
        val fragmentManager = fragment.parentFragmentManager

        if (fragmentManager.backStackEntryCount > 0) {
            Log.d(TAG, "[SUBMENU] Popping back stack to restore main menu")
            fragmentManager.popBackStackImmediate()
        } else {
            Log.w(TAG, "[SUBMENU] No back stack entries found, cannot restore main menu")
        }

        // Update MenuManager state
        viewModel.updateMenuState(MenuState.MAIN_MENU)
        Log.d(TAG, "[SUBMENU] Main menu restored")
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
