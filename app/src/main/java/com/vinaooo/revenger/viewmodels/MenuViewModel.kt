package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.ui.retromenu3.*

/**
 * ViewModel especializado para gerenciamento de menus. Responsável por toda a lógica relacionada a
 * menus, submenus e navegação.
 */
class MenuViewModel(application: Application) : AndroidViewModel(application) {

    // Estado do menu
    private val menuStateManager = MenuStateManager()
    private var menuManager: MenuManager? = null

    // Referências aos fragments (mantidas para compatibilidade)
    private var retroMenu3Fragment: RetroMenu3Fragment? = null
    private var settingsMenuFragment: SettingsMenuFragment? = null
    private var progressFragment: ProgressFragment? = null
    private var exitFragment: ExitFragment? = null

    // Container do menu
    private var menuContainerView: android.widget.FrameLayout? = null

    // Estado do menu RetroMenu3
    val isRetroMenu3Open: Boolean
        get() = menuStateManager.isRetroMenu3Open()

    val isDismissingAllMenus: Boolean
        get() = menuStateManager.isDismissingAllMenus()

    // ========== MÉTODOS DE CONFIGURAÇÃO ==========

    fun setMenuContainer(container: android.widget.FrameLayout) {
        menuContainerView = container
    }

    fun registerRetroMenu3Fragment(fragment: RetroMenu3Fragment) {
        retroMenu3Fragment = fragment
    }

    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        settingsMenuFragment = fragment
    }

    fun registerProgressFragment(fragment: ProgressFragment) {
        progressFragment = fragment
    }

    fun registerExitFragment(fragment: ExitFragment) {
        exitFragment = fragment
    }

    // ========== MÉTODOS DE CONTROLE DE MENU ==========

    fun showRetroMenu3(activity: androidx.fragment.app.FragmentActivity) {
        // TODO: Implementar lógica completa de mostrar menu
        // Por enquanto, delegar para o estado
        menuStateManager.setRetroMenu3Open(true)
    }

    fun dismissRetroMenu3() {
        menuStateManager.setRetroMenu3Open(false)
        // TODO: Implementar lógica de fechar menu
    }

    fun dismissAllMenus() {
        menuStateManager.setDismissingAllMenus(true)
        // TODO: Implementar lógica de fechar todos os menus
    }

    fun updateMenuState(newState: MenuState) {
        menuStateManager.changeState(newState)
    }

    fun clearControllerInputState() {
        // TODO: Implementar limpeza do estado de input do controlador
    }

    // ========== MÉTODOS DE VERIFICAÇÃO DE ESTADO ==========

    fun isSettingsMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU)

    fun isProgressMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU)

    fun isExitMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU)

    // ========== MÉTODOS PRIVADOS ==========

    private fun activateMenu(menuType: MenuSystemState.MenuType) {
        menuStateManager.activateMenu(menuType)
    }

    private fun deactivateMenu(menuType: MenuSystemState.MenuType) {
        menuStateManager.deactivateMenu(menuType)
    }
}
