package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
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

    // Flag to indicate when submenu is being closed programmatically (not via back stack)
    private var isClosingSubmenuProgrammatically: Boolean = false

    // Flag to prevent multiple restoration operations
    private var isRestoringSelection: Boolean = false

    // NOVO: Flag para indicar se há um submenu aberto (para controlar restauração)
    private var hasSubmenuOpen: Boolean = false

    // Callbacks para métodos do fragment
    private var showMainMenuCallback: ((Boolean) -> Unit)? = null
    private var setSelectedIndexCallback: ((Int) -> Unit)? = null
    private var getCurrentSelectedIndexCallback: (() -> Int)? = null

    private fun restoreMainMenuSelection() {
        if (!hasSubmenuOpen) {
            Log.d(TAG, "[RESTORE] No submenu was open - skipping restoration")
            return
        }

        if (isRestoringSelection) {
            Log.d(TAG, "[RESTORE] Already restoring selection - skipping")
            return
        }

        isRestoringSelection = true
        hasSubmenuOpen = false

        Log.d(
                TAG,
                "[RESTORE] Restoring main menu selection to index: $mainMenuSelectedIndexBeforeSubmenu"
        )

        // Garantir que os textos do menu principal sejam mostrados
        viewManager.showMainMenuTexts()

        // IMPORTANTE: Restaurar o estado do menu para MAIN_MENU
        menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)

        // RESTAURAR O ÍNDICE SELECIONADO PARA O ITEM QUE ABRIU O SUBMENU
        setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

        // AGUARDAR UM MOMENTO PARA GARANTIR QUE setSelectedIndex FOI PROCESSADO
        fragment.view?.postDelayed(
                {
                    // MOSTRAR O MENU PRINCIPAL NOVAMENTE COM SELEÇÃO PRESERVADA
                    showMainMenuCallback?.invoke(true)

                    // AGUARDAR MAIS UM MOMENTO PARA GARANTIR QUE O MENU FOI MOSTRADO
                    fragment.view?.postDelayed(
                            {
                                // ATUALIZAR A VISUALIZAÇÃO DAS SETAS APÓS RESTAURAR O ESTADO
                                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                                animationController?.updateSelectionVisual(currentIndex)

                                // MARCAR QUE A RESTAURAÇÃO FOI CONCLUÍDA
                                isRestoringSelection = false
                            },
                            50
                    )
                },
                50
        )
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

        // SALVAR O ÍNDICE ATUAL ANTES DE ABRIR O SUBMENU
        val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
        mainMenuSelectedIndexBeforeSubmenu = currentIndex
        hasSubmenuOpen = true
        Log.d(
                TAG,
                "[OPEN_SUBMENU] Saved mainMenuSelectedIndexBeforeSubmenu: $mainMenuSelectedIndexBeforeSubmenu"
        )

        when (submenuType) {
            MenuState.PROGRESS_MENU -> showProgressSubmenu()
            MenuState.SETTINGS_MENU -> showSettingsSubmenu()
            MenuState.CORE_VARIABLES_MENU -> showCoreVariablesSubmenu()
            MenuState.ABOUT_MENU -> showAboutSubmenu()
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
            Log.e(TAG, "[DEBUG] showSettingsSubmenu - Creating SettingsMenuFragment")
            val settingsFragment = SettingsMenuFragment.newInstance()
            settingsFragment.setSettingsListener(
                    fragment as SettingsMenuFragment.SettingsMenuListener
            )

            // Primeiro adicionar o submenu (mas invisível inicialmente)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, settingsFragment, "SettingsMenuFragment")
                    .addToBackStack("SettingsMenuFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showSettingsSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // OCULTAR COMPLETAMENTE O MENU PRINCIPAL APÓS O SUBMENU ESTAR PRONTO
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerSettingsMenuFragment(settingsFragment)

            // Alterar o estado do menu para SETTINGS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)

            Log.d(TAG, "SubmenuCoordinator: Settings submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Settings submenu", e)
        }
    }

    private fun showCoreVariablesSubmenu() {
        Log.d(TAG, "[DEBUG] showCoreVariablesSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showCoreVariablesSubmenu - Creating CoreVariablesFragment")
            val coreVariablesFragment = CoreVariablesFragment.newInstance()
            coreVariablesFragment.setCoreVariablesListener(
                    fragment as CoreVariablesFragment.CoreVariablesListener
            )

            // Primeiro adicionar o submenu (mas invisível inicialmente)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, coreVariablesFragment, "CoreVariablesFragment")
                    .addToBackStack("CoreVariablesFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showCoreVariablesSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // OCULTAR COMPLETAMENTE O MENU PRINCIPAL APÓS O SUBMENU ESTAR PRONTO
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerCoreVariablesFragment(coreVariablesFragment)

            // Alterar o estado do menu para CORE_VARIABLES_MENU
            menuManager.navigateToState(
                    com.vinaooo.revenger.ui.retromenu3.MenuState.CORE_VARIABLES_MENU
            )

            Log.d(TAG, "SubmenuCoordinator: Core Variables submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Core Variables submenu", e)
        }
    }

    private fun showAboutSubmenu() {
        Log.d(TAG, "[DEBUG] showAboutSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showAboutSubmenu - Creating AboutFragment")
            val aboutFragment = AboutFragment.newInstance()
            aboutFragment.setAboutListener(fragment as AboutFragment.AboutListener)

            // Primeiro adicionar o submenu (mas invisível inicialmente)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, aboutFragment, "AboutFragment")
                    .addToBackStack("AboutFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showAboutSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // OCULTAR COMPLETAMENTE O MENU PRINCIPAL APÓS O SUBMENU ESTAR PRONTO
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerAboutFragment(aboutFragment)

            // Alterar o estado do menu para ABOUT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU)

            Log.d(TAG, "SubmenuCoordinator: About submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open About submenu", e)
        }
    }

    private fun showProgressSubmenu() {
        Log.d(TAG, "[DEBUG] showProgressSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showProgressSubmenu - Creating ProgressFragment")
            val progressFragment = ProgressFragment.newInstance()
            progressFragment.setProgressListener(fragment as ProgressFragment.ProgressListener)

            // Primeiro adicionar o submenu (mas invisível inicialmente)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, progressFragment, "ProgressFragment")
                    .addToBackStack("ProgressFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showProgressSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // OCULTAR COMPLETAMENTE O MENU PRINCIPAL APÓS O SUBMENU ESTAR PRONTO
                viewManager.hideMainMenuCompletely()
            }

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
            Log.e(TAG, "[DEBUG] showExitSubmenu - Creating ExitFragment")
            val exitFragment = ExitFragment.newInstance()
            exitFragment.setExitListener(fragment as ExitFragment.ExitListener)

            // Primeiro adicionar o submenu (mas invisível inicialmente)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, exitFragment, "ExitFragment")
                    .addToBackStack("ExitFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showExitSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // OCULTAR COMPLETAMENTE O MENU PRINCIPAL APÓS O SUBMENU ESTAR PRONTO
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerExitFragment(exitFragment)

            // Alterar o estado do menu para EXIT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU)

            Log.d(TAG, "SubmenuCoordinator: Exit submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Exit submenu", e)
        }
    }

    fun closeCurrentSubmenu() {
        // Prevent multiple simultaneous close operations
        if (isClosingSubmenu) {
            return
        }

        isClosingSubmenu = true
        isClosingSubmenuProgrammatically = true

        try {
            // Fazer pop do back stack para fechar o submenu atual
            fragment.parentFragmentManager.popBackStack()

            // USAR O NOVO MÉTODO DE RESTAURAÇÃO APÓS O POPBACKSTACK
            // Isso garante que a restauração aconteça apenas uma vez
            fragment.view?.postDelayed(
                    { restoreMainMenuSelection() },
                    100
            ) // Dar tempo para o back stack ser processado
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to close submenu", e)
        } finally {
            isClosingSubmenu = false
            isClosingSubmenuProgrammatically = false
        }
    }

    fun setupBackStackListener() {
        // Listener para detectar quando submenus são fechados via back stack
        fragment.parentFragmentManager.addOnBackStackChangedListener {
            val backStackCount = fragment.parentFragmentManager.backStackEntryCount

            // Se o back stack ficou vazio, significa que não há mais submenus
            if (backStackCount == 0) {
                // VERIFICAR SE ESTAMOS NO MEIO DE closeCurrentSubmenu() (fechamento programático)
                // Se sim, NÃO executar a lógica de restauração para evitar duplicação
                if (isClosingSubmenuProgrammatically) {
                    return@addOnBackStackChangedListener
                }

                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                // Se sim, NÃO mostrar o menu principal para evitar piscada
                if (viewModel.isDismissingAllMenus()) {
                    return@addOnBackStackChangedListener
                }

                // USAR O NOVO MÉTODO DE RESTAURAÇÃO
                restoreMainMenuSelection()
            }
        }
    }
}
