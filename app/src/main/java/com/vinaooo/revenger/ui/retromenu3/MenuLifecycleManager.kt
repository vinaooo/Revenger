package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Interface para gerenciamento do ciclo de vida do RetroMenu3Fragment. Responsável por
 * inicialização, setup e cleanup do fragment.
 */
interface MenuLifecycleManager {
    fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View
    fun onViewCreated(view: View, savedInstanceState: Bundle?)
    fun onResume()
    fun onDestroy()
}

/**
 * Implementação do MenuLifecycleManager. Coordena a inicialização do menu e delega para os outros
 * managers especializados.
 */
class MenuLifecycleManagerImpl(
        private val fragment: RetroMenu3Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewInitializer: MenuViewInitializer,
        private val animationController: MenuAnimationController,
        private val inputHandler: MenuInputHandler,
        private val stateController: MenuStateController,
        private val callbackManager: MenuCallbackManager,
        private val menuViewManager: MenuViewManager,
        private val actionHandler: MenuActionHandler
) : MenuLifecycleManager {
    private lateinit var menuViews: MenuViews

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View {
        MenuLogger.lifecycle("MenuLifecycleManager: onCreateView START")
        return inflater.inflate(R.layout.retro_menu3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - Iniciando configuração")

        try {
            // NOVO: Aplicar proporções de layout configuráveis (10-80-10, 10-70-20, etc)
            applyConfigurableLayoutProportions(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Layout proportions applied")

            // Use the MenuViewManager instance passed from fragment
            MenuLogger.lifecycle("MenuLifecycleManager: MenuViewManager instance received")

            // Force all views to z=0 to stay below gamepad
            com.vinaooo.revenger.utils.ViewUtils.forceZeroElevationRecursively(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Elevation forced to zero")

            // Inicializar views através do viewInitializer
            val menuViews = viewInitializer.initializeViews(view)

            // ARMAZENAR menuViews NO FRAGMENT
            (fragment as? RetroMenu3Fragment)?.menuViews = menuViews
            MenuLogger.lifecycle("MenuLifecycleManager: menuViews stored in fragment")

            // Configurar estados iniciais das views
            viewInitializer.configureInitialViewStates(menuViews)

            // Configurar título dinâmico
            viewInitializer.setupDynamicTitle(menuViews)

            // Setup MenuViewManager views
            menuViewManager.setupViews(view)
            MenuLogger.lifecycle("MenuLifecycleManager: MenuViewManager views setup completed")

            // Configurar animationController com as views
            animationController.setMenuViews(menuViews)

            // Inicializar state controller
            stateController.initializeState(menuViews)

            // Configurar input handler
            inputHandler.setupInputHandling(menuViews)

            // Configurar click listeners
            // PHASE 3.3a: Pass navigationController for touch event routing
            val navigationController = viewModel.navigationController
            viewInitializer.setupClickListeners(menuViews, actionHandler, navigationController)

            // Iniciar animação do menu
            animationController.animateMenuIn()
            MenuLogger.lifecycle("MenuLifecycleManager: Menu animation started")

            // Ensure first item is selected after animation
            fragment.setSelectedIndex(0)
            stateController.updateSelectionVisuals()
            MenuLogger.lifecycle("MenuLifecycleManager: Selection visual updated")

            MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - Configuração concluída")
        } catch (e: Exception) {
            MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - ERROR: ${e.message}")
            throw e
        }
    }

    override fun onResume() {
        MenuLogger.lifecycle("MenuLifecycleManager: onResume START")

        MenuLogger.lifecycle("MenuLifecycleManager: onResume COMPLETED")
    }

    override fun onDestroy() {
        MenuLogger.lifecycle("MenuLifecycleManager: onDestroy START")

        // Notify ViewModel that fragment is being destroyed
        try {
            val activity = fragment.requireActivity()
            val viewModel =
                    androidx.lifecycle.ViewModelProvider(activity)[
                            com.vinaooo.revenger.viewmodels.GameActivityViewModel::class.java]
            viewModel.onRetroMenu3FragmentDestroyed()
        } catch (e: Exception) {
            MenuLogger.e("Error notifying ViewModel of fragment destruction")
            MenuLogger.e("MenuLifecycleManager", e)
        }

        // Clean up back stack change listener to prevent memory leaks
        // Note: This is handled by SubmenuCoordinator

        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            val activity = fragment.requireActivity()
            val viewModel =
                    androidx.lifecycle.ViewModelProvider(activity)[
                            com.vinaooo.revenger.viewmodels.GameActivityViewModel::class.java]
            // Call clearKeyLog through ViewModel to reset combo state
            viewModel.clearControllerKeyLog()
        } catch (e: Exception) {
            android.util.Log.w(
                    "MenuLifecycleManager",
                    "Error resetting combo state in onDestroy",
                    e
            )
        }
    }

    /**
     * Aplica as proporções de layout configuráveis ao menu. Detecta automaticamente se é portrait
     * ou landscape e aplica a configuração correta.
     */
    private fun applyConfigurableLayoutProportions(view: View) {
        try {
            // Aplicar todas as proporções (horizontal e vertical)
            com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig
                    .applyAllProportionsToMenuLayout(view)
        } catch (e: Exception) {
            android.util.Log.e("MenuLifecycleManager", "Error applying layout proportions", e)
        }
    }

    /**
     * Encontra o LinearLayout principal que contém a estrutura 3-colunas. Funciona para RetroMenu3,
     * SettingsMenu, ProgressMenu, AboutMenu e ExitMenu.
     */
    private fun findMainHorizontalLayout(view: View): android.widget.LinearLayout? {
        // IDs possíveis de containers principais (Filhos diretos da FrameLayout raiz)
        // O LinearLayout horizontal está normalmente como first child de FrameLayout ou
        // já é um container do menu (settings_menu_container, etc)

        // Primeiro tenta encontrar o LinearLayout que seja filho direto da FrameLayout raiz
        if (view is android.widget.FrameLayout) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.LinearLayout) {
                    val orientation = child.orientation
                    // Se for LinearLayout horizontal com 3+ filhos, provavelmente é o container
                    // correto
                    if (orientation == android.widget.LinearLayout.HORIZONTAL &&
                                    child.childCount >= 3
                    ) {
                        return child
                    }
                }
            }
        }

        MenuLogger.lifecycle("MenuLifecycleManager: onDestroy COMPLETED")
        return null
    }
}
