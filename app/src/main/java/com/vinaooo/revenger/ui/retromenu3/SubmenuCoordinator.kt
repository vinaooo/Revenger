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
        private const val TAG = "SubmenuCoordinator"
    }

    fun testMethodExecution(testType: String) {
        // DIMINUIR OPACIDADE DO MENU PRINCIPAL
        viewManager.dimMainMenu()
        Log.d(TAG, "SubmenuCoordinator: testMethodExecution - Main menu dimmed for $testType")
    }

    fun openSubmenu(submenuType: MenuState) {
        // DIMINUIR OPACIDADE DO MENU PRINCIPAL
        viewManager.dimMainMenu()
        Log.d(TAG, "SubmenuCoordinator: openSubmenu - Main menu dimmed for $submenuType")

        // CRIAR E EXIBIR O FRAGMENT DO SUBMENU
        when (submenuType) {
            MenuState.SETTINGS_MENU -> showSettingsSubmenu()
            MenuState.PROGRESS_MENU -> showProgressSubmenu()
            MenuState.EXIT_MENU -> showExitSubmenu()
            else -> Log.w(TAG, "SubmenuCoordinator: Unknown submenu type: $submenuType")
        }
    }

    private fun showSettingsSubmenu() {
        try {
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

            // Ocultar textos do menu principal
            viewManager.hideMainMenuTexts()

            Log.d(TAG, "SubmenuCoordinator: Settings submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Settings submenu", e)
        }
    }

    private fun showProgressSubmenu() {
        try {
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

            // Ocultar textos do menu principal
            viewManager.hideMainMenuTexts()

            Log.d(TAG, "SubmenuCoordinator: Progress submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Progress submenu", e)
        }
    }

    private fun showExitSubmenu() {
        try {
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

            // Ocultar textos do menu principal
            viewManager.hideMainMenuTexts()

            Log.d(TAG, "SubmenuCoordinator: Exit submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Exit submenu", e)
        }
    }
}
