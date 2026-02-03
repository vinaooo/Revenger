package com.vinaooo.revenger.ui.retromenu3.navigation

import com.vinaooo.revenger.ui.retromenu3.MenuIndices

/**
 * Processador de eventos de navegação.
 *
 * Responsável pela lógica de processamento de eventos e execução de ações de navegação. Remove a
 * complexidade lógica do NavigationController.
 */
class NavigationEventProcessor(
        private val stateManager: NavigationStateManager,
        private val fragmentAdapter: FragmentNavigationAdapter,
        private val eventQueue: EventQueue,
        private val onMenuOpened: () -> Unit,
        private val onMenuClosed: (Int?) -> Unit
) {

    /** Rastreia o último botão que causou uma ação (para grace period) */
    private var lastActionButton: Int? = null

    companion object {
        private const val TAG = "NavigationEventProcessor"
    }

    /** Processa um evento de navegação. */
    fun processEvent(event: NavigationEvent) {
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
                android.util.Log.d(
                        TAG,
                        "[MENU_EVENT] ActivateSelected: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Salvar botão que ativou
                activateItem()
            }
            is NavigationEvent.NavigateBack -> {
                android.util.Log.d(
                        TAG,
                        "[MENU_EVENT] NavigateBack: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Salvar botão que voltou
                navigateBack()
            }
            is NavigationEvent.OpenMenu -> {
                android.util.Log.d(TAG, "[MENU_EVENT] OpenMenu: inputSource=${event.inputSource}")
                openMainMenu()
            }
            is NavigationEvent.CloseAllMenus -> {
                android.util.Log.d(
                        TAG,
                        "[MENU_EVENT] CloseAllMenus: keyCode=${event.keyCode}, inputSource=${event.inputSource}"
                )
                lastActionButton = event.keyCode // Salvar botão que fechou
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

    /** Navega para a esquerda (LEFT - usado para navegação em grids). */
    fun navigateLeft() {
        // Delegate navigation to fragment (only grid fragments override this)
        stateManager.currentFragment?.onNavigateLeft()

        // Sync selectedItemIndex with fragment's current selection
        stateManager.updateSelectedIndex(
                stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
        )
    }

    /** Navega para a direita (RIGHT - usado para navegação em grids). */
    fun navigateRight() {
        // Delegate navigation to fragment (only grid fragments override this)
        stateManager.currentFragment?.onNavigateRight()

        // Sync selectedItemIndex with fragment's current selection
        stateManager.updateSelectedIndex(
                stateManager.currentFragment?.getCurrentSelectedIndex() ?: 0
        )
    }

    /** Seleciona um item específico diretamente. */
    fun selectItem(index: Int) {
        if (index < 0 || index >= stateManager.currentMenuItemCount) {
            android.util.Log.w(
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
        android.util.Log.d(
                TAG,
                "Activate item: index=${stateManager.selectedItemIndex} (menu: ${stateManager.currentMenu})"
        )

        if (stateManager.currentMenu == MenuType.MAIN) {
            val targetMenu =
                    when (stateManager.selectedItemIndex) {
                        MenuIndices.CONTINUE -> {
                            // Continue
                            android.util.Log.d(TAG, "Continue selected - closing menu")
                            fragmentAdapter.hideMenu()
                            stateManager.unregisterFragment()
                            eventQueue.clear()
                            onMenuClosed(lastActionButton)
                            lastActionButton = null
                            return
                        }
                        MenuIndices.RESET -> {
                            // Reset
                            android.util.Log.d(TAG, "Reset selected")
                            val handled = stateManager.currentFragment?.onConfirm() ?: false
                            if (handled) {
                                android.util.Log.d(TAG, "Reset handled by fragment")
                            } else {
                                android.util.Log.w(TAG, "Reset NOT handled by fragment")
                            }
                            return
                        }
                        MenuIndices.PROGRESS -> MenuType.PROGRESS
                        MenuIndices.SETTINGS -> MenuType.SETTINGS
                        MenuIndices.ABOUT -> MenuType.ABOUT
                        MenuIndices.EXIT -> MenuType.EXIT
                        else -> {
                            android.util.Log.w(
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

            android.util.Log.d(TAG, "Navigated to submenu: $targetMenu")
        } else {
            android.util.Log.d(
                    TAG,
                    "Activating item in submenu ${stateManager.currentMenu} at index ${stateManager.selectedItemIndex}"
            )
            val handled = stateManager.currentFragment?.onConfirm() ?: false
            if (handled) {
                android.util.Log.d(TAG, "Item activation handled by fragment")
            } else {
                android.util.Log.w(TAG, "Item activation NOT handled by fragment")
            }
        }
    }

    /**
     * Navega para trás.
     * @return true se navegou para trás, false se já estava no menu principal
     */
    fun navigateBack(): Boolean {
        android.util.Log.d(TAG, "[NAVIGATE_BACK] Navigate back called")

        if (stateManager.currentMenu == MenuType.MAIN && stateManager.isStackEmpty()) {
            android.util.Log.d(TAG, "[NAVIGATE_BACK] At main menu, closing menu completely")
            android.util.Log.d(TAG, "[NAVIGATE_BACK] Resetting combo state before menu close")
            onMenuClosed(lastActionButton)

            fragmentAdapter.hideMenu()
            stateManager.unregisterFragment()
            eventQueue.clear()

            android.util.Log.d(TAG, "[NAVIGATE_BACK] Menu closed successfully")
            return true
        }

        val previousState = stateManager.popState()

        if (previousState != null) {
            stateManager.updateCurrentMenu(previousState.menuType)
            stateManager.updateSelectedIndex(previousState.selectedIndex)

            android.util.Log.d(
                    TAG,
                    "[NAVIGATE_BACK] Restored state: menu=${stateManager.currentMenu}, index=${stateManager.selectedItemIndex}"
            )

            if (stateManager.currentMenu == MenuType.MAIN && stateManager.isStackEmpty()) {
                android.util.Log.d(
                        TAG,
                        "[NAVIGATE_BACK] Returned to main menu, resetting combo state"
                )
                lastActionButton = null
            }

            val success = fragmentAdapter.navigateBack()
            android.util.Log.d(
                    TAG,
                    "[NAVIGATE_BACK] fragmentAdapter.navigateBack() returned: $success"
            )
            return success
        } else {
            stateManager.updateCurrentMenu(MenuType.MAIN)
            stateManager.updateSelectedIndex(0)

            android.util.Log.d(TAG, "[NAVIGATE_BACK] Stack empty, setting to main menu")

            val success = fragmentAdapter.navigateBack()
            android.util.Log.d(
                    TAG,
                    "[NAVIGATE_BACK] fragmentAdapter.navigateBack() returned: $success"
            )
            return success
        }
    }

    /** Abre o menu principal. */
    private fun openMainMenu() {
        android.util.Log.d(TAG, "[MENU_OPEN] Opening main menu")

        android.util.Log.d(TAG, "[MENU_OPEN] Calling onMenuOpenedCallback to pause game")
        onMenuOpened()
        android.util.Log.d(TAG, "[MENU_OPEN] onMenuOpenedCallback completed")

        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(0)
        stateManager.clearStack()

        android.util.Log.d(TAG, "[MENU_OPEN] Calling fragmentAdapter.showMenu(MAIN)")
        fragmentAdapter.showMenu(MenuType.MAIN)
        android.util.Log.d(TAG, "[MENU_OPEN] Main menu opened successfully")
    }

    /** Fecha todos os menus. */
    private fun closeAllMenus() {
        android.util.Log.d(TAG, "[MENU_CLOSE] Closing all menus")
        android.util.Log.d(TAG, "[MENU_CLOSE] lastActionButton: $lastActionButton")

        android.util.Log.d(TAG, "[MENU_CLOSE] Resetting combo state before menu close")
        onMenuClosed(lastActionButton)

        stateManager.updateCurrentMenu(MenuType.MAIN)
        stateManager.updateSelectedIndex(0)
        stateManager.clearStack()

        android.util.Log.d(TAG, "[MENU_CLOSE] Calling fragmentAdapter.hideMenu()")
        fragmentAdapter.hideMenu()
        stateManager.unregisterFragment()
        eventQueue.clear()

        android.util.Log.d(TAG, "[MENU_CLOSE] All menus closed successfully")
        lastActionButton = null
    }

    /** Atualiza o visual de seleção no fragmento atual. */
    fun updateSelectionVisual() {
        if (stateManager.currentFragment == null) {
            android.util.Log.w(TAG, "updateSelectionVisual: currentFragment is null")
            return
        }

        try {
            stateManager.currentFragment?.setSelectedIndex(stateManager.selectedItemIndex)
        } catch (e: IllegalStateException) {
            android.util.Log.w(
                    TAG,
                    "updateSelectionVisual: fragment detached, clearing reference",
                    e
            )
            stateManager.unregisterFragment()
        }
    }

    /** Fecha o menu externamente. */
    fun closeMenuExternal(closingButton: Int? = null) {
        android.util.Log.d(TAG, "[CLOSE_EXTERNAL] Closing menu externally, button: $closingButton")

        fragmentAdapter.hideMenu()
        stateManager.unregisterFragment()
        eventQueue.clear()

        onMenuClosed(closingButton)
    }
}
