package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuAction
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState

/**
 * Handles [com.vinaooo.revenger.ui.retromenu3.MenuEvent.Action]. [saveLoad] and
 * [submenuDismissal] are the owning ViewModel itself, typed as the narrow interfaces it already
 * implements, so calls keep going through the ViewModel's own overrides instead of bypassing
 * them. [menuManager] is read via a provider lambda rather than captured, since it's mutated on
 * the owning ViewModel after construction.
 */
class MenuActionDispatcher(
        private val saveLoad: SaveLoadCentralizedFacade,
        private val submenuDismissal: SubmenuFragmentDismissal,
        private val dismissRetroMenu3: () -> Unit,
        private val menuManager: () -> MenuManager,
        private val menuToggleActions: MenuToggleActions
) {

    fun handleAction(action: MenuAction) {
        when (action) {
            MenuAction.SAVE_STATE -> saveLoad.saveStateCentralized()
            MenuAction.LOAD_STATE -> saveLoad.loadStateCentralized()
            MenuAction.RESET -> saveLoad.resetGameCentralized()
            MenuAction.TOGGLE_AUDIO -> menuToggleActions.toggleAudio()
            MenuAction.TOGGLE_SPEED -> menuToggleActions.toggleSpeed()
            MenuAction.TOGGLE_SHADER -> menuToggleActions.toggleShader()
            MenuAction.SAVE_AND_EXIT -> {
                // Save and exit - same logic as in ExitFragment
                saveLoad.saveStateCentralized(
                        onComplete = { android.os.Process.killProcess(android.os.Process.myPid()) }
                )
            }
            MenuAction.EXIT -> {
                // Exit without save
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            MenuAction.BACK -> dismissCurrentMenuState()
            is MenuAction.NAVIGATE -> menuManager().navigateToState(action.targetMenu)
            else -> {
                // Ignore other actions
            }
        }
    }

    /** Handles the "back" action's per-state dismissal. */
    private fun dismissCurrentMenuState() {
        when (menuManager().getCurrentState()) {
            MenuState.MAIN_MENU -> dismissRetroMenu3()
            MenuState.SETTINGS_MENU -> submenuDismissal.dismissSettingsMenu()
            MenuState.PROGRESS_MENU -> submenuDismissal.dismissProgress()
            MenuState.ABOUT_MENU -> submenuDismissal.dismissAboutMenu()
            MenuState.EXIT_MENU -> submenuDismissal.dismissExit()
            else -> {}
        }
    }
}
