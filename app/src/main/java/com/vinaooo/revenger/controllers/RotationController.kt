package com.vinaooo.revenger.controllers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Owns rotation/auto-rotate orchestration for `GameActivity`: listening for system auto-rotate
 * setting changes, reapplying the configured orientation, and (via [MenuRotationRecreator])
 * rebuilding the in-game menu after a configuration change if one was open. Extracted out of
 * `GameActivity` to keep it within detekt's `TooManyFunctions` threshold.
 */
class RotationController(
        private val activity: FragmentActivity,
        private val viewModel: GameActivityViewModel,
        private val appConfig: AppConfig
) {
        companion object {
                private const val TAG = "RotationController"
        }

        private val menuRotationRecreator = MenuRotationRecreator(activity, viewModel)

        private var rotationSettingsReceiver: BroadcastReceiver? = null

        /**
         * Registers a listener to monitor changes in the system auto-rotate setting. When the
         * user toggles auto-rotate in system settings, the app's orientation is automatically
         * reapplied. Call once from `GameActivity.onCreate()`.
         */
        fun register() {
                rotationSettingsReceiver =
                        object : BroadcastReceiver() {
                                override fun onReceive(context: Context?, intent: Intent?) {
                                        if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                                                // Configuration changed (could be auto-rotate)
                                                Log.d(
                                                        TAG,
                                                        "[ROTATION_LISTENER] System configuration changed - " +
                                                                "checking auto-rotate"
                                                )
                                                reapplyOrientation()
                                        }
                                }
                        }

                // Create IntentFilter to detect configuration changes
                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)

                // Register receiver with appropriate permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.registerReceiver(
                                rotationSettingsReceiver,
                                intentFilter,
                                Context.RECEIVER_EXPORTED
                        )
                } else {
                        activity.registerReceiver(rotationSettingsReceiver, intentFilter)
                }

                Log.d(TAG, "[ROTATION_LISTENER] BroadcastReceiver registered to monitor changes")
        }

        /** Call once from `GameActivity.onDestroy()`. */
        fun dispose() {
                // unregisterReceiver() throws IllegalArgumentException if the receiver was never
                // registered -- expected if rotation listening was never set up.
                rotationSettingsReceiver?.let {
                        try {
                                activity.unregisterReceiver(it)
                                Log.d(TAG, "[ROTATION_LISTENER] BroadcastReceiver unregistered")
                        } catch (e: IllegalArgumentException) {
                                Log.e(TAG, "[ROTATION_LISTENER] Erro ao desregistrar receiver", e)
                        }
                }
        }

        /**
         * Reapplies orientation configuration when the auto-rotate preference changes. This
         * allows the app to dynamically respond to system changes.
         */
        // Settings.System.getInt(cr, name, def) never throws SettingNotFoundException (that's
        // only the 2-arg overload without a default), but OEM-modified ContentResolvers are known
        // to fail in ways that aren't enumerable (SecurityException, provider-specific
        // RuntimeExceptions); treat any failure as auto-rotate being off, matching the
        // pre-existing fallback -- kept broad via detekt's documented name escape hatch.
        private fun readAutoRotateSetting(): Boolean =
                try {
                        Settings.System.getInt(
                                activity.contentResolver,
                                Settings.System.ACCELEROMETER_ROTATION,
                                0
                        ) == 1
                } catch (expectedSettingsReadFailure: Exception) {
                        Log.w(TAG, "[ROTATION] Could not read auto-rotate setting", expectedSettingsReadFailure)
                        false
                }

        private fun reapplyOrientation() {
                try {
                        val wasAutoRotate = readAutoRotateSetting()

                        Log.d(TAG, "[ROTATION_REAPPLY] System auto-rotate: $wasAutoRotate")

                        // Reapply orientation configuration based on new state
                        viewModel.setConfigOrientation(activity)

                        Log.d(TAG, "[ROTATION_REAPPLY] Orientation successfully reapplied")
                        // setConfigOrientation() fans out into OrientationManager and
                        // Activity.requestedOrientation, whose full set of reachable exceptions
                        // isn't enumerable from here; kept broad via the escape hatch rather than
                        // guessing a narrower type, to preserve never letting a reapply failure
                        // crash.
                } catch (expectedReapplyFailure: Exception) {
                        Log.e(TAG, "[ROTATION_REAPPLY] Error reapplying orientation", expectedReapplyFailure)
                }
        }

        /**
         * Call from `GameActivity.onConfigurationChanged()`, after `super...` runs. Reapplies the
         * configured orientation unless the user asked for auto-rotate with the system setting
         * off.
         */
        fun reapplyOrientationIfNeeded(newConfig: Configuration) {
                Log.d(TAG, "Configuration changed - orientation=${newConfig.orientation}")

                // Check if we should reprocess orientation
                // DO NOT reprocess when config=3 and auto-rotate=OFF (to allow manual button)
                val configOrientation = appConfig.getOrientation()
                val autoRotateEnabled = readAutoRotateSetting()

                // Only reapply orientation if config=1 or 2 (forced) or if config=3 with
                // auto-rotate ON
                if (configOrientation != "auto" || autoRotateEnabled) {
                        reapplyOrientation()
                } else {
                        Log.d(
                                TAG,
                                "[ROTATION] config=3 + auto-rotate OFF - not reapplying (allows manual button)"
                        )
                }
        }

        /**
         * Call from `GameActivity.onConfigurationChanged()`. Re-registers menu callbacks (lost on
         * rotation) and, if a menu fragment is visible, schedules
         * [MenuRotationRecreator.scheduleMenuRecreationAfterRotation] to rebuild it.
         */
        fun maybeRecreateMenuAfterRotation() {
                // CRITICAL FIX: Re-register menu callbacks after rotation to prevent back button
                // issues
                viewModel.setupMenuCallback(activity)
                Log.d(TAG, "[ROTATION_FIX] Menu callbacks re-registered after rotation")

                // --- SOLUTION: Recreate fragments after orientation change ---
                Log.d(TAG, "[ORIENTATION] ====== CHECKING FOR MENU AFTER ROTATION ======")

                val menuManager = viewModel.getMenuManager()
                val currentState =
                        menuManager.getCurrentState() // CRITICAL: Check the TRUE backstack to
                // detect if we are in a submenu
                // currentState may be outdated after BACK operations
                val fragmentManager = activity.supportFragmentManager
                val hasBackStack = fragmentManager.backStackEntryCount > 0
                val visibleFragment = fragmentManager.findFragmentById(R.id.menu_container)

                Log.d(TAG, "[ORIENTATION] Estado do menu: $currentState")
                Log.d(TAG, "[ORIENTATION] Backstack count: ${fragmentManager.backStackEntryCount}")
                Log.d(TAG, "[ORIENTATION] Visible fragment: ${visibleFragment?.javaClass?.simpleName ?: "none"}")

                // CRITICAL: Only recreate fragments if menu is actually open
                if (visibleFragment == null || visibleFragment !is MenuFragment) {
                        Log.d(TAG, "[ORIENTATION] ⏭️ No menu fragment visible, skipping recreation")
                        Log.d(TAG, "[ORIENTATION] ====== ORIENTATION CHECK COMPLETED ======")
                        return
                }

                Log.d(TAG, "[ORIENTATION] ✅ Menu fragment found, proceeding with recreation")

                // Wait for system to complete rotation
                menuRotationRecreator.scheduleMenuRecreationAfterRotation(
                        visibleFragment,
                        hasBackStack,
                        currentState
                )
        }
}
