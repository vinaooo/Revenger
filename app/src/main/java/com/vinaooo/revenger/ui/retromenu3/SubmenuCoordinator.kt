package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

class SubmenuCoordinator(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager,
        private val menuManager: com.vinaooo.revenger.ui.retromenu3.MenuManager,
        private val animationController: MenuAnimationController? = null
) {

    companion object {
        private const val TAG = "RetroMenu3"
    }

    // Store the main menu selected index before opening a submenu
    private var mainMenuSelectedIndexBeforeSubmenu: Int = 0

    // Flag to indicate if selection should be preserved when showing main menu
    private var shouldPreserveSelectionOnShowMainMenu: Boolean = false

    // Flag to prevent multiple simultaneous close operations
    private var isClosingSubmenu: Boolean = false

    // Callbacks para métodos do fragment
    private var showMainMenuCallback: ((Boolean) -> Unit)? = null
    private var setSelectedIndexCallback: ((Int) -> Unit)? = null
    private var getCurrentSelectedIndexCallback: (() -> Int)? = null

    fun setMainMenuSelectedIndexBeforeSubmenu(index: Int) {
        mainMenuSelectedIndexBeforeSubmenu = index
    }

    fun setCallbacks(
            showMainMenuCallback: (Boolean) -> Unit,
            setSelectedIndexCallback: (Int) -> Unit,
            getCurrentSelectedIndexCallback: () -> Int
    ) {
        this.showMainMenuCallback = showMainMenuCallback
        this.setSelectedIndexCallback = setSelectedIndexCallback
        this.getCurrentSelectedIndexCallback = getCurrentSelectedIndexCallback
    }

    init {
        Log.d(TAG, "[INIT] SubmenuCoordinator created and ready!")
    }

    fun testMethodExecution(testType: String) {
        // OCULTAR COMPLETAMENTE O MENU PRINCIPAL
        viewManager.hideMainMenu()
        Log.d(TAG, "SubmenuCoordinator: testMethodExecution - Main menu hidden for $testType")
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
            Log.d(TAG, "[DEBUG] showSettingsSubmenu - Calling hideMainMenu and hideMainMenuTexts")
            // OCULTAR COMPLETAMENTE O MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.hideMainMenu()

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
        Log.d(TAG, "[DEBUG] showProgressSubmenu START")
        try {
            Log.d(TAG, "[DEBUG] showProgressSubmenu - Calling hideMainMenu and hideMainMenuTexts")
            // OCULTAR COMPLETAMENTE O MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.hideMainMenu()

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
        Log.d(TAG, "[DEBUG] showExitSubmenu START")
        try {
            Log.d(TAG, "[DEBUG] showExitSubmenu - Calling hideMainMenu and hideMainMenuTexts")
            // OCULTAR COMPLETAMENTE O MENU PRINCIPAL ANTES DE ABRIR SUBMENU
            viewManager.hideMainMenu()

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

    fun setupBackStackListener() {
        // Listener para detectar quando submenus são fechados via back stack
        fragment.parentFragmentManager.addOnBackStackChangedListener {
            val backStackCount = fragment.parentFragmentManager.backStackEntryCount
            MenuLogger.d("[BACK_STACK] Back stack changed - count: $backStackCount")

            // Se o back stack ficou vazio, significa que não há mais submenus
            if (backStackCount == 0) {
                MenuLogger.d(
                        "[BACK_STACK] 🔥🔥🔥 BACK STACK LISTENER FIRED - EXECUTING RESTORATION LOGIC 🔥🔥🔥"
                )

                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                // Se sim, NÃO mostrar o menu principal para evitar piscada
                if (viewModel.isDismissingAllMenus()) {
                    MenuLogger.d(
                            "[BACK_STACK] ⚠️ isDismissingAllMenus = true, SKIPPING main menu restoration to avoid flicker"
                    )
                    return@addOnBackStackChangedListener
                }

                // Garantir que os textos do menu principal sejam mostrados
                viewManager.showMainMenuTexts()

                // MOSTRAR O MENU PRINCIPAL NOVAMENTE COM SELEÇÃO PRESERVADA
                showMainMenuCallback?.invoke(true)

                // IMPORTANTE: Restaurar o estado do menu para MAIN_MENU
                menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)

                // RESTAURAR O ÍNDICE SELECIONADO PARA O ITEM QUE ABRIU O SUBMENU
                MenuLogger.d(
                        "[BACK_STACK] Back stack empty - stored main menu index: $mainMenuSelectedIndexBeforeSubmenu"
                )
                MenuLogger.d(
                        "[BACK_STACK] Back stack empty - restoring selected index to $mainMenuSelectedIndexBeforeSubmenu (submenu entry point)"
                )
                setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

                // LOG PARA DEBUG: Verificar o índice selecionado atual
                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                MenuLogger.d(
                        "[BACK_STACK] Back stack empty - current selected index after restore: $currentIndex"
                )

                // ATUALIZAR A VISUALIZAÇÃO DAS SETAS APÓS RESTAURAR O ESTADO
                MenuLogger.d("[BACK_STACK] Back stack empty - calling updateSelectionVisual()")
                animationController?.updateSelectionVisual(currentIndex)
                MenuLogger.d("[BACK_STACK] Back stack empty - updateSelectionVisual() completed")
            }
        }
    }

    fun closeCurrentSubmenu() {
        // Prevent multiple simultaneous close operations
        if (isClosingSubmenu) {
            MenuLogger.d(
                    "[BACK] SubmenuCoordinator.closeCurrentSubmenu() already in progress, ignoring"
            )
            return
        }

        isClosingSubmenu = true

        try {
            MenuLogger.d("[BACK] SubmenuCoordinator.closeCurrentSubmenu() started")
            // Mostrar novamente os textos do menu principal
            viewManager.showMainMenuTexts()

            // Restaurar menu principal
            viewManager.restoreMainMenu()

            // IMPORTANTE: Restaurar o estado do menu para MAIN_MENU antes de fechar o submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)

            // Fazer pop do back stack para fechar o submenu atual
            MenuLogger.d("[BACK] SubmenuCoordinator.closeCurrentSubmenu() calling popBackStack()")
            fragment.parentFragmentManager.popBackStack()
            MenuLogger.d("[BACK] SubmenuCoordinator.closeCurrentSubmenu() popBackStack() completed")

            MenuLogger.d("[BACK] SubmenuCoordinator.closeCurrentSubmenu() completed successfully")
        } catch (e: Exception) {
            MenuLogger.d("[BACK] SubmenuCoordinator.closeCurrentSubmenu() failed: ${e.message}")
            Log.e(TAG, "SubmenuCoordinator: Failed to close submenu", e)
        } finally {
            isClosingSubmenu = false
        }
    }
}
