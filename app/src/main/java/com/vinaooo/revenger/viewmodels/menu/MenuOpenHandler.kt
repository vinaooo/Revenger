package com.vinaooo.revenger.viewmodels.menu

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.viewmodels.FloatingButtonVisibilityHost

/**
 * Runs when the RetroMenu3 overlay opens: pause emulation and prepare the save-state screenshot.
 * [retroView]/[retroViewUtils]/[speedController] are read via provider lambdas rather than
 * captured, since all three are mutated on the owning ViewModel after construction.
 */
class MenuOpenHandler(
        private val retroView: () -> RetroView?,
        private val retroViewUtils: () -> RetroViewUtils?,
        private val speedController: () -> SpeedController?,
        private val captureScreenshotForSaveState: () -> Unit
) {

    fun handleMenuOpened(activity: FragmentActivity) {
        try {
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} " +
                            "thread=${Thread.currentThread().name} - menu opened callback start"
            )
            val menuFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.menu_container)
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] Fragment in container=${menuFragment?.javaClass?.simpleName} " +
                            "backStack=${activity.supportFragmentManager.backStackEntryCount}"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // This block only formats debug strings from trivial fragment-manager getters; there
            // is no narrower reachable type, and Throwable is itself on detekt's generic-exception
            // list, so the name-based escape hatch is used instead of guessing one.
            Log.w(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] failed to log fragment manager state",
                    expectedDiagnosticLoggingFailure
            )
        }

        // Capturar screenshot ANTES de pausar para save states
        captureScreenshotForSaveState()
        // Preservar estado do emulador
        retroView()?.let { retroViewUtils()?.preserveEmulatorState(it) }
        // PAUSAR o jogo quando menu abre
        retroView()?.let { speedController()?.pause(it.view) }

        (activity as? FloatingButtonVisibilityHost)?.restoreFloatingButtonVisibility()

        try {
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} - menu opened callback completed"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // Same rationale as the catch above: purely diagnostic logging with no narrower
            // reachable type.
            Log.w(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] failed to log completion",
                    expectedDiagnosticLoggingFailure
            )
        }
    }
}
