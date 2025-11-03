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

    // NOVO: Rastrear o count do back stack para detectar mudanças
    private var previousBackStackCount: Int = 0

    init {
        // Inicializar o count do back stack
        previousBackStackCount = fragment.parentFragmentManager.backStackEntryCount

        // CRITICAL: If backstack has entries, a submenu is open
        if (previousBackStackCount > 0) {
            hasSubmenuOpen = true
            Log.d(
                    TAG,
                    "[INIT] Detected backstack ($previousBackStackCount entries) - hasSubmenuOpen=true"
            )
        } else {
            hasSubmenuOpen = false
            Log.d(TAG, "[INIT] No backstack - hasSubmenuOpen=false")
        }
    }

    // Callbacks para métodos do fragment
    private var showMainMenuCallback: ((Boolean) -> Unit)? = null
    private var setSelectedIndexCallback: ((Int) -> Unit)? = null
    private var getCurrentSelectedIndexCallback: (() -> Int)? = null

    private fun restoreMainMenuSelection() {
        android.util.Log.d(
                TAG,
                "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION START =========="
        )
        android.util.Log.d(TAG, "[RESTORE] 📊 hasSubmenuOpen=$hasSubmenuOpen")
        android.util.Log.d(TAG, "[RESTORE] 📊 isRestoringSelection=$isRestoringSelection")

        if (!hasSubmenuOpen) {
            android.util.Log.d(TAG, "[RESTORE] ❌ No submenu was open - skipping restoration")
            android.util.Log.d(
                    TAG,
                    "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END (NO SUBMENU) =========="
            )
            return
        }

        if (isRestoringSelection) {
            android.util.Log.d(TAG, "[RESTORE] ❌ Already restoring selection - skipping")
            android.util.Log.d(
                    TAG,
                    "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END (ALREADY RESTORING) =========="
            )
            return
        }

        isRestoringSelection = true
        hasSubmenuOpen = false

        android.util.Log.d(TAG, "[RESTORE] ✅ Starting restoration process")
        android.util.Log.d(
                TAG,
                "[RESTORE] 📊 mainMenuSelectedIndexBeforeSubmenu=$mainMenuSelectedIndexBeforeSubmenu"
        )

        // Garantir que os textos do menu principal sejam mostrados
        android.util.Log.d(TAG, "[RESTORE] 📝 Calling viewManager.showMainMenuTexts()")
        viewManager.showMainMenuTexts()

        // IMPORTANTE: Determinar o estado correto para restaurar baseado no estado atual
        val currentState = menuManager.getCurrentState()
        android.util.Log.d(TAG, "[RESTORE] 🔍 Checking current state before determining target...")

        val targetState =
                when (currentState) {
                    MenuState.CORE_VARIABLES_MENU -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state CORE_VARIABLES_MENU -> Target ABOUT_MENU"
                        )
                        MenuState.ABOUT_MENU // Voltar do Core Variables para About
                    }
                    MenuState.SETTINGS_MENU -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state SETTINGS_MENU -> Target MAIN_MENU"
                        )
                        // CRITICAL: Unregister SettingsMenuFragment to prevent re-activation
                        android.util.Log.d(TAG, "[RESTORE] 🧹 Unregistering SettingsMenuFragment")
                        viewModel.unregisterSettingsMenuFragment()
                        MenuState.MAIN_MENU // Voltar do Settings para Main
                    }
                    MenuState.ABOUT_MENU -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state ABOUT_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Voltar do About para Main
                    }
                    MenuState.PROGRESS_MENU -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state PROGRESS_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Voltar do Progress para Main
                    }
                    MenuState.EXIT_MENU -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state EXIT_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Voltar do Exit para Main
                    }
                    else -> {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state $currentState -> Target MAIN_MENU (fallback)"
                        )
                        MenuState.MAIN_MENU // Fallback para Main
                    }
                }

        android.util.Log.d(
                TAG,
                "[RESTORE] 🔄 Current state: $currentState, Target state: $targetState"
        )

        // Restaurar o estado do menu para o estado pai apropriado
        android.util.Log.d(TAG, "[RESTORE] 🧭 Calling menuManager.navigateToState($targetState)")
        menuManager.navigateToState(targetState)

        android.util.Log.d(
                TAG,
                "[RESTORE] 🎯 Calling setSelectedIndexCallback($mainMenuSelectedIndexBeforeSubmenu)"
        )
        setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

        // MARCAR QUE A RESTAURAÇÃO PRINCIPAL FOI CONCLUÍDA (antes dos postDelayeds)
        // Isso permite que operações subsequentes funcionem mesmo se os delays ainda não executaram
        android.util.Log.d(TAG, "[RESTORE] ✅ Main restoration operations completed")
        isRestoringSelection = false

        // AGUARDAR UM MOMENTO PARA GARANTIR QUE setSelectedIndex FOI PROCESSADO
        fragment.view?.postDelayed(
                {
                    android.util.Log.d(
                            TAG,
                            "[RESTORE] ⏱️ First postDelayed executed - checking if should show main menu"
                    )

                    // MOSTRAR O MENU PRINCIPAL NOVAMENTE COM SELEÇÃO PRESERVADA
                    // APENAS se estamos voltando para o MAIN_MENU, não para submenus
                    if (targetState == MenuState.MAIN_MENU) {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 📺 Calling showMainMenuCallback(true) - RETURNING TO MAIN MENU"
                        )
                        showMainMenuCallback?.invoke(true)
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 📺 showMainMenuCallback invoked successfully"
                        )
                    } else {
                        android.util.Log.d(
                                TAG,
                                "[RESTORE] 🚫 Skipping showMainMenuCallback (targetState=$targetState != MAIN_MENU)"
                        )
                    }

                    // AGUARDAR MAIS UM MOMENTO PARA GARANTIR QUE O MENU FOI MOSTRADO
                    fragment.view?.postDelayed(
                            {
                                android.util.Log.d(
                                        TAG,
                                        "[RESTORE] ⏱️ Second postDelayed executed - updating selection visual"
                                )

                                // ATUALIZAR A VISUALIZAÇÃO DAS SETAS APÓS RESTAURAR O ESTADO
                                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                                android.util.Log.d(
                                        TAG,
                                        "[RESTORE] 🎨 Updating selection visual for index: $currentIndex"
                                )
                                animationController?.updateSelectionVisual(currentIndex)

                                // MARCAR QUE A RESTAURAÇÃO VISUAL FOI CONCLUÍDA
                                android.util.Log.d(TAG, "[RESTORE] ✅ Visual restoration completed")
                                android.util.Log.d(
                                        TAG,
                                        "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END =========="
                                )
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
        android.util.Log.d(
                TAG,
                "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU START =========="
        )

        // Prevent multiple simultaneous close operations
        if (isClosingSubmenu) {
            android.util.Log.d(TAG, "[CLOSE_SUBMENU] ❌ Already closing submenu, skipping")
            android.util.Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU END (ALREADY CLOSING) =========="
            )
            return
        }

        isClosingSubmenu = true
        isClosingSubmenuProgrammatically = true

        android.util.Log.d(TAG, "[CLOSE_SUBMENU] ✅ Starting close operation")

        try {
            // Fazer pop do back stack para fechar o submenu atual
            android.util.Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 📚 Calling parentFragmentManager.popBackStack()"
            )
            fragment.parentFragmentManager.popBackStack()

            // A restauração será feita pelo back stack listener
            android.util.Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 📋 Restoration will be handled by back stack listener"
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[CLOSE_SUBMENU] ❌ Error closing submenu", e)
        } finally {
            isClosingSubmenu = false
            isClosingSubmenuProgrammatically = false
            android.util.Log.d(TAG, "[CLOSE_SUBMENU] 🔄 Close operation flags reset")
            android.util.Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU END =========="
            )
        }
    }

    fun setupBackStackListener() {
        // Listener para detectar quando submenus são fechados via back stack
        fragment.parentFragmentManager.addOnBackStackChangedListener {
            // SAFETY CHECK: Verificar se fragment ainda está associado a um FragmentManager
            if (!fragment.isAdded || fragment.activity == null) {
                android.util.Log.d(
                        TAG,
                        "[BACK_STACK] ⚠️ Fragment not added or activity null - skipping listener"
                )
                return@addOnBackStackChangedListener
            }

            val backStackCount = fragment.parentFragmentManager.backStackEntryCount
            val backStackDecreased = backStackCount < previousBackStackCount

            android.util.Log.d(
                    TAG,
                    "[BACK_STACK] 📚 Back stack changed: previous=$previousBackStackCount, current=$backStackCount, decreased=$backStackDecreased"
            )

            // Se o back stack diminuiu (submenu foi fechado), executar restauração
            if (backStackDecreased && hasSubmenuOpen) {
                // VERIFICAR SE ESTAMOS NO MEIO DE closeCurrentSubmenu() (fechamento programático)
                // Se sim, NÃO executar a lógica de restauração para evitar duplicação
                if (isClosingSubmenuProgrammatically) {
                    android.util.Log.d(
                            TAG,
                            "[BACK_STACK] 🚫 Skipping restoration - isClosingSubmenuProgrammatically=true"
                    )
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                // Se sim, NÃO mostrar o menu principal para evitar piscada
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            TAG,
                            "[BACK_STACK] 🚫 Skipping restoration - isDismissingAllMenus=true"
                    )
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                android.util.Log.d(
                        TAG,
                        "[BACK_STACK] ✅ Back stack decreased and submenu was open - calling restoreMainMenuSelection()"
                )
                // USAR O NOVO MÉTODO DE RESTAURAÇÃO
                restoreMainMenuSelection()
            }

            // Se o back stack ficou vazio (caso especial), executar restauração
            else if (backStackCount == 0) {
                // VERIFICAR SE ESTAMOS NO MEIO DE closeCurrentSubmenu() (fechamento programático)
                // Se sim, NÃO executar a lógica de restauração para evitar duplicação
                if (isClosingSubmenuProgrammatically) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                // Se sim, NÃO mostrar o menu principal para evitar piscada
                if (viewModel.isDismissingAllMenus()) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                android.util.Log.d(
                        TAG,
                        "[BACK_STACK] ✅ Back stack empty - calling restoreMainMenuSelection()"
                )
                // USAR O NOVO MÉTODO DE RESTAURAÇÃO
                restoreMainMenuSelection()
            }

            previousBackStackCount = backStackCount
        }
    }
}
