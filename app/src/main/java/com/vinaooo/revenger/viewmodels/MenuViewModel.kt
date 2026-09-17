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
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel specialized in menu management. Responsible for all logic related to
 * menus, submenus, and navigation.
 */
class MenuViewModel(application: Application) :
        AndroidViewModel(application),
        MenuFragmentRegistration by MenuFragmentRegistry() {

    sealed class MenuEvent {
        object Idle : MenuEvent()
        data class ShowRetroMenu3(val activity: androidx.fragment.app.FragmentActivity) : MenuEvent()
        object DismissRetroMenu3 : MenuEvent()
        object DismissAllMenus : MenuEvent()
        object ClearControllerInputState : MenuEvent()
    }

    private val _eventFlow = MutableStateFlow<MenuEvent>(MenuEvent.Idle)
    val eventFlow: StateFlow<MenuEvent> = _eventFlow.asStateFlow()


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

    fun showRetroMenu3(activity: androidx.fragment.app.FragmentActivity) {
        _eventFlow.value = MenuEvent.ShowRetroMenu3(activity)
        menuStateManager.setRetroMenu3Open(true)
    }

    fun dismissRetroMenu3() {
        menuStateManager.setRetroMenu3Open(false)
        _eventFlow.value = MenuEvent.DismissRetroMenu3
    }

    fun dismissAllMenus() {
        menuStateManager.setDismissingAllMenus(true)
        _eventFlow.value = MenuEvent.DismissAllMenus
    }

    fun updateMenuState(newState: MenuState) {
        menuStateManager.changeState(newState)
    }

    fun clearControllerInputState() {
        _eventFlow.value = MenuEvent.ClearControllerInputState
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
