package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Classe especializada para processar ações de menu no RetroMenu3Fragment. Responsável por executar
 * todas as ações do menu (continue, reset, submenus, etc.) através de uma interface unificada.
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

        /** Executa uma ação de menu baseada no MenuAction */
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

        /** Executa ação de continuar jogo */
        private fun executeContinue() {
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] ===== STARTING CONTINUE ====="
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Fragment isAdded: ${fragment.isAdded}"
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Fragment isVisible: ${fragment.isVisible}"
                )

                MenuLogger.action("🎮 Continue game - closing menu")

                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_CONTINUE] Calling dismissMenuPublic() with callback"
                )
                // Close menu first using the public method with callback
                (fragment as? RetroMenu3Fragment)?.dismissMenuPublic {
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Animation completed - now restoring game speed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Calling clearControllerInputState()"
                        )
                        // Clear keyLog and reset comboAlreadyTriggered after closing
                        viewModel.clearControllerInputState()
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] clearControllerInputState() completed - comboAlreadyTriggered should be reset now"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Calling restoreGameSpeedFromPreferences()"
                        )
                        // Set frameSpeed to correct value from Game Speed sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] restoreGameSpeedFromPreferences() completed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] ===== CONTINUE COMPLETED ====="
                        )
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_CONTINUE] Final Timestamp: ${System.currentTimeMillis()}"
                        )
                }
        }

        /** Executa ação de reset do jogo */
        private fun executeReset() {
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] ===== STARTING RESET ====="
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Fragment isAdded: ${fragment.isAdded}"
                )
                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Fragment isVisible: ${fragment.isVisible}"
                )

                MenuLogger.action("🔄 Reset game - closing menu and resetting")

                android.util.Log.d(
                        "MenuActionHandler",
                        "🔥 [EXECUTE_RESET] Calling dismissMenuPublic() with callback"
                )
                // Close menu first using the public method with callback
                (fragment as? RetroMenu3Fragment)?.dismissMenuPublic {
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Animation completed - now restoring game speed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Calling clearControllerInputState()"
                        )
                        // Clear keyLog and reset comboAlreadyTriggered after closing
                        viewModel.clearControllerInputState()
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] clearControllerInputState() completed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Calling restoreGameSpeedFromPreferences()"
                        )
                        // Set frameSpeed to correct value from Game Speed sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] restoreGameSpeedFromPreferences() completed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Calling resetGameCentralized()"
                        )
                        // Apply reset function
                        viewModel.resetGameCentralized()
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] resetGameCentralized() completed"
                        )

                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] ===== RESET COMPLETED ====="
                        )
                        android.util.Log.d(
                                "MenuActionHandler",
                                "🔥 [EXECUTE_RESET] Final Timestamp: ${System.currentTimeMillis()}"
                        )
                }
        }

        /** Executa ação de salvar log */
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

        /** Executa ação de navegação para submenu */
        private fun executeNavigate(targetMenu: MenuState) {
                // O ÍNDICE SELECIONADO ATUAL SERÁ SALVO DIRETAMENTE NO openSubmenu DO
                // SubmenuCoordinator
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

        /** Abre submenu de configurações */
        private fun openSettingsSubmenu() {
                MenuLogger.action("⚙️ Open Settings submenu")
                submenuCoordinator.openSubmenu(MenuState.SETTINGS_MENU)
        }

        /** Abre submenu About */
        private fun openAboutSubmenu() {
                MenuLogger.action("📋 Open About submenu")
                submenuCoordinator.openSubmenu(MenuState.ABOUT_MENU)
        }

        /** Abre submenu de saída */
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
