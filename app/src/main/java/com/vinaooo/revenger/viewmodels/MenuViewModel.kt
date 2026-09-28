package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.viewmodels.menu.MenuFragmentRegistration
import com.vinaooo.revenger.viewmodels.menu.MenuFragmentRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel specialized in menu management. Responsible for all logic related to
 * menus, submenus, and navigation.
 */
class MenuViewModel(application: Application) :
        AndroidViewModel(application),
        MenuFragmentRegistration by MenuFragmentRegistry() {


    // Menu state with StateFlow for reactivity
    private val menuStateManager = MenuStateManager { newState -> _menuState.value = newState }
    private val _menuState: MutableStateFlow<MenuSystemState>
    val menuState: StateFlow<MenuSystemState>
        get() = _menuState

    init {
        _menuState = MutableStateFlow(menuStateManager.currentState)
    }

    // RetroMenu3 menu state
    val isRetroMenu3Open: Boolean
        get() = menuStateManager.isRetroMenu3Open()

    val isDismissingAllMenus: Boolean
        get() = menuStateManager.isDismissingAllMenus()

    // ========== CONFIGURATION METHODS ==========
    // setMenuContainer/registerRetroMenu3Fragment/registerSettingsMenuFragment/
    // registerProgressFragment/registerExitFragment come from the MenuFragmentRegistration
    // delegation in the class header above.

    // ========== MENU CONTROL METHODS ==========

    fun dismissRetroMenu3() {
        menuStateManager.setRetroMenu3Open(false)
    }

    fun dismissAllMenus() {
        menuStateManager.setDismissingAllMenus(true)
    }

    fun updateMenuState(newState: MenuState) {
        menuStateManager.changeState(newState)
    }

    // ========== STATE CHECK METHODS ==========

    fun isSettingsMenuOpen(): Boolean =
            menuStateManager.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU)
}
