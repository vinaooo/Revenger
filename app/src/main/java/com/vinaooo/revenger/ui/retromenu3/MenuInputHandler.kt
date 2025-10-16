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
        private val stateController: MenuStateController
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
        // TODO: Implementar back - performBack é protected, precisa de método público no fragment
        return false
    }

    override fun handleMenuItemSelected(item: MenuItem) {
        MenuLogger.action(
                "MenuInputHandler: handleMenuItemSelected called with item: ${item.title}"
        )
        // TODO: Implementar seleção usando MenuCallbackManager
        when (item.action) {
            MenuAction.CONTINUE -> {
                // TODO: Chamar callback
                MenuLogger.action("MenuInputHandler: CONTINUE selected")
            }
            MenuAction.RESET -> {
                // TODO: Chamar callback
                MenuLogger.action("MenuInputHandler: RESET selected")
            }
            MenuAction.SAVE_STATE -> {
                // TODO: Chamar callback
                MenuLogger.action("MenuInputHandler: SAVE_STATE selected")
            }
            MenuAction.TOGGLE_AUDIO -> {
                // TODO: Chamar callback
                MenuLogger.action("MenuInputHandler: TOGGLE_AUDIO selected")
            }
            MenuAction.EXIT -> {
                // TODO: Chamar callback
                MenuLogger.action("MenuInputHandler: EXIT selected")
            }
            else -> {
                MenuLogger.action("MenuInputHandler: Unknown action: ${item.action}")
            }
        }
    }
}
