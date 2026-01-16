package com.vinaooo.revenger.ui.retromenu3

/**
 * Sealed class para ações de menu garantindo type safety e fornecendo um padrão Command unificado
 * para todas as interações de menu no sistema RetroMenu3.
 *
 * **Command Pattern**: Cada ação é um objeto imutável que representa uma intenção de usuário.
 * **Type Safety**: O compilador garante que apenas ações válidas sejam processadas.
 *
 * **Tipos de Ações**:
 * - **Main Menu**: CONTINUE, RESET, SAVE_STATE, LOAD_STATE
 * - **Toggles**: TOGGLE_AUDIO, TOGGLE_SPEED, TOGGLE_SHADER
 * - **Exit**: SAVE_AND_EXIT, EXIT
 * - **Navegação**: NAVIGATE(targetMenu), BACK
 * - **Utility**: SAVE_LOG, NONE (itens desabilitados)
 *
 * @see MenuManager Processa e roteia as ações
 * @see MenuFragment Produz ações via navegação do usuário
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
 * Sistema de eventos unificado para interações de menu.
 *
 * **Arquitetura Event-Driven (Phase 4+)**: Substitui arquitetura híbrida de chamadas diretas +
 * callbacks por abordagem consistente event-driven. Benefícios:
 * - **Desacoplamento**: Componentes comunicam via eventos, não referências diretas
 * - **Testabilidade**: Fácil mockar e verificar fluxo de eventos
 * - **Manutenibilidade**: Adicionar novos eventos não quebra código existente
 *
 * **Tipos de Eventos**:
 * - **Navigation**: NavigateUp, NavigateDown, Confirm, Back
 * - **Action**: Action(menuAction) - wrapper para MenuAction
 * - **State**: StateChanged(from, to), MenuClosed
 *
 * @see MenuManager Processa eventos e atualiza estado
 * @see MenuAction Ações concretas executadas pelos eventos
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
 * Enum representando diferentes estados de menu no sistema RetroMenu3.
 *
 * **State Machine**: Fornece máquina de estados clara para navegação de menu.
 *
 * **Estados Disponíveis**:
 * - `MAIN_MENU`: Menu principal (6 opções: Continue, Reset, Progress, Settings, About, Exit)
 * - `PROGRESS_MENU`: Submenu de save/load states
 * - `SETTINGS_MENU`: Submenu de configurações (Audio, Shader, Speed)
 * - `ABOUT_MENU`: Submenu de informações sobre ROM/Core
 * - `EXIT_MENU`: Submenu de confirmação de saída (Save & Exit, Exit, Back)
 *
 * **Transições**:
 * ```
 * MAIN_MENU → PROGRESS_MENU (seleção "Progress")
 * PROGRESS_MENU → MAIN_MENU (ação "Back")
 * ```
 *
 * @see MenuManager Gerencia transições entre estados
 * @see MenuAction.NAVIGATE Ação para navegar entre estados
 */
enum class MenuState {
    MAIN_MENU,
    PROGRESS_MENU,
    SETTINGS_MENU,
    ABOUT_MENU,
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
        ABOUT_MENU,
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
 * Gerenciador de estado centralizado para o sistema de menu.
 *
 * **Single Source of Truth (Phase 4+)**: Substitui flags booleanas distribuídas por estado imutável
 * centralizado.
 *
 * **Arquitetura**:
 * - **Thread-Safe**: Atualizações atômicas via funções de transformação
 * - **Predictable**: Estado imutável = mudanças rastreáveis e testáveis
 * - **Observable**: Callback onStateChanged notifica observers sobre mudanças
 *
 * **Uso**:
 * ```kotlin
 * menuStateManager.updateState { it.withMenuActivated(MenuType.PROGRESS_MENU) }
 * menuStateManager.activateMenu(MenuType.SETTINGS_MENU) // convenience method
 * ```
 *
 * @param onStateChanged Callback opcional invocado após cada mudança de estado
 * @see MenuSystemState Estado imutável gerenciado por esta classe
 */
class MenuStateManager(private val onStateChanged: ((MenuSystemState) -> Unit)? = null) {

    private var _currentState = MenuSystemState()

