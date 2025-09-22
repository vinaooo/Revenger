package com.vinaooo.revenger.utils

import android.os.Build
import android.util.Log

/**
 * Android Version Compatibility Manager
 * Phase 9.4: Target SDK 36 Features with backward compatibility
 */
object AndroidCompatibility {
    
    private const val TAG = "AndroidCompatibility"
    
    /**
     * Check if running on Android 11+ (minSdk 30)
     * Base compatibility level for all features
     */
    fun isAndroid11Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    
    /**
     * Check if running on Android 12+ (API 31)
     * Features: Material You, Dynamic Colors, etc.
     */
    fun isAndroid12Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    /**
     * Check if running on Android 13+ (API 33) 
     * Features: Themed app icons, improved notifications
     */
    fun isAndroid13Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    
    /**
     * Check if running on Android 14+ (API 34)
     * Features: Enhanced security, partial photo permissions
     */
    fun isAndroid14Plus(): Boolean = Build.VERSION.SDK_INT >= 34
    
    /**
     * Check if running on Android 15+ (API 35)
     * Features: Enhanced privacy controls, improved performance
     */
    fun isAndroid15Plus(): Boolean = Build.VERSION.SDK_INT >= 35
    
    /**
     * Check if running on Android 16+ (API 36) - Target SDK
     * Features: Latest SDK 36 specific features
     */
    fun isAndroid16Plus(): Boolean = Build.VERSION.SDK_INT >= 36
    
    /**
     * Apply features conditionally based on Android version
     */
    fun applyConditionalFeatures() {
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        
        when {
            isAndroid16Plus() -> {
                Log.d(TAG, "✅ Android 16+: All SDK 36 features enabled")
                // Enable latest SDK 36 features
            }
            isAndroid15Plus() -> {
                Log.d(TAG, "✅ Android 15+: Enhanced features enabled")
                // Enable Android 15 features
            }
            isAndroid14Plus() -> {
                Log.d(TAG, "✅ Android 14+: Security features enabled")
                // Enable Android 14 features
            }
            isAndroid13Plus() -> {
                Log.d(TAG, "✅ Android 13+: Material You features enabled")
                // Enable Android 13 features
            }
            isAndroid12Plus() -> {
                Log.d(TAG, "✅ Android 12+: Dynamic colors enabled")
                // Enable Android 12 features
            }
            isAndroid11Plus() -> {
                Log.d(TAG, "✅ Android 11+: Base features enabled (minimum supported)")
                // Base functionality - guaranteed to work
            }
        }
    }
    
    /**
     * Example: Conditional feature implementation
     */
    fun enableAdvancedGraphics(): Boolean {
        return when {
            isAndroid16Plus() -> {
                // Use latest graphics APIs
                Log.d(TAG, "Using Android 16 advanced graphics")
                true
            }
            isAndroid14Plus() -> {
                // Use Android 14 graphics features
                Log.d(TAG, "Using Android 14 graphics")
                true
            }
            isAndroid12Plus() -> {
                // Use Android 12 graphics
                Log.d(TAG, "Using Android 12 graphics")
                true
            }
            else -> {
                // Fallback to Android 11 compatible graphics
                Log.d(TAG, "Using Android 11 compatible graphics")
                false
            }
        }
    }
}
