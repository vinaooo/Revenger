package com.vinaooo.revenger.ui.retromenu3

/**
 * Sealed class for menu actions to ensure type safety and provide a unified command pattern for all
 * menu interactions in the RetroMenu3 system.
 */
sealed class MenuAction {
    // Main menu actions
    object CONTINUE : MenuAction()
    object RESET : MenuAction()
    object SAVE_STATE : MenuAction()
    object LOAD_STATE : MenuAction()
    object TOGGLE_AUDIO : MenuAction()
    object TOGGLE_SPEED : MenuAction()
    object TOGGLE_SHADER : MenuAction()
    object SAVE_AND_EXIT : MenuAction()
    object EXIT : MenuAction()
    object SAVE_LOG : MenuAction()
    object BACK : MenuAction()

    // Navigation actions
    data class NAVIGATE(val targetMenu: MenuState) : MenuAction()

    // No operation (for disabled items)
    object NONE : MenuAction()
}

/**
 * Unified event system for menu interactions. Replaces the hybrid architecture of direct method
 * calls and callbacks with a single, consistent event-driven approach. This enables better
 * decoupling, testability, and maintainability of the menu system.
 */
sealed class MenuEvent {
    // Navigation events
    object NavigateUp : MenuEvent()
    object NavigateDown : MenuEvent()
    object Confirm : MenuEvent()
    object Back : MenuEvent()

    // Action events
    data class Action(val action: MenuAction) : MenuEvent()

    // State change events
    data class StateChanged(val from: MenuState, val to: MenuState) : MenuEvent()
    object MenuClosed : MenuEvent()
}

/**
 * Enum representing different menu states in the RetroMenu3 system. This provides a clear state
 * machine for menu navigation.
 */
enum class MenuState {
    MAIN_MENU,
    PROGRESS_MENU,
    SETTINGS_MENU,
    EXIT_MENU
}

/**
 * Centralized state representation for the entire menu system. Replaces the distributed boolean
 * flags and manual state management with a single, consistent state object. This enables better
 * state tracking, debugging, and prevents inconsistent states.
 */
data class MenuSystemState(
        val currentState: MenuState = MenuState.MAIN_MENU,
        val activeMenus: Set<MenuType> = emptySet(),
        val navigationStack: List<MenuState> = emptyList(),
        val isRetroMenu3Open: Boolean = false,
        val isDismissingAllMenus: Boolean = false
) {
    enum class MenuType {
        RETRO_MENU_3,
        SETTINGS_MENU,
        PROGRESS_MENU,
        EXIT_MENU
    }

    /** Check if any menu is currently active */
    fun hasActiveMenus(): Boolean = activeMenus.isNotEmpty()

    /** Check if a specific menu type is active */
    fun isMenuActive(menuType: MenuType): Boolean = menuType in activeMenus

    /** Add a menu to the active set */
    fun withMenuActivated(menuType: MenuType): MenuSystemState =
            copy(activeMenus = activeMenus + menuType)

    /** Remove a menu from the active set */
    fun withMenuDeactivated(menuType: MenuType): MenuSystemState =
            copy(activeMenus = activeMenus - menuType)

    /** Change current state */
    fun withState(newState: MenuState): MenuSystemState = copy(currentState = newState)

    /** Push state to navigation stack */
    fun withStatePushed(state: MenuState): MenuSystemState =
            copy(navigationStack = navigationStack + state)

    /** Pop state from navigation stack */
    fun withStatePopped(): MenuSystemState = copy(navigationStack = navigationStack.dropLast(1))

    /** Set RetroMenu3 open/closed */
    fun withRetroMenu3Open(open: Boolean): MenuSystemState = copy(isRetroMenu3Open = open)

    /** Set dismissing all menus flag */
    fun withDismissingAllMenus(dismissing: Boolean): MenuSystemState =
            copy(isDismissingAllMenus = dismissing)
}

/**
 * Centralized state manager for the menu system. Provides a single source of truth for all menu
 * state, replacing the distributed boolean flags. Uses immutable state updates for thread safety
 * and predictability.
 */
class MenuStateManager {

    private var _currentState = MenuSystemState()

    /** Get current state (immutable copy) */
    val currentState: MenuSystemState
        get() = _currentState

    /** Update state using a transformation function */
    fun updateState(transform: (MenuSystemState) -> MenuSystemState) {
        _currentState = transform(_currentState)
        android.util.Log.d(TAG, "Menu state updated: $_currentState")
    }

    /** Convenience methods for common state changes */
    fun activateMenu(menuType: MenuSystemState.MenuType) {
        updateState { it.withMenuActivated(menuType) }
    }

    fun deactivateMenu(menuType: MenuSystemState.MenuType) {
        updateState { it.withMenuDeactivated(menuType) }
    }

    fun changeState(newState: MenuState) {
        updateState { it.withState(newState) }
    }

    fun pushToNavigationStack(state: MenuState) {
        updateState { it.withStatePushed(state) }
    }

    fun popFromNavigationStack() {
        updateState { it.withStatePopped() }
    }

    fun setRetroMenu3Open(open: Boolean) {
        updateState { it.withRetroMenu3Open(open) }
    }

    fun setDismissingAllMenus(dismissing: Boolean) {
        updateState { it.withDismissingAllMenus(dismissing) }
    }

    /** Query methods */
    fun hasActiveMenus(): Boolean = _currentState.hasActiveMenus()

