package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log

/**
 * OrientationManager - Centralizes screen orientation application logic
 *
 * Purpose: Ensure that both SplashActivity and GameActivity apply the same
 * orientation logic based on conf_orientation from config.xml
 *
 * conf_orientation values:
 * - 1: Portrait only
 * - 2: Landscape only
 * - 3: Any orientation (respects system auto-rotate)
 */
object OrientationManager {
    private const val TAG = "OrientationManager"

    /**
     * Applies the configured screen orientation based on conf_orientation
     *
     * @param activity Activity where orientation should be applied
     * @param configOrientation Value of conf_orientation (1, 2, or 3)
     */
    fun applyConfigOrientation(activity: Activity, configOrientation: Int) {
        // Check system auto-rotate preference
        val accelerometerRotationEnabled = try {
            android.provider.Settings.System.getInt(
                activity.contentResolver,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (e: Exception) {
            Log.w(TAG, "Error reading auto-rotate setting", e)
            false
        }

        Log.d(
            TAG,
            "applyConfigOrientation: config=$configOrientation, autoRotate=$accelerometerRotationEnabled"
        )

        val orientation = when (configOrientation) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT // Sempre portrait
            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE // Sempre landscape
            3 -> {
                // If config is "any orientation", respect OS preference
                if (accelerometerRotationEnabled) {
                    // Auto-rotate enabled → allow free rotation based on sensors
                    ActivityInfo.SCREEN_ORIENTATION_USER
                } else {
                    // Auto-rotate disabled → delegate completely to the system
                    // UNSPECIFIED allows the system manual button to work
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            else -> {
                Log.w(TAG, "Invalid conf_orientation value: $configOrientation")
                return
            }
        }

        activity.requestedOrientation = orientation
        Log.d(TAG, "Orientation applied: $orientation")
    }

    /**
     * Forces the Configuration.orientation BEFORE setContentView()
     * This ensures Android selects the correct layout (layout/ vs layout-land/)
     * without an incorrect orientation flash
     *
     * @param activity Activity where the configuration should be applied
     * @param configOrientation Value of conf_orientation (1, 2, or 3)
     */
    fun forceConfigurationBeforeSetContent(activity: Activity, configOrientation: Int) {
        // For mode 3 (any orientation), do not force anything - let Android decide
        if (configOrientation == 3) {
            Log.d(TAG, "Mode 3 (any orientation) - skipping configuration force")
            return
        }

        // Determine the desired orientation
        val desiredOrientation = when (configOrientation) {
            1 -> Configuration.ORIENTATION_PORTRAIT
            2 -> Configuration.ORIENTATION_LANDSCAPE
            else -> {
                Log.w(TAG, "Invalid conf_orientation: $configOrientation")
                return
            }
        }

        // Create a configuration with only the properties to override
        val newConfig = Configuration()
        newConfig.orientation = desiredOrientation
        
        try {
            activity.applyOverrideConfiguration(newConfig)
            Log.d(TAG, "Configuration forced: orientation=$desiredOrientation (before setContentView)")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Cannot apply config override: getResources() already called", e)
        }
    }
}
