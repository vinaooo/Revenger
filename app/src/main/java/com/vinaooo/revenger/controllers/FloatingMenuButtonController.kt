package com.vinaooo.revenger.controllers

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.viewmodels.FloatingButtonVisibilityHost
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Owns the floating menu button: config-driven gravity/visibility setup, click wiring, and the
 * fade-to-30%-after-inactivity / restore-on-menu-events behavior.
 *
 * Extracted from `GameActivity` (Task 10 of the split-god-classes refactor). `GameActivity`
 * keeps implementing [FloatingButtonVisibilityHost] itself (rather than this controller alone
 * satisfying it): `GameActivityViewModel` casts its `activity: FragmentActivity` parameter
 * directly to [FloatingButtonVisibilityHost] to call back into the host Activity, so the cast
 * target must remain the Activity instance. `GameActivity`'s two override methods are one-line
 * delegations to this controller.
 */
class FloatingMenuButtonController(
    private val floatingButton: Button,
    private val viewModel: GameActivityViewModel
) : FloatingButtonVisibilityHost {

    companion object {
        private const val TAG = "FloatingMenuButtonController"
    }

    private var fadeHandler: Handler? = null
    private var fadeRunnable: Runnable? = null

    /** Set up the floating menu button config and listener */
    fun setup() {
        val appConfig = RevengerApplication.appConfig
        val configValue = appConfig.getMenuModeFab().lowercase()

        if (configValue == "disabled") {
            floatingButton.visibility = View.GONE
            return
        }

        val layoutParams = floatingButton.layoutParams as FrameLayout.LayoutParams

        when (configValue) {
            "top-left" -> layoutParams.gravity = Gravity.TOP or Gravity.START
            "top-right" -> layoutParams.gravity = Gravity.TOP or Gravity.END
            "bottom-left" -> layoutParams.gravity = Gravity.BOTTOM or Gravity.START
            "bottom-right" -> layoutParams.gravity = Gravity.BOTTOM or Gravity.END
            else -> {
                Log.w(
                    TAG,
                    "Unknown floating menu button config: $configValue. Disabling floating button."
                )
                floatingButton.visibility = View.GONE
                return
            }
        }

        floatingButton.layoutParams = layoutParams
        val shouldShowGamePads =
            GamePad.shouldShowGamePads(floatingButton.context as Activity, appConfig)
        floatingButton.visibility = if (!shouldShowGamePads) View.VISIBLE else View.GONE

        floatingButton.setOnClickListener {
            Log.d(TAG, "Floating menu button clicked.")
            viewModel.toggleMainMenu()
        }

        // Setup fade handler
        fadeHandler = Handler(Looper.getMainLooper())
        fadeRunnable = Runnable { floatingButton.animate().alpha(1.0f).setDuration(500).start() }
    }

    /**
     * Fades the button to 30% alpha on user input (unless a menu is active or it's already
     * hidden), and schedules a restore back to full opacity in 10s of inactivity.
     */
    fun triggerFade() {
        if (viewModel.isAnyMenuActive()) return
        if (floatingButton.visibility != View.VISIBLE) return

        // Fade button to 30% alpha
        floatingButton.animate().alpha(0.3f).setDuration(200).start()

        // Cancel any pending restorative fades, and schedule a new one in 10s
        fadeRunnable?.let { runnable ->
            fadeHandler?.removeCallbacks(runnable)
            fadeHandler?.postDelayed(runnable, 10000)
        }
    }

    override fun restoreFloatingButtonVisibility() {
        if (floatingButton.visibility != View.VISIBLE) return

        fadeRunnable?.let { runnable -> fadeHandler?.removeCallbacks(runnable) }
        floatingButton.animate().alpha(1.0f).setDuration(200).start()
    }

    override fun fadeFloatingButtonImmediately() {
        if (floatingButton.visibility != View.VISIBLE) return

        floatingButton.animate().alpha(0.3f).setDuration(200).start()
        fadeRunnable?.let { runnable ->
            fadeHandler?.removeCallbacks(runnable)
            fadeHandler?.postDelayed(runnable, 10000)
        }
    }

    /**
     * Cancels a pending restorative fade, if any. Call from the host Activity's teardown (e.g.
     * onDestroy()) -- without this, a fade triggered shortly before destruction keeps this
     * controller's Runnable (which closes over [floatingButton]) alive on the main Handler for
     * up to 10s after the Activity is gone.
     */
    fun dispose() {
        fadeRunnable?.let { runnable -> fadeHandler?.removeCallbacks(runnable) }
    }
}
