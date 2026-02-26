package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.ui.retromenu3.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel specialized in menu management. Responsible for all logic related to
 * menus, submenus, and navigation.
 */
class MenuViewModel(application: Application) : AndroidViewModel(application) {

    // Menu state with StateFlow for reactivity
    private val menuStateManager = MenuStateManager { newState -> _menuState.value = newState }
    private val _menuState: MutableStateFlow<MenuSystemState>
    val menuState: StateFlow<MenuSystemState>
        get() = _menuState

    init {
        _menuState = MutableStateFlow(menuStateManager.currentState)
    }

    private var menuManager: MenuManager? = null

    // References to fragments (kept for compatibility)
    private var retroMenu3Fragment: RetroMenu3Fragment? = null
    private var settingsMenuFragment: SettingsMenuFragment? = null
    private var progressFragment: ProgressFragment? = null
    private var exitFragment: ExitFragment? = null

    // Menu container
    private var menuContainerView: android.widget.FrameLayout? = null

    // RetroMenu3 menu state
    val isRetroMenu3Open: Boolean
        get() = menuStateManager.isRetroMenu3Open()

    val isDismissingAllMenus: Boolean
        get() = menuStateManager.isDismissingAllMenus()

    // ========== CONFIGURATION METHODS ==========

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

    // ========== MENU CONTROL METHODS ==========

    fun showRetroMenu3(activity: androidx.fragment.app.FragmentActivity) {
        // TODO: Implement full logic for showing menu
        // Por enquanto, delegar para o estado
        menuStateManager.setRetroMenu3Open(true)
    }

    fun dismissRetroMenu3() {
        menuStateManager.setRetroMenu3Open(false)
        // TODO: Implement menu closing logic
    }

    fun dismissAllMenus() {
        menuStateManager.setDismissingAllMenus(true)
        // TODO: Implement logic to close all menus
    }

    fun updateMenuState(newState: MenuState) {
        menuStateManager.changeState(newState)
    }

    fun clearControllerInputState() {
        // TODO: Implement clearing of controller input state
    }

    // ========== STATE CHECK METHODS ==========

    fun isSettingsMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU)

    fun isProgressMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU)

    fun isExitMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.EXIT_MENU)

    // ========== PRIVATE METHODS ==========

    private fun activateMenu(menuType: MenuSystemState.MenuType) {
        menuStateManager.activateMenu(menuType)
    }

    private fun deactivateMenu(menuType: MenuSystemState.MenuType) {
        menuStateManager.deactivateMenu(menuType)
    }
}
