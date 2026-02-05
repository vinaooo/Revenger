package com.vinaooo.revenger.ui.retromenu3


import com.vinaooo.revenger.utils.MenuLogger

/** Interface para processamento de entrada do usuário no menu. */
interface MenuInputHandler {
    fun setupInputHandling(menuViews: MenuViews)
    fun handleNavigateUp(): Boolean
    fun handleNavigateDown(): Boolean
    fun handleConfirm(): Boolean
    fun handleBack(): Boolean
    fun handleMenuItemSelected(item: MenuItem)
}

/**
 * Implementação básica do MenuInputHandler. TODO: Implementar lógica completa após criar
 * MenuCallbackManager.
 */
class MenuInputHandlerImpl(
        private val fragment: RetroMenu3Fragment,
        private val stateController: MenuStateController,
        private val callbackManager: MenuCallbackManager,
        private val actionHandler: MenuActionHandler
) : MenuInputHandler {

    override fun setupInputHandling(menuViews: MenuViews) {
        MenuLogger.action("MenuInputHandler: setupInputHandling - Configurando handlers de entrada")
        // TODO: Configurar listeners para botões virtuais e entrada física
    }

    override fun handleNavigateUp(): Boolean {
        MenuLogger.action("MenuInputHandler: handleNavigateUp called")
        return stateController.selectPreviousItem()
    }

    override fun handleNavigateDown(): Boolean {
        MenuLogger.action("MenuInputHandler: handleNavigateDown called")
        return stateController.selectNextItem()
    }

    override fun handleConfirm(): Boolean {
        MenuLogger.action("MenuInputHandler: handleConfirm called")
        val currentItem = stateController.getCurrentSelection()
        handleMenuItemSelected(currentItem)
        return true
    }

    override fun handleBack(): Boolean {
        MenuLogger.action("MenuInputHandler: handleBack called")
        // For main menu, back should close the menu
        // This will be handled by the MenuManager calling the appropriate action
        return false // Let MenuManager handle this
    }

    override fun handleMenuItemSelected(item: MenuItem) {
        MenuLogger.action(
                "MenuInputHandler: handleMenuItemSelected called with item: ${item.title}"
        )

        when (item.action) {
            MenuAction.CONTINUE -> {
                MenuLogger.action("MenuInputHandler: CONTINUE selected - executing action")
                actionHandler.executeAction(MenuAction.CONTINUE)
            }
            MenuAction.RESET -> {
                MenuLogger.action("MenuInputHandler: RESET selected - executing action")
                actionHandler.executeAction(MenuAction.RESET)
            }
            MenuAction.SAVE_STATE -> {
                MenuLogger.action("MenuInputHandler: SAVE_STATE selected - calling callback")
                callbackManager.onSaveState()
                // Open progress submenu for save state selection
                actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.PROGRESS_MENU))
            }
            MenuAction.TOGGLE_AUDIO -> {
                MenuLogger.action("MenuInputHandler: TOGGLE_AUDIO selected - calling callback")
                callbackManager.onToggleAudio()
                // Trigger settings menu click for UI feedback
                actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU))
            }
            MenuAction.EXIT -> {
                MenuLogger.action("MenuInputHandler: EXIT selected - opening exit menu")
                // Open exit confirmation submenu
                actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.EXIT_MENU))
            }
            is MenuAction.NAVIGATE -> {
                MenuLogger.action(
                        "MenuInputHandler: NAVIGATE selected - opening submenu ${item.action.targetMenu}"
                )
                actionHandler.executeAction(item.action)
            }
            else -> {
                MenuLogger.action("MenuInputHandler: Unknown action: ${item.action}")
            }
        }
    }
}
