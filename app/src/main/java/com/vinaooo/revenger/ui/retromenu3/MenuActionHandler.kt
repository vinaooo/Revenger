package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
        Log.d(TAG, "[ACTION] Executing action: $action")

        when (action) {
            MenuAction.CONTINUE -> executeContinue()
            MenuAction.RESET -> executeReset()
            MenuAction.SAVE_LOG -> executeSaveLog()
            is MenuAction.NAVIGATE -> executeNavigate(action.targetMenu)
            else -> Log.w(TAG, "[ACTION] Unhandled action: $action")
        }
    }

    /** Executa ação de continuar jogo */
    private fun executeContinue() {
        Log.d(TAG, "[ACTION] 🎮 Continue game - closing menu")

        // Close menu first using the public method
        (fragment as? RetroMenu3Fragment)?.dismissMenuPublic()

        // Clear keyLog and reset comboAlreadyTriggered after closing
        viewModel.clearControllerInputState()

        // Set frameSpeed to correct value from Game Speed sharedPreference
        viewModel.restoreGameSpeedFromPreferences()
    }

    /** Executa ação de reset do jogo */
    private fun executeReset() {
        Log.d(TAG, "[ACTION] 🔄 Reset game - closing menu and resetting")

        // Close menu first using the public method
        (fragment as? RetroMenu3Fragment)?.dismissMenuPublic()

        // Clear keyLog and reset comboAlreadyTriggered after closing
        viewModel.clearControllerInputState()

        // Set frameSpeed to correct value from Game Speed sharedPreference
        viewModel.restoreGameSpeedFromPreferences()

        // Apply reset function
        viewModel.resetGameCentralized()
    }

    /** Executa ação de salvar log */
    private fun executeSaveLog() {
        Log.d(TAG, "[ACTION] 💾 Starting log file save process")

        // Run in background thread to avoid blocking UI
        val context = fragment.requireContext()
        fragment.lifecycleScope.launch {
            try {
                val filePath = com.vinaooo.revenger.utils.LogSaver.saveCompleteLog(context)

                if (filePath != null) {
                    Log.d(TAG, "[ACTION] ✅ Log file saved successfully: $filePath")

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
                    Log.e(TAG, "[ACTION] ❌ Failed to save log file")

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
                Log.e(TAG, "[ACTION] ❌ Exception while saving log", e)

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
        when (targetMenu) {
            MenuState.PROGRESS_MENU -> openProgressSubmenu()
            MenuState.SETTINGS_MENU -> openSettingsSubmenu()
            MenuState.EXIT_MENU -> openExitSubmenu()
            else -> Log.w(TAG, "[ACTION] Unknown menu state: $targetMenu")
        }
    }

    /** Abre submenu de progresso */
    private fun openProgressSubmenu() {
        Log.d(TAG, "[ACTION] 📊 Open Progress submenu")
        submenuCoordinator.openSubmenu(MenuState.PROGRESS_MENU)
    }

    /** Abre submenu de configurações */
    private fun openSettingsSubmenu() {
        Log.d(TAG, "[ACTION] ⚙️ Open Settings submenu")
        submenuCoordinator.openSubmenu(MenuState.SETTINGS_MENU)
    }

    /** Abre submenu de saída */
    private fun openExitSubmenu() {
        Log.d(TAG, "[ACTION] 🚪 Open Exit submenu")
        submenuCoordinator.openSubmenu(MenuState.EXIT_MENU)
    }

    /** Dismiss do menu (fecha o fragment) */
    private fun dismissMenu() {
        // Remove the fragment visually
        fragment.parentFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
    }
}
