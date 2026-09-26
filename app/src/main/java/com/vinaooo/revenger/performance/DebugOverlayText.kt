package com.vinaooo.revenger.performance

import java.util.Locale

/**
 * The text of the performance debug overlay ([DebugOverlayController]). Pure, so the formatting
 * rules are unit-testable without a view.
 */
object DebugOverlayText {

    /** Keys [ProfilingSessionController] writes into the shared performance data map. */
    const val MEMORY_USED_MB_KEY = "memory_used_mb"
    const val CPU_USAGE_PERCENT_KEY = "cpu_usage_percent"

    /**
     * FPS, frame time, dropped frames, memory and CPU, one per line. Until any frame has been
     * measured ([AdvancedPerformanceProfiler.FrameStats.averageFps] is 0) the frame lines show
     * placeholders.
     */
    fun format(
            frameStats: AdvancedPerformanceProfiler.FrameStats,
            memoryUsedMb: Long,
            cpuUsagePercent: Double,
            locale: Locale = Locale.getDefault()
    ): String {
        val frameLines =
                if (frameStats.averageFps > 0) {
                    listOf(
                            "FPS: ${"%.1f".format(locale, frameStats.averageFps)}",
                            "Frame Time: ${"%.2f".format(locale, frameStats.averageFrameTimeMs)}ms",
                            "Dropped: ${frameStats.droppedFrames}"
                    )
                } else {
                    listOf("FPS: Collecting data...", "Frame Time: --", "Dropped: --")
                }
        return (frameLines + listOf("Memory: ${memoryUsedMb}MB", "CPU: ${"%.1f".format(locale, cpuUsagePercent)}%"))
                .joinToString("\n")
    }

    /**
     * [format] for the values in the profiler's shared data map; a missing or mistyped entry
     * reads as 0.
     */
    fun format(
            frameStats: AdvancedPerformanceProfiler.FrameStats,
            performanceData: Map<String, Any>,
            locale: Locale = Locale.getDefault()
    ): String =
            format(
                    frameStats,
                    performanceData[MEMORY_USED_MB_KEY] as? Long ?: 0L,
                    performanceData[CPU_USAGE_PERCENT_KEY] as? Double ?: 0.0,
                    locale
            )
}
