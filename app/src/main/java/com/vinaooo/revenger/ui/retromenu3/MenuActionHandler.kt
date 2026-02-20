package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Specialized class to process menu actions in the RetroMenu3Fragment. Responsible for executing
 * all menu actions (continue, reset, submenus, etc.) through a unified interface.
 */
class MenuActionHandler(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager,
        private val submenuCoordinator: SubmenuCoordinator
) {

        companion object {
                private const val TAG = "MenuActionHandler"
        }

        /** Executes a menu action based on MenuAction */
        fun executeAction(action: MenuAction) {
                MenuLogger.action("Executing action: $action")

                when (action) {
                        MenuAction.CONTINUE -> executeContinue()
                        MenuAction.RESET -> executeReset()
                        MenuAction.SAVE_LOG -> executeSaveLog()
                        is MenuAction.NAVIGATE -> executeNavigate(action.targetMenu)
                        else -> MenuLogger.w("[ACTION] Unhandled action: $action")
                }
        }

        /** Executes the continue game action */
        private fun executeContinue() {
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] ===== STARTING CONTINUE ====="
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Timestamp: ${System.currentTimeMillis()}"
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Fragment isAdded: ${fragment.isAdded}"
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Fragment isVisible: ${fragment.isVisible}"
                )

                MenuLogger.action("🎮 Continue game - closing menu")

                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Calling dismissMenuPublic() with callback"
                )
                // Close menu first using the public method with callback
                (fragment as? RetroMenu3Fragment)?.dismissMenuPublic {
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Animation completed - now restoring game speed"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Calling closeMenuExternal() to properly close menu and reset state"
                        )
                        // Properly close menu through NavigationController to trigger
                        // onMenuClosedCallback
                        viewModel.navigationController?.closeMenuExternal()
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] closeMenuExternal() completed - comboAlreadyTriggered should be reset now"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] REMOVED: restoreGameSpeedFromPreferences() - NavigationController handles speed restoration"
                        )
                        // REMOVED: NavigationController handles speed restoration
                        // viewModel.restoreGameSpeedFromPreferences()
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Speed restoration handled by NavigationController"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] ===== CONTINUE COMPLETED ====="
                        )
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Final Timestamp: ${System.currentTimeMillis()}"
                        )
                }
        }

        /** Executes the game reset action */
        private fun executeReset() {
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] ===== STARTING RESET ====="
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Timestamp: ${System.currentTimeMillis()}"
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Fragment isAdded: ${fragment.isAdded}"
                )
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Fragment isVisible: ${fragment.isVisible}"
                )

                MenuLogger.action("🔄 Reset game - closing menu and resetting")

                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Calling dismissMenuPublic() with callback"
                )
                // FIX: Set game speed to normal (1) before closing menu, since reset should start
                // fresh
                Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Setting speed to normal (1) for fresh game start"
                )
                viewModel.setGameSpeed(1)
                // Close menu first using the public method with callback
                (fragment as? RetroMenu3Fragment)?.dismissMenuPublic {
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Animation completed - now resetting game"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Calling closeMenuExternal() to properly close menu and reset state"
                        )
                        // Properly close menu through NavigationController to trigger
                        // onMenuClosedCallback
                        viewModel.navigationController?.closeMenuExternal()
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] closeMenuExternal() completed"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] REMOVED: restoreGameSpeedFromPreferences() - NavigationController handles speed restoration"
                        )
                        // REMOVED: NavigationController handles speed restoration
                        // viewModel.restoreGameSpeedFromPreferences()
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Speed restoration handled by NavigationController"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Calling resetGameCentralized()"
                        )
                        // Apply reset function
                        viewModel.resetGameCentralized()
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] resetGameCentralized() completed"
                        )

                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] ===== RESET COMPLETED ====="
                        )
                        Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Final Timestamp: ${System.currentTimeMillis()}"
                        )
                }
        }

        /** Executes the save log action */
        private fun executeSaveLog() {
                MenuLogger.action("💾 Starting log file save process")

                // Run in background thread to avoid blocking UI
                val context = fragment.requireContext()
                fragment.lifecycleScope.launch {
                        try {
                                val filePath =
                                        com.vinaooo.revenger.utils.LogSaver.saveCompleteLog(context)

                                if (filePath != null) {
                                        MenuLogger.action(
                                                "✅ Log file saved successfully: $filePath"
                                        )

                                        // Show success message on main thread
                                        withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Log saved: ${java.io.File(filePath).name}",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                        }
                                } else {
                                        MenuLogger.e("[ACTION] ❌ Failed to save log file")

                                        // Show error message on main thread
                                        withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Error saving log",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                        }
                                }
                        } catch (e: Exception) {
                                MenuLogger.e("[ACTION] ❌ Exception while saving log", e)

                                // Show error message on main thread
                                withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                                        context,
                                                        "Error saving log: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()
                                }
                        }
                }
        }

        /** Executes navigation to a submenu action */
        private fun executeNavigate(targetMenu: MenuState) {
                // The CURRENT SELECTED INDEX WILL BE SAVED DIRECTLY IN SubmenuCoordinator.openSubmenu
                MenuLogger.action("� Opening submenu: $targetMenu")

                when (targetMenu) {
                        MenuState.PROGRESS_MENU -> openProgressSubmenu()
                        MenuState.SETTINGS_MENU -> openSettingsSubmenu()
                        MenuState.ABOUT_MENU -> openAboutSubmenu()
                        MenuState.EXIT_MENU -> openExitSubmenu()
                        else -> MenuLogger.w("[ACTION] Unknown menu state: $targetMenu")
                }
        }

        /** Abre submenu de progresso */
        private fun openProgressSubmenu() {
                MenuLogger.action("📊 Open Progress submenu")
                submenuCoordinator.openSubmenu(MenuState.PROGRESS_MENU)
        }

        /** Opens settings submenu */
        private fun openSettingsSubmenu() {
                MenuLogger.action("⚙️ Open Settings submenu")
                submenuCoordinator.openSubmenu(MenuState.SETTINGS_MENU)
        }

        /** Abre submenu About */
        private fun openAboutSubmenu() {
                MenuLogger.action("📋 Open About submenu")
                submenuCoordinator.openSubmenu(MenuState.ABOUT_MENU)
        }

        /** Opens exit submenu */
        private fun openExitSubmenu() {
                MenuLogger.action("🚪 Open Exit submenu")
                submenuCoordinator.openSubmenu(MenuState.EXIT_MENU)
        }

        /** Dismiss do menu (fecha o fragment) */
        private fun dismissMenu() {
                // Remove the fragment visually
                fragment.parentFragmentManager
                        .beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss()
        }
}
