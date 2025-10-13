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

    /** Abre um submenu específico */
    fun openSubmenu(menuState: MenuState) {
        Log.d(TAG, "[SUBMENU] Opening submenu: $menuState")

        when (menuState) {
            MenuState.PROGRESS_MENU -> openProgressSubmenu()
            MenuState.SETTINGS_MENU -> openSettingsSubmenu()
            MenuState.EXIT_MENU -> openExitSubmenu()
            else -> Log.w(TAG, "[SUBMENU] Unknown submenu state: $menuState")
        }
    }

    /** Fecha o submenu atual e volta ao menu principal */
    fun closeCurrentSubmenu() {
        Log.d(TAG, "[SUBMENU] Closing current submenu")

        // Remove o listener de back stack
        backStackChangeListener?.let { listener ->
            fragment.parentFragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangeListener = null
        }

        // Pop do back stack para fechar o submenu
        if (fragment.parentFragmentManager.backStackEntryCount > 0) {
            fragment.parentFragmentManager.popBackStack()
        }

        // Mostrar menu principal novamente
        viewManager.showMainMenu()

        // Reset MenuManager state to MAIN_MENU
        viewModel.updateMenuState(MenuState.MAIN_MENU)
    }

    /** Configura listener para detectar quando submenus são fechados */
    private fun setupBackStackListener(onSubmenuClosed: () -> Unit) {
        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let {
            fragment.parentFragmentManager.removeOnBackStackChangedListener(it)
        }

        // Create new listener for this submenu session
        backStackChangeListener =
                FragmentManager.OnBackStackChangedListener {
                    // If the back stack is empty, it means the submenu was removed
                    if (fragment.parentFragmentManager.backStackEntryCount == 0) {
                        // Only show main menu if we're not dismissing all menus at once
                        if (!viewModel.isDismissingAllMenus()) {
                            Log.d(TAG, "[SUBMENU] Submenu closed, executing callback")
                            onSubmenuClosed()
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

    /** Abre submenu de progresso */
    private fun openProgressSubmenu() {
        // Make main menu invisible before opening submenu
        viewManager.hideMainMenu()

        // Setup back stack listener
        setupBackStackListener {
            Log.d(TAG, "[SUBMENU] Progress submenu closed, showing main menu")
            viewManager.showMainMenu()
            viewModel.updateMenuState(MenuState.MAIN_MENU)
        }

        // Create and show ProgressFragment
        val progressFragment = ProgressFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerProgressFragment(progressFragment)

        // Use the same container as the parent fragment (menu_container)
        val containerId = (fragment.view?.parent as? android.view.View)?.id ?: R.id.menu_container

        fragment.parentFragmentManager
                .beginTransaction()
                .add(containerId, progressFragment, "ProgressFragment")
                .addToBackStack("ProgressFragment")
                .commit()

        // Ensure that the transaction is executed immediately
        fragment.parentFragmentManager.executePendingTransactions()

        // Update MenuManager state to PROGRESS_MENU
        viewModel.updateMenuState(MenuState.PROGRESS_MENU)
        Log.d(TAG, "[SUBMENU] Progress submenu opened successfully")
    }

    /** Abre submenu de configurações */
    private fun openSettingsSubmenu() {
        // Make main menu invisible before opening submenu
        viewManager.hideMainMenu()

        // Setup back stack listener
        setupBackStackListener {
            Log.d(TAG, "[SUBMENU] Settings submenu closed, showing main menu")
            viewManager.showMainMenu()
            viewModel.updateMenuState(MenuState.MAIN_MENU)
        }

        // Create and show SettingsMenuFragment with visual identical to RetroMenu3
        val settingsFragment =
                SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

        // Register the fragment in ViewModel so navigation works
        viewModel.registerSettingsMenuFragment(settingsFragment)

        // Use the same container as the parent fragment (menu_container)
        val containerId = (fragment.view?.parent as? android.view.View)?.id ?: R.id.menu_container

        fragment.parentFragmentManager
                .beginTransaction()
                .add(containerId, settingsFragment, "SettingsMenuFragment")
                .addToBackStack("SettingsMenuFragment")
                .commit()

        // Ensure that the transaction is executed immediately
        fragment.parentFragmentManager.executePendingTransactions()

        // Update MenuManager state to SETTINGS_MENU
        viewModel.updateMenuState(MenuState.SETTINGS_MENU)
        Log.d(TAG, "[SUBMENU] Settings submenu opened successfully")
    }

    /** Abre submenu de saída */
    private fun openExitSubmenu() {
        // Make main menu invisible before opening submenu
        viewManager.hideMainMenu()

        // Setup back stack listener
        setupBackStackListener {
            Log.d(TAG, "[SUBMENU] Exit submenu closed, showing main menu")
            viewManager.showMainMenu()
            viewModel.updateMenuState(MenuState.MAIN_MENU)
        }

        // Create and show ExitFragment
        val exitFragment = ExitFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerExitFragment(exitFragment)

        // Use the same container as the parent fragment (menu_container)
        val containerId = (fragment.view?.parent as? android.view.View)?.id ?: R.id.menu_container

        fragment.parentFragmentManager
                .beginTransaction()
                .add(containerId, exitFragment, "ExitFragment")
                .addToBackStack("ExitFragment")
                .commit()

        // Ensure that the transaction is executed immediately
        fragment.parentFragmentManager.executePendingTransactions()

        // Update MenuManager state to EXIT_MENU
        viewModel.updateMenuState(MenuState.EXIT_MENU)
        Log.d(TAG, "[SUBMENU] Exit submenu opened successfully")
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
