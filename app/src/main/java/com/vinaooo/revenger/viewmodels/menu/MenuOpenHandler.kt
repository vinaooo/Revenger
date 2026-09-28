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
        val menuFragment = activity.supportFragmentManager.findFragmentById(R.id.menu_container)
        Log.d(
                TAG,
                "[ON_MENU_OPENED] Fragment in container=${menuFragment?.javaClass?.simpleName ?: "none"} " +
                        "backStack=${activity.supportFragmentManager.backStackEntryCount}"
        )

        // Capturar screenshot ANTES de pausar para save states
        captureScreenshotForSaveState()
        // Preservar estado do emulador
        retroView()?.let { retroViewUtils()?.preserveEmulatorState(it) }
        // PAUSAR o jogo quando menu abre
        retroView()?.let { speedController()?.pause(it.view) }

        (activity as? FloatingButtonVisibilityHost)?.restoreFloatingButtonVisibility()
    }

    private companion object {
        const val TAG = "GameActivityViewModel"
    }
}
