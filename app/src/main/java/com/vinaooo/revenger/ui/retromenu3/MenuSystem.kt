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
class MenuManager(private val listener: MenuManagerListener) {

    interface MenuManagerListener {
        fun onMenuAction(action: MenuAction)
        fun onMenuStateChanged(oldState: MenuState, newState: MenuState)
        fun onMenuClosed()
    }

    private var currentState: MenuState = MenuState.MAIN_MENU
    private val fragments = mutableMapOf<MenuState, MenuFragment>()

    /** Register a fragment for a specific menu state */
    fun registerFragment(state: MenuState, fragment: MenuFragment) {
        fragments[state] = fragment
    }

    /** Get the current menu state */
    fun getCurrentState(): MenuState = currentState

    /** Get the current fragment */
    fun getCurrentFragment(): MenuFragment? = fragments[currentState]

    /** Navigate to a specific menu state */
    fun navigateToState(newState: MenuState) {
        val oldState = currentState
        currentState = newState
        listener.onMenuStateChanged(oldState, newState)
    }

    /** Handle a menu action */
    fun handleAction(action: MenuAction) {
        when (action) {
            is MenuAction.NAVIGATE -> {
                navigateToState(action.targetMenu)
            }
            MenuAction.BACK -> {
                if (currentState != MenuState.MAIN_MENU) {
                    navigateToState(MenuState.MAIN_MENU)
                } else {
                    listener.onMenuClosed()
                }
            }
            else -> {
                listener.onMenuAction(action)
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
    fun back(): Boolean = getCurrentFragment()?.onBack() ?: false

    /** Get current selected index */
    fun getCurrentSelectedIndex(): Int = getCurrentFragment()?.getCurrentSelectedIndex() ?: 0

    /** Set selected index */
    fun setSelectedIndex(index: Int) {
        getCurrentFragment()?.setSelectedIndex(index)
    }
}