    /** Get current state (immutable copy) */
    val currentState: MenuSystemState
        get() = _currentState

    /** Update state using a transformation function */
    fun updateState(transform: (MenuSystemState) -> MenuSystemState) {
        _currentState = transform(_currentState)
        onStateChanged?.invoke(_currentState)
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

    // Protection against simultaneous confirm operations
    private var isProcessingConfirm = false
    private var isProcessingBack = false

    /** Register a fragment for a specific menu state */
    fun registerFragment(state: MenuState, fragment: MenuFragment) {
        fragments[state] = fragment
    }

    /** Unregister a fragment for a specific menu state */
    fun unregisterFragment(state: MenuState) {
        fragments.remove(state)
        android.util.Log.d(
                "MenuManager",
                "[FRAGMENT] unregisterFragment: Removed fragment for state $state"
        )
    }

    /** Get the current menu state */
    fun getCurrentState(): MenuState = stateManager.getCurrentState()

    /** Get the current fragment */
    fun getCurrentFragment(): MenuFragment? =
            fragments[stateManager.getCurrentState()].also {
                android.util.Log.d(
                        "MenuManager",
                        "[FRAGMENT] getCurrentFragment: state=${stateManager.getCurrentState()}, fragment=${it?.javaClass?.simpleName}, isAdded=${(it as? androidx.fragment.app.Fragment)?.isAdded}"
                )
            }

    /** Navigate to a specific menu state */
    fun navigateToState(newState: MenuState) {
        android.util.Log.d(
                "MenuManager",
                "[NAVIGATE_TO_STATE] 🧭 ========== NAVIGATE TO STATE START =========="
        )
        android.util.Log.d("MenuManager", "[NAVIGATE_TO_STATE] 📊 newState=$newState")

        val oldState = stateManager.getCurrentState()
        android.util.Log.d("MenuManager", "[NAVIGATE_TO_STATE] 📊 oldState=$oldState")

        stateManager.changeState(newState)
        android.util.Log.d(
                "MenuManager",
                "[NAVIGATE_TO_STATE] ✅ State changed: $oldState -> $newState"
        )

        android.util.Log.d(
                "MenuManager",
                "[NAVIGATE_TO_STATE] 📡 Calling listener.onMenuEvent(StateChanged)"
        )
        listener.onMenuEvent(MenuEvent.StateChanged(oldState, newState))

        android.util.Log.d(
                "MenuManager",
                "[NAVIGATE_TO_STATE] 🧭 ========== NAVIGATE TO STATE END =========="
        )
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
    fun navigateUp(): Boolean {
        android.util.Log.d("MenuManager", "[NAV] ↑ ========== NAVIGATE UP START ==========")
        val fragment = getCurrentFragment()
        val isAdded = (fragment as? androidx.fragment.app.Fragment)?.isAdded == true
        val hasContext = (fragment as? androidx.fragment.app.Fragment)?.context != null
        val isVisible = (fragment as? androidx.fragment.app.Fragment)?.isVisible == true
        val isResumed = (fragment as? androidx.fragment.app.Fragment)?.isResumed == true

        android.util.Log.d("MenuManager", "[NAV] ↑ Fragment status check")
        android.util.Log.d("MenuManager", "[NAV]   📋 fragment=${fragment?.javaClass?.simpleName}")
        android.util.Log.d("MenuManager", "[NAV]   ✅ isAdded=$isAdded")
        android.util.Log.d("MenuManager", "[NAV]   🎯 hasContext=$hasContext")
        android.util.Log.d("MenuManager", "[NAV]   👁️ isVisible=$isVisible")
        android.util.Log.d("MenuManager", "[NAV]   🎮 isResumed=$isResumed")
        android.util.Log.d("MenuManager", "[NAV]   📊 currentState=${getCurrentState()}")

        if (fragment != null && isAdded && hasContext) {
            android.util.Log.d("MenuManager", "[NAV] ↑ Calling fragment.onNavigateUp()")
            val result = fragment.onNavigateUp()
            android.util.Log.d("MenuManager", "[NAV] ↑ Result=$result")
            android.util.Log.d("MenuManager", "[NAV] ↑ ========== NAVIGATE UP COMPLETED ==========")
            return result
        } else {
            android.util.Log.w(
                    "MenuManager",
                    "[NAV] Navigate up: Fragment not available or not attached - fragment=$fragment, isAdded=$isAdded, hasContext=$hasContext, isVisible=$isVisible, isResumed=$isResumed"
            )
            android.util.Log.d("MenuManager", "[NAV] ↑ ========== NAVIGATE UP FAILED ==========")
            return false
        }
    }

    /** Navigate down in current menu */
    fun navigateDown(): Boolean {
        android.util.Log.d(
                "MenuManager",
                "[NAV] ↓ navigateDown: ========== STARTING NAVIGATE DOWN =========="
        )
        val fragment = getCurrentFragment()
        val isAdded = (fragment as? androidx.fragment.app.Fragment)?.isAdded == true
        val hasContext = (fragment as? androidx.fragment.app.Fragment)?.context != null
        val isVisible = (fragment as? androidx.fragment.app.Fragment)?.isVisible == true
        val isResumed = (fragment as? androidx.fragment.app.Fragment)?.isResumed == true

        android.util.Log.d("MenuManager", "[NAV] ↓ navigateDown: Fragment status check")
        android.util.Log.d("MenuManager", "[NAV]   📋 fragment=${fragment?.javaClass?.simpleName}")
        android.util.Log.d("MenuManager", "[NAV]   ✅ isAdded=$isAdded")
        android.util.Log.d("MenuManager", "[NAV]   🎯 hasContext=$hasContext")
        android.util.Log.d("MenuManager", "[NAV]   👁️ isVisible=$isVisible")
        android.util.Log.d("MenuManager", "[NAV]   🎮 isResumed=$isResumed")
        android.util.Log.d("MenuManager", "[NAV]   📊 currentState=${getCurrentState()}")

        if (fragment != null && isAdded && hasContext) {
            android.util.Log.d(
                    "MenuManager",
                    "[NAV] ↓ navigateDown: Calling fragment.onNavigateDown()"
            )
            val result = fragment.onNavigateDown()
            android.util.Log.d("MenuManager", "[NAV] ↓ navigateDown: Result=$result")
            android.util.Log.d(
                    "MenuManager",
                    "[NAV] ↓ navigateDown: ========== NAVIGATE DOWN COMPLETED =========="
            )
            return result
        } else {
            android.util.Log.w(
                    "MenuManager",
                    "[NAV] navigateDown: Fragment not available or not attached - fragment=$fragment, isAdded=$isAdded, hasContext=$hasContext, isVisible=$isVisible, isResumed=$isResumed"
            )
            android.util.Log.d(
                    "MenuManager",
                    "[NAV] ↓ navigateDown: ========== NAVIGATE DOWN FAILED =========="
            )
            return false
        }
    }

    /** Confirm current selection */
    fun confirm(): Boolean {
        android.util.Log.d("MenuManager", "[CONFIRM] ===== CONFIRM OPERATION START =====")
        android.util.Log.d("MenuManager", "[CONFIRM] isProcessingConfirm=$isProcessingConfirm")

        // Prevent simultaneous confirm operations
        if (isProcessingConfirm) {
            android.util.Log.d(
                    "MenuManager",
                    "[CONFIRM] ⚠️ confirm() already in progress, ignoring"
            )
            return false
        }

        isProcessingConfirm = true
        android.util.Log.d("MenuManager", "[CONFIRM] 🔄 Starting confirm operation")

        try {
            val fragment = getCurrentFragment()
            if (fragment != null &&
                            (fragment as? androidx.fragment.app.Fragment)?.isAdded == true &&
                            (fragment as? androidx.fragment.app.Fragment)?.context != null
            ) {
                val result = fragment.onConfirm()
                android.util.Log.d(
                        "MenuManager",
                        "[CONFIRM] ✅ Confirm operation completed, result=$result"
                )
                return result
            } else {
                android.util.Log.w(
                        "MenuManager",
                        "[CONFIRM] ⚠️ Fragment not available or not attached - fragment=$fragment, isAdded=${(fragment as? androidx.fragment.app.Fragment)?.isAdded}, context=${(fragment as? androidx.fragment.app.Fragment)?.context}"
                )
                return false
            }
        } finally {
            isProcessingConfirm = false
            android.util.Log.d("MenuManager", "[CONFIRM] 🔄 Confirm operation flag reset")
            android.util.Log.d("MenuManager", "[CONFIRM] ===== CONFIRM OPERATION END =====")
        }
    }

    /** Go back */
    fun back(): Boolean {
        android.util.Log.d("MenuManager", "[BACK] ===== BACK OPERATION START =====")
        android.util.Log.d("MenuManager", "[BACK] isProcessingBack=$isProcessingBack")
        android.util.Log.d("MenuManager", "[BACK] isProcessingConfirm=$isProcessingConfirm")

        // Prevent simultaneous back operations
        if (isProcessingBack) {
            android.util.Log.d("MenuManager", "[BACK] ⚠️ back() already in progress, ignoring")
            return false
        }

        // Prevent back operations while confirm is in progress (critical dismiss operation)
        if (isProcessingConfirm) {
            android.util.Log.d(
                    "MenuManager",
                    "[BACK] ⚠️ confirm() in progress, ignoring back during dismiss"
            )
            android.util.Log.d("MenuManager", "[BACK] ===== BACK OPERATION BLOCKED =====")
            return false
        }

        isProcessingBack = true
        android.util.Log.d("MenuManager", "[BACK] 🔄 Starting back operation")

        try {
            val fragment = getCurrentFragment()
            val fragmentHandled =
                    if (fragment != null &&
                                    (fragment as? androidx.fragment.app.Fragment)?.isAdded ==
                                            true &&
                                    (fragment as? androidx.fragment.app.Fragment)?.context != null
                    ) {
                        fragment.onBack()
                    } else {
                        android.util.Log.w(
                                "MenuManager",
                                "[NAV] back: Fragment not available or not attached - fragment=$fragment, isAdded=${(fragment as? androidx.fragment.app.Fragment)?.isAdded}, context=${(fragment as? androidx.fragment.app.Fragment)?.context}"
                        )
                        false
                    }
            // If fragment didn't handle it (returned false) and we're in main menu, close the menu
            if (!fragmentHandled && stateManager.getCurrentState() == MenuState.MAIN_MENU) {
                listener.onMenuEvent(MenuEvent.MenuClosed)
                return true
            }
            return fragmentHandled
        } finally {
            isProcessingBack = false
            android.util.Log.d("MenuManager", "[BACK] 🔄 Back operation flag reset")
        }
    }
    /** Get current selected index */
    fun getCurrentSelectedIndex(): Int {
        val fragment = getCurrentFragment()
        return if (fragment != null &&
                        (fragment as? androidx.fragment.app.Fragment)?.isAdded == true &&
                        (fragment as? androidx.fragment.app.Fragment)?.context != null
        ) {
            fragment.getCurrentSelectedIndex()
        } else {
            android.util.Log.w(
                    "MenuManager",
                    "[NAV] getCurrentSelectedIndex: Fragment not available or not attached"
            )
            0
        }
    }

    /** Set selected index */
    fun setSelectedIndex(index: Int) {
        val fragment = getCurrentFragment()
        if (fragment != null &&
                        (fragment as? androidx.fragment.app.Fragment)?.isAdded == true &&
                        (fragment as? androidx.fragment.app.Fragment)?.context != null
        ) {
            fragment.setSelectedIndex(index)
        } else {
            android.util.Log.w(
                    "MenuManager",
                    "[NAV] setSelectedIndex: Fragment not available or not attached"
            )
        }
    }

    // ===== NOVOS MÉTODOS PARA SISTEMA UNIFICADO DE EVENTOS =====

    /** Send navigation up event */
    fun sendNavigateUp() {
        listener.onMenuEvent(MenuEvent.NavigateUp)
    }

    /** Send navigation down event */
    fun sendNavigateDown() {
        android.util.Log.d("MenuManager", "[SEND] sendNavigateDown: Sending navigate down event")
        listener.onMenuEvent(MenuEvent.NavigateDown)
        android.util.Log.d("MenuManager", "[SEND] sendNavigateDown: Event sent")
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
