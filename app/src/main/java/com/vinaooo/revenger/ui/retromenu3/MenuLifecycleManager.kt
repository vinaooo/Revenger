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
        private val menuViewManager: MenuViewManager
) : MenuLifecycleManager {
    private lateinit var menuViews: MenuViews

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View {
        MenuLogger.lifecycle("MenuLifecycleManager: onCreateView START")
        return inflater.inflate(R.layout.retro_menu3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - Iniciando configuração")

        try {
            // Use the MenuViewManager instance passed from fragment
            MenuLogger.lifecycle("MenuLifecycleManager: MenuViewManager instance received")

            // Force all views to z=0 to stay below gamepad
            com.vinaooo.revenger.utils.ViewUtils.forceZeroElevationRecursively(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Elevation forced to zero")

            // Inicializar views através do viewInitializer
            val menuViews = viewInitializer.initializeViews(view)

            // CHAMAR setupViews DO FRAGMENT PARA CONFIGURAÇÕES ADICIONAIS
            fragment.setupViewsPublic(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Fragment setupViews completed")

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
            viewInitializer.setupClickListeners(menuViews) { menuItem ->
                inputHandler.handleMenuItemSelected(menuItem)
            }

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
        // TODO: Implementar updateControlsHint se necessário
        MenuLogger.lifecycle("MenuLifecycleManager: onResume COMPLETED")
    }

    override fun onDestroy() {
        MenuLogger.lifecycle("MenuLifecycleManager: onDestroy START")
        // TODO: Cleanup de listeners se necessário
        MenuLogger.lifecycle("MenuLifecycleManager: onDestroy COMPLETED")
    }
}
