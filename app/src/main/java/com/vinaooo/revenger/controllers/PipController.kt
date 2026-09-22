package com.vinaooo.revenger.controllers

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.utils.PipLastSlotScreenshotResolver
import com.vinaooo.revenger.utils.PipSnapshotSelector
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Owns Picture-in-Picture orchestration for `GameActivity`: entering/exiting PiP, the PiP
 * overlay still-frame, the Quick Save / Save and Exit PiP actions and their broadcast receiver,
 * and keeping [android.app.PictureInPictureParams] current. Extracted out of `GameActivity` to
 * keep it within detekt's `TooManyFunctions`/`ReturnCount` thresholds; `GameActivity`'s lifecycle
 * overrides now delegate to this controller, each at the same point in the override body the
 * inline code used to run (see each method's own KDoc for its exact call-site position relative
 * to `super`).
 */
class PipController(
        private val host: PipHost,
        private val viewModel: GameActivityViewModel,
        private val appConfig: AppConfig,
        private val views: PipViews
) {
        companion object {
                private const val TAG = "PipController"
                const val ACTION_PIP_QUICK_SAVE = "com.vinaooo.revenger.PIP_QUICK_SAVE"
                const val ACTION_PIP_SAVE = "com.vinaooo.revenger.PIP_SAVE"
        }

        private val pipParamsFactory = PipParamsFactory(host, views.retroviewContainer, appConfig)
        private val pipQuickSaveExecutor = PipQuickSaveExecutor(host, viewModel)

        private var pendingPipQuickSave = false
        private var pendingPipSaveMenu = false
        private var wasGamepadVisibleBeforePip = false

        private val pipBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                                ACTION_PIP_QUICK_SAVE -> {
                                        // serializeState() would deadlock while in PiP (paused GL
                                        // thread). Leave PiP now and finish the save in
                                        // PipQuickSaveExecutor once resumed.
                                        pendingPipQuickSave = true
                                        host.bringTaskToFront()
                                }
                                ACTION_PIP_SAVE -> {
                                        val bitmap = ScreenshotCaptureUtil.getPipFrame()
                                        if (bitmap != null) {
                                                val config = bitmap.config ?: Bitmap.Config.ARGB_8888
                                                val bitmapCopy = bitmap.copy(config, true)
                                                ScreenshotCaptureUtil.setManualScreenshots(bitmapCopy, bitmapCopy)
                                                viewModel.suppressNextScreenshotCapture = true
                                        }

                                        // Open the save menu only AFTER we have fully left PiP —
                                        // onExitedPictureInPicture() forces frameSpeed = 1, so opening
                                        // (and pausing) the menu before that leaves the game running
                                        // behind it. See onPictureInPictureModeChanged().
                                        pendingPipSaveMenu = true
                                        host.bringTaskToFront()
                                }
                        }
                }
        }

        /** Call once from `GameActivity.onDestroy()`. */
        fun dispose() {
                // pipBroadcastReceiver is only unregistered on the "exited PiP" branch of
                // onPictureInPictureModeChanged; if the Activity is destroyed while still in
                // PiP (task swiped away, killed for memory, etc.) that branch never runs, so
                // unregister it here too. unregisterReceiver() throws IllegalArgumentException
                // if it was never registered (never entered PiP) -- catch that expected case.
                try {
                        host.unregisterPipReceiver(pipBroadcastReceiver)
                } catch (ignoredNeverRegisteredForPip: IllegalArgumentException) {
                        // Never entered PiP this session, or already unregistered -- expected.
                }
                clearPipOverlaySnapshot()
        }

        /** Call from `GameActivity.onPause()`, before `super.onPause()`. */
        fun onActivityPaused() {
                // Last chance to grab a game frame while the GL surface is still valid — the PiP
                // window relies entirely on this bitmap (the surface goes black during the
                // transition). No-op before the first rendered frame or when PiP is disabled.
                maybeCapturePipFrame(force = true)
        }

        /** Call from `GameActivity.onResume()`, after `super.onResume()`. No-op below SDK O. */
        fun onActivityResumed() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

                try {
                        // isCurrentlyInPip(), pipOverlay.visibility and clearPipOverlaySnapshot()
                        // document no throwable condition, so there is no narrower reachable type;
                        // kept as a defensive net.
                        if (!host.isCurrentlyInPip() && views.pipOverlay.visibility == View.VISIBLE) {
                                Log.d(TAG, "[PIP] Cleaning stuck PiP overlay in onResume")
                                clearPipOverlaySnapshot()
                        }
                } catch (expectedUnreachable: Exception) {
                        Log.e(TAG, "[PIP] Error checking PiP state in onResume", expectedUnreachable)
                }

                // Keep PiP params (aspect ratio, actions, auto-enter) current so a Home
                // gesture doesn't have to arm them during the same gesture it triggers.
                updatePictureInPictureParams()

                // Finish a Quick Save that was requested from the PiP window.
                if (pendingPipQuickSave) {
                        pendingPipQuickSave = false
                        pipQuickSaveExecutor.execute()
                }
        }

        /** Call from `GameActivity.onUserLeaveHint()`, after `super.onUserLeaveHint()`. */
        fun onUserLeaveHint() {
                if (viewModel.retroView?.frameRendered?.value != true) {
                        Log.d(TAG, "[PIP] Ignoring PiP request before first frame render")
                        return
                }
                if (!appConfig.isPipEnabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

                // Grab a frame now, while the GL surface is guaranteed valid and on-screen.
                maybeCapturePipFrame(force = true)

                if (viewModel.isAnyMenuActive()) {
                        Log.d(TAG, "[PIP] Closing active menu before entering PiP")
                        viewModel.dismissRetroMenu3 { maybeEnterPictureInPictureAfterMenuClosed() }
                } else {
                        maybeEnterPictureInPictureAfterMenuClosed()
                }
        }

        private fun maybeEnterPictureInPictureAfterMenuClosed() {
                if (viewModel.retroView?.frameRendered?.value != true) {
                        Log.d(TAG, "[PIP] Abort PiP transition: first frame not rendered yet")
                        return
                }
                if (!appConfig.isPipEnabled() ||
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                                viewModel.isAnyMenuActive()
                ) {
                        return
                }

                try {
                        // Make sure we have the freshest possible frame, then paint the overlay
                        // BEFORE the transition so the OS composites the still image, not black.
                        maybeCapturePipFrame(force = true)
                        showPipOverlaySnapshotIfAvailable()
                        host.gameLifecycleObserverOrNull()?.prepareForPipTransition()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                updatePictureInPictureParams()
                        } else {
                                val entered = host.enterPip(pipParamsFactory.newBuilder().build())
                                if (!entered) {
                                        Log.w(TAG, "[PIP] OS rejected Picture-in-Picture request")
                                        clearPipOverlaySnapshot()
                                        host.gameLifecycleObserverOrNull()?.clearPendingPipTransition()
                                }
                        }
                        // This block also fans through maybeCapturePipFrame() and
                        // showPipOverlaySnapshotIfAvailable() -> PipSnapshotSelector.select(),
                        // whose fallback suppliers (bitmap decode, GL surface capture) are not
                        // fully enumerable even after narrowing their own internal catches in this
                        // same change; kept broad as the last line of defense before a PiP-entry
                        // failure would otherwise crash the Activity.
                } catch (expectedPipEntryFailure: Exception) {
                        Log.e(TAG, "[PIP] Failed to enter Picture-in-Picture mode", expectedPipEntryFailure)
                        clearPipOverlaySnapshot()
                        host.gameLifecycleObserverOrNull()?.clearPendingPipTransition()
                }
        }

        /**
         * Refresh [ScreenshotCaptureUtil.getPipFrame] from the live GL surface. Throttled unless
         * [force]. Called on user input while playing and right before leaving the app, so the PiP
         * overlay always has a recent frame to show.
         */
        fun maybeCapturePipFrame(force: Boolean = false) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
                if (!appConfig.isPipEnabled()) return
                if (host.isCurrentlyInPip()) return
                if (viewModel.retroView?.frameRendered?.value != true) return
                val glRetroView = viewModel.retroView?.view ?: return
                ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = force)
        }

        /**
         * Paint the PiP overlay with the best game frame available and make it visible. The GL
         * surface is unreliable during and after the PiP resize, so this ImageView is the actual
         * source of the "paused game" picture in the PiP window. Falls back through progressively
         * older sources; only leaves the overlay hidden if nothing at all is available.
         */
        private fun showPipOverlaySnapshotIfAvailable() {
                val snapshot = PipSnapshotSelector.select(
                        { ScreenshotCaptureUtil.getPipFrame() },
                        { ScreenshotCaptureUtil.getCachedFullScreenshot() },
                        { ScreenshotCaptureUtil.getCachedScreenshot() },
                        { PipLastSlotScreenshotResolver.resolve(host.activity.applicationContext) }
                )
                if (snapshot != null) {
                        views.pipOverlay.setImageBitmap(snapshot)
                        views.pipOverlay.visibility = View.VISIBLE
                        Log.d(TAG, "[PIP] PiP overlay snapshot displayed")
                } else {
                        Log.w(TAG, "[PIP] No snapshot available for PiP overlay")
                }
        }

        private fun clearPipOverlaySnapshot() {
                views.pipOverlay.visibility = View.GONE
                views.pipOverlay.setImageDrawable(null)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun updatePictureInPictureParams() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
                try {
                        if (viewModel.retroView?.frameRendered?.value != true) {
                                Log.d(TAG, "[PIP] Skipping PiP params update before first frame render")
                                return
                        }

                        val builder = pipParamsFactory.newBuilder()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // Only auto-enter PiP on Home when the feature is actually enabled.
                                builder.setAutoEnterEnabled(appConfig.isPipEnabled())
                        }
                        // setPictureInPictureParams() documents IllegalArgumentException (invalid
                        // aspect ratio) and IllegalStateException (activity not visible/eligible)
                        // as its reachable failures; PipParamsFactory.newBuilder()'s own
                        // collaborators (PipConfigRepository, PipAspectRatioResolver) don't throw.
                        host.updatePipParams(builder.build())
                } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "[PIP] Failed to update Picture-in-Picture params", e)
                } catch (e: IllegalStateException) {
                        Log.e(TAG, "[PIP] Failed to update Picture-in-Picture params", e)
                }
        }

        /** Call from `GameActivity.onPictureInPictureModeChanged()`, after `super...` runs. */
        fun onPictureInPictureModeChanged(isInPip: Boolean) {
                val containers = host.activity.findViewById<View>(R.id.containers)
                val floatingBtn = host.activity.findViewById<View>(R.id.floating_menu_button)

                if (isInPip) {
                        showPipOverlaySnapshotIfAvailable()
                        host.gameLifecycleObserverOrNull()?.onEnteredPictureInPicture()

                        // Registrar receiver de botoes do PiP
                        val filter = IntentFilter().apply {
                                addAction(ACTION_PIP_QUICK_SAVE)
                                addAction(ACTION_PIP_SAVE)
                        }
                        host.registerPipReceiver(pipBroadcastReceiver, filter)

                        // Memorizar se o gamepad estava visível e escondê-lo para o PiP
                        wasGamepadVisibleBeforePip = (containers?.visibility == View.VISIBLE)
                        if (wasGamepadVisibleBeforePip) {
                                // Invisível para que as dimensões não quebrem
                                containers?.visibility = View.INVISIBLE
                        }

                        floatingBtn?.visibility = View.GONE
                        views.menuContainer.visibility = View.GONE
                } else {
                        host.gameLifecycleObserverOrNull()?.onExitedPictureInPicture()

                        // Desregistrar receiver. unregisterReceiver() throws
                        // IllegalArgumentException if it was never registered.
                        try {
                                host.unregisterPipReceiver(pipBroadcastReceiver)
                        } catch (e: IllegalArgumentException) {
                                Log.e(TAG, "Error unregistering pip receiver", e)
                        }

                        // Limpar overlay de imagem
                        clearPipOverlaySnapshot()

                        // Retornar os itens se estavam visíveis antes
                        if (wasGamepadVisibleBeforePip) {
                                containers?.visibility = View.VISIBLE
                        }

                        // Restaurar a renderização dependendo da configuração e reativar fade
                        viewModel.updateGamePadVisibility(host.activity, views.leftContainer, views.rightContainer, floatingBtn)
                        host.restoreFloatingButtonVisibility()

                        views.menuContainer.visibility = View.VISIBLE

                        // PiP "Save and Exit": now that onExitedPictureInPicture() has run (and set
                        // frameSpeed = 1), open the save menu — its own onMenuOpened() re-pauses the
                        // game and that pause now sticks. Backing out of it resumes the game via the
                        // rootless-submenu close path in NavigationEventProcessor.navigateBack().
                        if (pendingPipSaveMenu) {
                                pendingPipSaveMenu = false
                                viewModel.navigationController?.handleNavigationEvent(
                                        NavigationEvent.OpenMenu(
                                                inputSource = InputSource.TOUCH,
                                                targetMenu = MenuType.EXIT_SAVE_SLOTS
                                        )
                                )
                        }
                }
        }
}
