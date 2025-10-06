package com.vinaooo.revenger.utils

import android.os.Build

/**
 * Android Version Compatibility Manager Phase 9.4: Target SDK 36 Features with backward
 * compatibility
 */
object AndroidCompatibility {

    /** Check if running on Android 12+ (API 31) Features: Material You, Dynamic Colors, etc. */
    fun isAndroid12Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Check if running on Android 13+ (API 33) Features: Themed app icons, improved notifications
     */
    fun isAndroid13Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Check if running on Android 14+ (API 34) Features: Enhanced security, partial photo
     * permissions
     */
    fun isAndroid14Plus(): Boolean = Build.VERSION.SDK_INT >= 34

    /**
     * Check if running on Android 15+ (API 35) Features: Enhanced privacy controls, improved
     * performance
     */
    fun isAndroid15Plus(): Boolean = Build.VERSION.SDK_INT >= 35

    /**
     * Check if running on Android 16+ (API 36) - Target SDK Features: Latest SDK 36 specific
     * features
     */
    fun isAndroid16Plus(): Boolean = Build.VERSION.SDK_INT >= 36

    /** Apply features conditionally based on Android version */
    fun applyConditionalFeatures() {
        when {
            isAndroid16Plus() -> {
                // Enable latest SDK 36 features
            }
            isAndroid15Plus() -> {
                // Enable Android 15 features
            }
            isAndroid14Plus() -> {
                // Enable Android 14 features
            }
            isAndroid13Plus() -> {
                // Enable Android 13 features
            }
            isAndroid12Plus() -> {
                // Enable Android 12 features
            }
            else -> {
                // Base functionality - guaranteed to work
            }
        }
    }
}
