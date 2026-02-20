package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import com.vinaooo.revenger.ui.retromenu3.MenuIndices

/**
 * Navigation event processor.
 *
 * Responsible for the logic of processing events and executing navigation actions. Removes the
 * logical complexity from the NavigationController.
 */
class NavigationEventProcessor(
        private val stateManager: NavigationStateManager,
        private val fragmentAdapter: FragmentNavigationAdapter,
        private val eventQueue: EventQueue,
        private val onMenuOpened: () -> Unit,
        private val onMenuClosed: (Int?) -> Unit
) {

    /** Tracks the last button that caused an action (for grace period) */
    private var lastActionButton: Int? = null

    companion object {
        private const val TAG = "NavigationEventProcessor"
    }

    /** Processes a navigation event. */
    fun processEvent(event: NavigationEvent) {
        try {
            Log.d(TAG, "[PROCESS_EVENT] ts=${System.currentTimeMillis()} thread=${Thread.currentThread().name} event=$event lastAction=$lastActionButton currentMenu=${stateManager.currentMenu} backStack=${fragmentAdapter.getBackStackCount()}")
        } catch (t: Throwable) {
            Log.w(TAG, "[PROCESS_EVENT] failed to log debug info", t)
        }
        when (event) {
            is NavigationEvent.Navigate -> {
                when (event.direction) {
                    Direction.UP -> navigateUp()
                    Direction.DOWN -> navigateDown()
                    Direction.LEFT -> navigateLeft()
                    Direction.RIGHT -> navigateRight()
                }
            }
            is NavigationEvent.SelectItem -> {
                selectItem(event.index)
            }
            is NavigationEvent.ActivateSelected -> {
                Log.d(
                        TAG,
                        "[MENU_EVENT] ActivateSelected: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Save button that activated
                activateItem()
            }
            is NavigationEvent.NavigateBack -> {
                Log.d(
                        TAG,
                        "[MENU_EVENT] NavigateBack: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Save button that returned
                navigateBack()
            }
            is NavigationEvent.OpenMenu -> {
                Log.d(TAG, "[MENU_EVENT] OpenMenu: inputSource=${event.inputSource}")
                openMainMenu()
            }
            is NavigationEvent.CloseAllMenus -> {
                Log.d(
                        TAG,
                        "[MENU_EVENT] CloseAllMenus: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Save button that closed
                closeAllMenus()
            }
        }
    }

    /** Navega para o item acima (UP). */
    fun navigateUp() {
        // PHASE 3.2: Delegate navigation to fragment to support custom logic
        stateManager.currentFragment?.onNavigateUp()

        // Sync selectedItemIndex with fragment's current selection
        stateManager.updateSelectedIndex(
                stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
        )
    }

    /** Navega para o item abaixo (DOWN). */
    fun navigateDown() {
        // PHASE 3.2: Delegate navigation to fragment to support custom logic
        stateManager.currentFragment?.onNavigateDown()

        // Sync selectedItemIndex with fragment's current selection
        stateManager.updateSelectedIndex(
                stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
        )
    }

    /** Navigate left (LEFT). Used for 2D grid navigation. */
    fun navigateLeft() {
        val handled = stateManager.currentFragment?.onNavigateLeft() ?: false
        if (handled) {
            // Sync selectedItemIndex with fragment's current selection
            stateManager.updateSelectedIndex(
                    stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
            )
        }
    }

    /** Navega para a direita (RIGHT). Usado para navegação 2D em grids. */
    fun navigateRight() {
        val handled = stateManager.currentFragment?.onNavigateRight() ?: false
        if (handled) {
            // Sync selectedItemIndex with fragment's current selection
            stateManager.updateSelectedIndex(
                    stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
            )
        }
    }

    /** Seleciona um item específico diretamente. */
    fun selectItem(index: Int) {
        if (index < 0 || index >= stateManager.currentMenuItemCount) {
            Log.w(
                    TAG,
                    "Invalid item index: $index (max: ${stateManager.currentMenuItemCount})"
            )
            return
        }

        stateManager.updateSelectedIndex(index)
        updateSelectionVisual()
    }

    /** Ativa o item atualmente selecionado. */
    fun activateItem() {
        Log.d(
                TAG,
                "Activate item: index=${stateManager.selectedItemIndex} (menu: ${stateManager.currentMenu})"
        )

        if (stateManager.currentMenu == MenuType.MAIN) {
            val targetMenu =
                    when (stateManager.selectedItemIndex) {
                        MenuIndices.CONTINUE -> {
                            // Continue
                            Log.d(TAG, "Continue selected - closing menu")
                            fragmentAdapter.hideMenu()
                            stateManager.unregisterFragment()
                            eventQueue.clear()
                            onMenuClosed(lastActionButton)
                            lastActionButton = null
                            return
                        }
                        MenuIndices.RESET -> {
                            // Reset
                            Log.d(TAG, "Reset selected")
                            val handled = stateManager.currentFragment?.onConfirm() ?: false
                            if (handled) {
                                Log.d(TAG, "Reset handled by fragment")
                            } else {
                                Log.w(TAG, "Reset NOT handled by fragment")
                            }
                            return
                        }
                        MenuIndices.PROGRESS -> MenuType.PROGRESS
                        MenuIndices.SETTINGS -> MenuType.SETTINGS
                        MenuIndices.ABOUT -> MenuType.ABOUT
                        MenuIndices.EXIT -> MenuType.EXIT
                        else -> {
                            Log.w(
                                    TAG,
                                    "Unknown menu item index: ${stateManager.selectedItemIndex}"
                            )
                            return
                        }
                    }

            stateManager.pushCurrentState()
            stateManager.updateCurrentMenu(targetMenu)
            stateManager.updateSelectedIndex(0)
            fragmentAdapter.showMenu(targetMenu)

            Log.d(TAG, "Navigated to submenu: $targetMenu")
        } else {
            Log.d(
                    TAG,
                    "Activating item in submenu ${stateManager.currentMenu} at index ${stateManager.selectedItemIndex}"
            )
            val handled = stateManager.currentFragment?.onConfirm() ?: false
            if (handled) {
                Log.d(TAG, "Item activation handled by fragment")
            } else {
                Log.w(TAG, "Item activation NOT handled by fragment")
            }
        }
    }

    /**
     * Navega para trás.
     * @return true se navegou para trás, false se já estava no menu principal
     */
    fun navigateBack(): Boolean {
        Log.d(TAG, "[NAVIGATE_BACK] Navigate back called")

        // IMPORTANT: First, let the current fragment handle the back event
        // This allows fragments with dialogs to consume the back event
        val fragment = stateManager.currentFragment
        if (fragment != null) {
            val consumed = fragment.onBack()
            Log.d(TAG, "[NAVIGATE_BACK] Fragment onBack() returned: $consumed")
            if (consumed) {
                // Fragment consumed the event (e.g., closed a dialog)
                // Don't navigate back in the menu stack
                return true
            }
        }

        if (stateManager.currentMenu == MenuType.MAIN && stateManager.isStackEmpty()) {
            Log.d(TAG, "[NAVIGATE_BACK] At main menu, closing menu completely")
            Log.d(TAG, "[NAVIGATE_BACK] Resetting combo state before menu close")
            onMenuClosed(lastActionButton)

            fragmentAdapter.hideMenu()
            stateManager.unregisterFragment()
            eventQueue.clear()

            Log.d(TAG, "[NAVIGATE_BACK] Menu closed successfully")
            return true
        }

        val previousState = stateManager.popState()

        if (previousState != null) {
            stateManager.updateCurrentMenu(previousState.menuType)
            stateManager.updateSelectedIndex(previousState.selectedIndex)

            Log.d(
                    TAG,
                    "[NAVIGATE_BACK] Restored state: menu=${stateManager.currentMenu}, index=${stateManager.selectedItemIndex}"
            )

            if (stateManager.currentMenu == MenuType.MAIN && stateManager.isStackEmpty()) {
                Log.d(
                        TAG,
                        "[NAVIGATE_BACK] Returned to main menu, resetting combo state"
                )
                lastActionButton = null
            }

            val success = fragmentAdapter.navigateBack()
            Log.d(
                    TAG,
                    "[NAVIGATE_BACK] fragmentAdapter.navigateBack() returned: $success"
            )
            return success
        } else {
            stateManager.updateCurrentMenu(MenuType.MAIN)
            stateManager.updateSelectedIndex(0)

            Log.d(TAG, "[NAVIGATE_BACK] Stack empty, setting to main menu")

            val success = fragmentAdapter.navigateBack()
            Log.d(
                    TAG,
                    "[NAVIGATE_BACK] fragmentAdapter.navigateBack() returned: $success"
            )
            return success
        }
    }

    /** Abre o menu principal. */
    private fun openMainMenu() {
        Log.d(TAG, "[MENU_OPEN] Opening main menu")

        Log.d(TAG, "[MENU_OPEN] Calling onMenuOpenedCallback to pause game")
        onMenuOpened()
        Log.d(TAG, "[MENU_OPEN] onMenuOpenedCallback completed")

        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(0)
        stateManager.clearStack()

        Log.d(TAG, "[MENU_OPEN] Calling fragmentAdapter.showMenu(MAIN)")
        fragmentAdapter.showMenu(MenuType.MAIN)
        Log.d(TAG, "[MENU_OPEN] Main menu opened successfully")
    }

    /** Fecha todos os menus. */
    private fun closeAllMenus() {
        Log.d(TAG, "[MENU_CLOSE] Closing all menus")
        Log.d(TAG, "[MENU_CLOSE] lastActionButton: $lastActionButton")

        Log.d(TAG, "[MENU_CLOSE] Resetting combo state before menu close")
        onMenuClosed(lastActionButton)

        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(0)
        stateManager.clearStack()

        Log.d(TAG, "[MENU_CLOSE] Calling fragmentAdapter.hideMenu()")
        fragmentAdapter.hideMenu()
        stateManager.unregisterFragment()
        eventQueue.clear()

        Log.d(TAG, "[MENU_CLOSE] All menus closed successfully")
        lastActionButton = null
    }

    /** Atualiza o visual de seleção no fragmento atual. */
    fun updateSelectionVisual() {
        if (stateManager.currentFragment == null) {
            Log.w(TAG, "updateSelectionVisual: currentFragment is null")
            return
        }

        try {
            stateManager.currentFragment?.setSelectedIndex(stateManager.selectedItemIndex)
        } catch (e: IllegalStateException) {
            Log.w(
                    TAG,
                    "updateSelectionVisual: fragment detached, clearing reference",
                    e
            )
            stateManager.unregisterFragment()
        }
    }

    /**
     * Navega para um submenu específico, empilhando o estado atual. Usado pelos fragments para
     * navegar para submenus mantendo o histórico.
     *
     * @param targetMenu Menu destino
     * @param saveCurrentState Se deve salvar o estado atual na pilha (default: true)
     */
    fun navigateToSubmenu(targetMenu: MenuType, saveCurrentState: Boolean = true) {
        Log.d(
                TAG,
                "[NAV_TO_SUBMENU] Navigating to $targetMenu from ${stateManager.currentMenu} (saveState=$saveCurrentState)"
        )

        if (saveCurrentState) {
            // Salvar estado atual na pilha antes de navegar
            stateManager.pushCurrentState()
            Log.d(
                    TAG,
                    "[NAV_TO_SUBMENU] Pushed state: menu=${stateManager.currentMenu}, index=${stateManager.selectedItemIndex}, stack size=${stateManager.getStackSize()}"
            )
        }

        // Atualizar para o novo menu
        stateManager.updateCurrentMenu(targetMenu)
        stateManager.updateSelectedIndex(0)

        // Mostrar o fragment do submenu
        fragmentAdapter.showMenu(targetMenu)

        Log.d(
                TAG,
                "[NAV_TO_SUBMENU] Now at $targetMenu with selection 0, stack size=${stateManager.getStackSize()}"
        )
    }

    /** Fecha o menu externamente. */
    fun closeMenuExternal(closingButton: Int? = null) {
        Log.d(TAG, "[CLOSE_EXTERNAL] Closing menu externally, button: $closingButton")

        fragmentAdapter.hideMenu()
        stateManager.unregisterFragment()
        eventQueue.clear()

        onMenuClosed(closingButton)
    }
}
