package com.vinaooo.revenger.utils

import android.util.Log
import com.vinaooo.revenger.BuildConfig

/**
 * Conditional debug logging gated by [setDebugEnabled], on by default only in debug builds
 * (`BuildConfig.DEBUG`). Split out of [MenuLogger] alongside [LevelLogger] for the same
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

/**
 * @param tag the log tag
 * @param defaultEnabled whether [d] logs before any [setDebugEnabled] call; `BuildConfig.DEBUG`
 *   unless a test passes its own
 */
class DebugGate(
        private val tag: String,
        defaultEnabled: Boolean = BuildConfig.DEBUG
) : DebugLogging {

    private var isDebugEnabled: Boolean = defaultEnabled

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
