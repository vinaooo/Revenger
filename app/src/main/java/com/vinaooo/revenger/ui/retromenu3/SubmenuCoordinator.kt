package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

class SubmenuCoordinator(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager,
        private val menuManager: com.vinaooo.revenger.ui.retromenu3.MenuManager
) {

    companion object {
        private const val TAG = "RetroMenu3"
    }

    init {
        Log.d(TAG, "[INIT] SubmenuCoordinator created and ready!")
    }

    fun testMethodExecution(testType: String) {
        // DIMINUIR OPACIDADE DO MENU PRINCIPAL
        viewManager.dimMainMenu()
        Log.d(TAG, "SubmenuCoordinator: testMethodExecution - Main menu dimmed for $testType")
    }

    fun openSubmenu(submenuType: MenuState) {
        Log.d(TAG, "🚪 Calling SubmenuCoordinator.openSubmenu($submenuType)")

        when (submenuType) {
            MenuState.PROGRESS_MENU -> showProgressSubmenu()
            MenuState.SETTINGS_MENU -> showSettingsSubmenu()
            MenuState.EXIT_MENU -> showExitSubmenu()
            MenuState.MAIN_MENU -> {
                Log.w(TAG, "openSubmenu called with MAIN_MENU - this should not happen")
            }
        }

        Log.d(TAG, "✅ openSubmenu called successfully")
    }

    private fun showSettingsSubmenu() {
        Log.d(TAG, "[DEBUG] showSettingsSubmenu START")
        try {
            Log.d(TAG, "[DEBUG] showSettingsSubmenu - Calling dimMainMenu and hideMainMenuTexts")
            // DIMINUIR OPACIDADE DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.dimMainMenu()

            // OCULTAR TEXTOS DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU (SOMENTE SE INICIALIZADO)
            if (viewManager.isViewsInitialized()) {
                viewManager.hideMainMenuTexts()
                Log.d(TAG, "[DEBUG] showSettingsSubmenu - hideMainMenuTexts called successfully")
            } else {
                Log.d(
                        TAG,
                        "[DEBUG] showSettingsSubmenu - MenuViewManager not initialized, skipping hideMainMenuTexts"
                )
            }

            Log.e(TAG, "[DEBUG] showSettingsSubmenu - Creating SettingsMenuFragment")
            val settingsFragment = SettingsMenuFragment.newInstance()
            settingsFragment.setSettingsListener(
                    fragment as SettingsMenuFragment.SettingsMenuListener
            )

            fragment.parentFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, settingsFragment, "SettingsMenuFragment")
                    .addToBackStack("SettingsMenuFragment")
                    .commitAllowingStateLoss()

            // Registrar o fragment no ViewModel
            viewModel.registerSettingsMenuFragment(settingsFragment)

            // Alterar o estado do menu para SETTINGS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)

            Log.d(TAG, "SubmenuCoordinator: Settings submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Settings submenu", e)
        }
    }

    private fun showProgressSubmenu() {
        Log.d(TAG, "[DEBUG] showProgressSubmenu - START")
        try {
            Log.d(TAG, "[DEBUG] showProgressSubmenu - Calling dimMainMenu and hideMainMenuTexts")
            // DIMINUIR OPACIDADE DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.dimMainMenu()

            // OCULTAR TEXTOS DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU (SOMENTE SE INICIALIZADO)
            if (viewManager.isViewsInitialized()) {
                viewManager.hideMainMenuTexts()
                Log.d(TAG, "[DEBUG] showProgressSubmenu - hideMainMenuTexts called successfully")
            } else {
                Log.d(
                        TAG,
                        "[DEBUG] showProgressSubmenu - MenuViewManager not initialized, skipping hideMainMenuTexts"
                )
            }

            Log.e(TAG, "[DEBUG] showProgressSubmenu - Creating ProgressFragment")
            val progressFragment = ProgressFragment.newInstance()
            progressFragment.setProgressListener(fragment as ProgressFragment.ProgressListener)

            fragment.parentFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, progressFragment, "ProgressFragment")
                    .addToBackStack("ProgressFragment")
                    .commitAllowingStateLoss()

            // Registrar o fragment no ViewModel
            viewModel.registerProgressFragment(progressFragment)

            // Alterar o estado do menu para PROGRESS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU)

            Log.d(TAG, "SubmenuCoordinator: Progress submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Progress submenu", e)
        }
    }

    private fun showExitSubmenu() {
        Log.d(TAG, "[DEBUG] showExitSubmenu - START")
        try {
            Log.d(TAG, "[DEBUG] showExitSubmenu - Calling dimMainMenu and hideMainMenuTexts")
            // DIMINUIR OPACIDADE DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.dimMainMenu()

            // OCULTAR TEXTOS DO MENU PRINCIPAL ANTES DE ABRIR SUBMENU (SOMENTE SE INICIALIZADO)
            if (viewManager.isViewsInitialized()) {
                viewManager.hideMainMenuTexts()
                Log.d(TAG, "[DEBUG] showExitSubmenu - hideMainMenuTexts called successfully")
            } else {
                Log.d(
                        TAG,
                        "[DEBUG] showExitSubmenu - MenuViewManager not initialized, skipping hideMainMenuTexts"
                )
            }

            Log.e(TAG, "[DEBUG] showExitSubmenu - Creating ExitFragment")
            val exitFragment = ExitFragment.newInstance()
            exitFragment.setExitListener(fragment as ExitFragment.ExitListener)

            fragment.parentFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, exitFragment, "ExitFragment")
                    .addToBackStack("ExitFragment")
                    .commitAllowingStateLoss()

            // Registrar o fragment no ViewModel
            viewModel.registerExitFragment(exitFragment)

            // Alterar o estado do menu para EXIT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU)

            Log.d(TAG, "SubmenuCoordinator: Exit submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Exit submenu", e)
        }
    }
}
