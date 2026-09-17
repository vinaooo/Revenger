package com.vinaooo.revenger.utils

import android.util.Log

/**
 * Conditional debug logging gated by [setDebugEnabled], defaulting from `BuildConfig.DEBUG` when
 * reachable via reflection. Split out of [MenuLogger] alongside [LevelLogger] for the same
 * function-count reason; exposed back on it via Kotlin interface delegation (`by`) for API
 * compatibility -- [MenuLogger]'s tagged convenience methods (`lifecycle`/`action`/`state`/etc.)
 * all funnel through [d].
 */
interface DebugLogging {
    /** Enable or disable debug logging */
    fun setDebugEnabled(enabled: Boolean)

    /** Conditional debug log */
    fun d(message: String)

    /** Conditional debug log with throwable */
    fun d(message: String, throwable: Throwable)
}

class DebugGate(private val tag: String) : DebugLogging {

    // Flag to control logs - uses BuildConfig.DEBUG when available
    private var isDebugEnabled: Boolean =
            try {
                // Try to access BuildConfig.DEBUG if available
                Class.forName("com.vinaooo.revenger.BuildConfig").getField("DEBUG").getBoolean(null)
                // Class.forName/getField/getBoolean's checked failures (ClassNotFoundException,
                // NoSuchFieldException, IllegalAccessException) all share this common ancestor,
                // which isn't on detekt's generic-exception list.
            } catch (e: ReflectiveOperationException) {
                Log.w(tag, "BuildConfig.DEBUG not accessible via reflection, defaulting to true", e)
                // Fallback to true if BuildConfig is not available
                true
            }

    override fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
        Log.i(tag, "[LOGGER] Debug logging ${if (enabled) "enabled" else "disabled"}")
    }

    override fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }

    override fun d(message: String, throwable: Throwable) {
        if (isDebugEnabled) {
            Log.d(tag, message, throwable)
        }
    }
}
