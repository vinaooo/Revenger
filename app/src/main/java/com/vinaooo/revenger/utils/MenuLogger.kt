package com.vinaooo.revenger.utils

import android.util.Log

/**
 * Utility for conditional logging in the RetroMenu3 menu system. Allows controlling
 * production logs via a debug flag.
 */
object MenuLogger {

    private const val TAG = "RetroMenu3"

    // Flag to control logs - uses BuildConfig.DEBUG when available
    private var isDebugEnabled: Boolean =
            try {
                // Try to access BuildConfig.DEBUG if available
                Class.forName("com.vinaooo.revenger.BuildConfig").getField("DEBUG").getBoolean(null)
            } catch (e: Exception) {
                // Fallback to true if BuildConfig is not available
                true
            }

    /** Enable or disable debug logging */
    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
        Log.i(TAG, "[LOGGER] Debug logging ${if (enabled) "enabled" else "disabled"}")
    }

    /** Conditional debug log */
    fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(TAG, message)
        }
    }

    /** Conditional debug log with throwable */
    fun d(message: String, throwable: Throwable) {
        if (isDebugEnabled) {
            Log.d(TAG, message, throwable)
        }
    }

    /** Info log (always active) */
    fun i(message: String) {
        Log.i(TAG, message)
    }

    /** Info log with throwable (always active) */
    fun i(message: String, throwable: Throwable) {
        Log.i(TAG, message, throwable)
    }

    /** Warning log (always active) */
    fun w(message: String) {
        Log.w(TAG, message)
    }

    /** Warning log with throwable (always active) */
    fun w(message: String, throwable: Throwable) {
        Log.w(TAG, message, throwable)
    }

    /** Error log (always active) */
    fun e(message: String) {
        Log.e(TAG, message)
    }

    /** Error log with throwable (always active) */
    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    /** Log specifically for lifecycle events */
    fun lifecycle(message: String) {
        d("[LIFECYCLE] $message")
    }

    /** Log specifically for navigation */
    fun navigation(message: String) {
        d("[NAV] $message")
    }

    /** Log specifically for actions */
    fun action(message: String) {
        d("[ACTION] $message")
    }

    /** Log specifically for animations/dismiss */
    fun animation(message: String) {
        d("[ANIMATION] $message")
    }

    /** Log specifically for menu state */
    fun state(message: String) {
        d("[STATE] $message")
    }

    /** Log specifically for performance */
    fun performance(message: String) {
        d("[PERFORMANCE] $message")
    }
}
