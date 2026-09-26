package com.vinaooo.revenger.utils

/**
 * Time between consecutive input events, fed to the performance profiler. Holds the last
 * timestamp; a zero timestamp means "nothing recorded yet".
 */
class FrameTimeRecorder {

    private var lastNs = 0L

    /** Returns the time since the previous record (or [reset]), or null for the first one. */
    fun record(nowNs: Long): Long? {
        val delta = if (lastNs > 0) nowNs - lastNs else null
        lastNs = nowNs
        return delta
    }

    /** Restarts the measurement from [nowNs], e.g. in `onResume`. */
    fun reset(nowNs: Long) {
        lastNs = nowNs
    }
}