    fun isMenuActive(menuType: MenuSystemState.MenuType): Boolean =
            _currentState.isMenuActive(menuType)

    fun isRetroMenu3Open(): Boolean = _currentState.isRetroMenu3Open

    fun isDismissingAllMenus(): Boolean = _currentState.isDismissingAllMenus

    fun getCurrentState(): MenuState = _currentState.currentState

    companion object {
        private const val TAG = "MenuStateManager"
    }
}

/**
 * Data class representing a menu item with all necessary properties. Provides a standardized way to
 * define menu items across all fragments.
 */
data class MenuItem(
        val id: String,
        val title: String,
        val subtitle: String? = null,
        val iconResId: Int? = null,
        val isEnabled: Boolean = true,
        val isSelected: Boolean = false,
        val action: MenuAction = MenuAction.NONE
)

/**
 * Unified interface for all menu fragments in the RetroMenu3 system. This ensures consistent
 * behavior and navigation across all menu components.
 */
interface MenuFragment {
    /** Returns the list of menu items for this fragment */
    fun getMenuItems(): List<MenuItem>

    /** Called when a menu item is selected (clicked or confirmed via gamepad) */
    fun onMenuItemSelected(item: MenuItem)

    /**
     * Navigate up in the menu (gamepad DPAD up)
     * @return true if navigation was handled, false otherwise
     */
    fun onNavigateUp(): Boolean

    /**
     * Navigate down in the menu (gamepad DPAD down)
     * @return true if navigation was handled, false otherwise
     */
    fun onNavigateDown(): Boolean

    /**
     * Confirm current selection (gamepad A button)
     * @return true if action was handled, false otherwise
     */
    fun onConfirm(): Boolean

    /**
     * Go back (gamepad B button or back navigation)
     * @return true if back was handled, false otherwise
     */
    fun onBack(): Boolean

    /** Get the currently selected index */
    fun getCurrentSelectedIndex(): Int

    /** Set the selected index programmatically */
    fun setSelectedIndex(index: Int)
}

/**
 * Central menu manager that coordinates all menu fragments and handles state transitions. This
 * implements the State Machine pattern for menu navigation.
 */
class MenuManager(
        private val listener: MenuManagerListener,
        private val stateManager: MenuStateManager
) {

    interface MenuManagerListener {
        fun onMenuEvent(event: MenuEvent)
    }

    private val fragments = mutableMapOf<MenuState, MenuFragment>()

    /** Register a fragment for a specific menu state */
    fun registerFragment(state: MenuState, fragment: MenuFragment) {
        fragments[state] = fragment
    }

    /** Get the current menu state */
    fun getCurrentState(): MenuState = stateManager.getCurrentState()

    /** Get the current fragment */
    fun getCurrentFragment(): MenuFragment? = fragments[stateManager.getCurrentState()]

    /** Navigate to a specific menu state */
    fun navigateToState(newState: MenuState) {
        val oldState = stateManager.getCurrentState()
        stateManager.changeState(newState)
        listener.onMenuEvent(MenuEvent.StateChanged(oldState, newState))
    }

    /** Handle a menu action */
    fun handleAction(action: MenuAction) {
        when (action) {
            is MenuAction.NAVIGATE -> {
                navigateToState(action.targetMenu)
            }
            MenuAction.BACK -> {
                if (stateManager.getCurrentState() != MenuState.MAIN_MENU) {
                    navigateToState(MenuState.MAIN_MENU)
                } else {
                    listener.onMenuEvent(MenuEvent.MenuClosed)
                }
            }
            else -> {
                listener.onMenuEvent(MenuEvent.Action(action))
            }
        }
    }

    /** Navigate up in current menu */
    fun navigateUp(): Boolean = getCurrentFragment()?.onNavigateUp() ?: false

    /** Navigate down in current menu */
    fun navigateDown(): Boolean = getCurrentFragment()?.onNavigateDown() ?: false

    /** Confirm current selection */
    fun confirm(): Boolean = getCurrentFragment()?.onConfirm() ?: false

    /** Go back */
    fun back(): Boolean {
        val fragmentHandled = getCurrentFragment()?.onBack() ?: false
        // If fragment didn't handle it (returned false) and we're in main menu, close the menu
        if (!fragmentHandled && stateManager.getCurrentState() == MenuState.MAIN_MENU) {
            listener.onMenuEvent(MenuEvent.MenuClosed)
            return true
        }
        return fragmentHandled
    }

    /** Get current selected index */
    fun getCurrentSelectedIndex(): Int = getCurrentFragment()?.getCurrentSelectedIndex() ?: 0

    /** Set selected index */
    fun setSelectedIndex(index: Int) {
        getCurrentFragment()?.setSelectedIndex(index)
    }

    // ===== NOVOS MÉTODOS PARA SISTEMA UNIFICADO DE EVENTOS =====

    /** Send navigation up event */
    fun sendNavigateUp() {
        listener.onMenuEvent(MenuEvent.NavigateUp)
    }

    /** Send navigation down event */
    fun sendNavigateDown() {
        listener.onMenuEvent(MenuEvent.NavigateDown)
    }

    /** Send confirm event */
    fun sendConfirm() {
        listener.onMenuEvent(MenuEvent.Confirm)
    }

    /** Send back event */
    fun sendBack() {
        listener.onMenuEvent(MenuEvent.Back)
    }

    /** Send menu action event */
    fun sendAction(action: MenuAction) {
        listener.onMenuEvent(MenuEvent.Action(action))
    }
}
