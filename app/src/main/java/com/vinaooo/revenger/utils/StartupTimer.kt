package com.vinaooo.revenger.utils

import android.util.Log

/** Logs how long each `GameActivity.onCreate` step took since [startTimeMs], at debug level. */
class StartupTimer(
    private val startTimeMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    companion object {
        const val TAG = "STARTUP_TIMING"
    }

    /** Logs and returns `⏱️ [T+<elapsed>ms] <step>`. */
    fun mark(step: String): String {
        val message = "⏱️ [T+${clock() - startTimeMs}ms] $step"
        Log.d(TAG, message)
        return message
    }
}
