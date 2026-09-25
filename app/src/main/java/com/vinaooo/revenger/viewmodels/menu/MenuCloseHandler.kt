package com.vinaooo.revenger.viewmodels.menu

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.FloatingButtonVisibilityHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs when the RetroMenu3 overlay closes: reset combo state and resume emulation.
 * [retroView]/[controllerInput] are read via provider lambdas rather than captured, since both are
 * mutated on the owning ViewModel after construction. [viewModelScope] is passed directly since
 * it's a stable value for the ViewModel's lifetime.
 */
class MenuCloseHandler(
        private val retroView: () -> RetroView?,
        private val speedController: () -> SpeedController?,
        private val controllerInput: () -> ControllerInput,
        private val clearCachedScreenshot: () -> Unit,
        private val hideLoadPreview: () -> Unit,
        private val viewModelScope: CoroutineScope
) {

    companion object {
        // Grace period after the menu closes during which button interception stays active.
        // Covers the ~150ms hardware delay observed between ACTION_DOWN and ACTION_UP; a
        // shorter window (50ms) was found insufficient.
        private const val MENU_CLOSE_BUTTON_INTERCEPT_GRACE_MS = 200L
    }

    fun handleMenuClosed(activity: FragmentActivity, closingButton: Int?) {
        (activity as? FloatingButtonVisibilityHost)?.fadeFloatingButtonImmediately()

        Log.d("GameActivityViewModel", "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED =====")
        try {
            Log.d(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] ts=${System.currentTimeMillis()} " +
                            "thread=${Thread.currentThread().name} closingButton=${closingButton ?: "none"}"
            )
            val menuFragment = activity.supportFragmentManager.findFragmentById(R.id.menu_container)
            Log.d(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] Fragment in container=" +
                            "${menuFragment?.javaClass?.simpleName ?: "none"} " +
                            "backStack=${activity.supportFragmentManager.backStackEntryCount}"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // Same rationale as handleMenuOpened()'s diagnostic-logging catches: purely debug
            // string formatting with no narrower reachable type.
            Log.w(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] failed to log fragment manager state",
                    expectedDiagnosticLoggingFailure
            )
        }
        Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] Timestamp: ${System.currentTimeMillis()}"
        )
        Log.d("GameActivityViewModel", "🔥 [ON_MENU_CLOSED_CALLBACK] closingButton: ${closingButton ?: "none"}")

        val input = controllerInput()

        // Limpar botões de menu do keyLog para evitar "wasAlreadyPressed" bugs
        input.comboTracker.clearMenuActionButtons()

        // Reset combo state to allow SELECT+START to work again after menu closes
        input.comboTracker.resetComboAlreadyTriggered()

        // Clear keyLog immediately to prevent residual button states from causing combo
        // detection issues
        input.comboTracker.clearKeyLog()

        // Update menu close debounce time to prevent immediate combo detection
        input.comboTracker.updateMenuCloseDebounceTime()

        Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] comboAlreadyTriggered reset, keyLog cleared, debounce updated"
        )

        // Grace period: keep interception active for 200ms after menu closes
        // 200ms covers the ~150ms hardware delay between ACTION_DOWN and ACTION_UP
        // Identified via logs: UP arrives 150ms later; 50ms was insufficient
        // Block only the button that actually closed the menu
        input.callbackDebouncer.keepInterceptingButtons(
                MENU_CLOSE_BUTTON_INTERCEPT_GRACE_MS,
                closingButton = closingButton
        )

        // Keep the freshest known frame as the PiP still before dropping the menu caches.
        ScreenshotCaptureUtil.promoteCachedFullToPipFrame()

        // Limpar screenshot cacheado quando menu fecha
        clearCachedScreenshot()

        // RESUMIR o jogo quando menu fecha - aplicar velocidade salva nas preferences
        retroView()?.let { speedController()?.restoreSpeedFromPreferences(it.view) }

        // Hide load preview overlay AFTER game resumes and the first new frame is rendered
        retroView()?.view?.getGLRetroEvents()?.let { events ->
            viewModelScope.launch(Dispatchers.Main) {
                events.first { it == GLRetroView.GLRetroEvents.FrameRendered }
                hideLoadPreview()
            }
        }
                ?: run { hideLoadPreview() }

        Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED COMPLETED ====="
        )
    }
}
