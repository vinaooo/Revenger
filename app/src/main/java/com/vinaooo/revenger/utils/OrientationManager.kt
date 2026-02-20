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
     * Aplica a orientação de tela configurada baseada em conf_orientation
     *
     * @param activity Activity onde aplicar a orientação
     * @param configOrientation Valor de conf_orientation (1, 2, ou 3)
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
            Log.w(TAG, "Erro ao ler configuração de auto-rotate", e)
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
        Log.d(TAG, "Orientação aplicada: $orientation")
    }

    /**
     * Força a Configuration.orientation ANTES de setContentView()
     * Isso garante que o Android escolha o layout correto (layout/ vs layout-land/)
     * sem flash de orientação incorreta
     *
     * @param activity Activity onde aplicar a configuração
     * @param configOrientation Valor de conf_orientation (1, 2, ou 3)
     */
    @Suppress("DEPRECATION")
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

        // Get current configuration and create a modified copy
        val currentConfig = activity.resources.configuration
        
        // Check if it's already in the correct orientation
        if (currentConfig.orientation == desiredOrientation) {
            Log.d(TAG, "Configuration already correct: $desiredOrientation")
            return
        }

        // Force the orientation in the configuration
        // NOTE: This method is deprecated but it is the only way to ensure
        // that setContentView() chooses the correct layout immediately
        val newConfig = Configuration(currentConfig)
        newConfig.orientation = desiredOrientation
        
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(newConfig, activity.resources.displayMetrics)
        
        Log.d(TAG, "Configuration forced: orientation=$desiredOrientation (before setContentView)")
    }
}
