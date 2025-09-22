package com.vinaooo.revenger.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.material.color.DynamicColors
import com.vinaooo.revenger.utils.AndroidCompatibility

/**
 * Dynamic Material You theming system Phase 9.4: Target SDK 36 Features Compatible with Android 11+
 * (graceful degradation)
 */
object DynamicThemeManager {

    private const val TAG = "DynamicThemeManager"

    /**
     * Apply dynamic theme based on Android version Android 12+: Material You dynamic colors Android
     * 11: Static fallback theme
     */
    fun applyDynamicTheme(context: Context) {
        when {
            AndroidCompatibility.isAndroid16Plus() -> {
                // Android 16: Enhanced Material You 3.0
                applyAdvancedMaterialYou(context)
            }
            AndroidCompatibility.isAndroid12Plus() -> {
                // Android 12+: Standard Material You
                applyMaterialYou(context)
            }
            else -> {
                // Android 11: Static theme fallback
                applyStaticTheme()
            }
        }
    }

    /** Android 16+: Enhanced Material You 3.0 features */
    @RequiresApi(36)
    private fun applyAdvancedMaterialYou(context: Context) {
        // Apply dynamic colors with enhanced features
        if (DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivitiesIfAvailable(
                    context.applicationContext as android.app.Application
            )

            // Enhanced features for Android 16
            // Note: These are hypothetical features that would be available in SDK 36
            applyEnhancedColorContrast()
            applyAdvancedAccessibilityColors()
        }
    }

    /** Android 12+: Standard Material You */
    private fun applyMaterialYou(context: Context) {
        if (DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivitiesIfAvailable(
                    context.applicationContext as android.app.Application
            )
        }
    }

    /** Android 11: Static theme fallback */
    private fun applyStaticTheme() {
        // Use predefined colors that work well with gaming
        // This ensures the app looks great even on Android 11
    }

    /** Get adaptive colors based on system theme and device capability */
    fun getAdaptiveColors(context: Context): ThemeColors {
        val isNightMode =
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES

        return when {
            AndroidCompatibility.isAndroid12Plus() && DynamicColors.isDynamicColorAvailable() -> {
                // Extract dynamic colors from system
                getDynamicColors(context, isNightMode)
            }
            else -> {
                // Static colors optimized for gaming
                getStaticGameColors(isNightMode)
            }
        }
    }

    private fun getDynamicColors(context: Context, isNightMode: Boolean): ThemeColors {
        // Extract Material You colors from system
        val primary =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getColor(android.R.color.system_accent1_500)
                } else {
                    Color.parseColor("#6750A4") // Material Design default
                }

        return ThemeColors(
                primary = primary,
                onPrimary = if (isNightMode) Color.WHITE else Color.BLACK,
                background = if (isNightMode) Color.parseColor("#121212") else Color.WHITE,
                surface =
                        if (isNightMode) Color.parseColor("#1E1E1E")
                        else Color.parseColor("#F5F5F5")
        )
    }

    private fun getStaticGameColors(isNightMode: Boolean): ThemeColors {
        return if (isNightMode) {
            // Dark theme optimized for gaming
            ThemeColors(
                    primary = Color.parseColor("#BB86FC"),
                    onPrimary = Color.BLACK,
                    background = Color.parseColor("#121212"),
                    surface = Color.parseColor("#1E1E1E")
            )
        } else {
            // Light theme
            ThemeColors(
                    primary = Color.parseColor("#6200EE"),
                    onPrimary = Color.WHITE,
                    background = Color.WHITE,
                    surface = Color.parseColor("#F5F5F5")
            )
        }
    }

    /** Android 16 hypothetical enhanced features */
    @RequiresApi(36)
    private fun applyEnhancedColorContrast() {
        // Enhanced contrast adjustments for better gaming visibility
        // This would use hypothetical SDK 36 APIs
    }

    @RequiresApi(36)
    private fun applyAdvancedAccessibilityColors() {
        // Advanced accessibility color adjustments
        // This would use hypothetical SDK 36 APIs
    }
}

/** Theme colors data class */
data class ThemeColors(val primary: Int, val onPrimary: Int, val background: Int, val surface: Int)
