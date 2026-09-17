package com.vinaooo.revenger.performance

/** Aggregated frame-timing statistics, see [FrameStatsProvider.getFrameStats]. */
data class FrameStats(val averageFps: Double, val averageFrameTimeMs: Double, val droppedFrames: Int)

/**
 * Frame-time bookkeeping and FPS calculation for [AdvancedPerformanceProfiler], re-exposed on it
 * via Kotlin interface delegation (`by`).
 */
interface FrameStatsProvider {
    /** Get current frame statistics */
    fun getFrameStats(): FrameStats

    /** Add frame time measurement */
    fun recordFrameTime(frameTimeNs: Long)

    /** Register frame callback from RetroView for accurate FPS calculation */
    fun onFrameRendered()
}

/**
 * Owns the frame-time ring buffer and the emulator FPS moving average, extracted from
 * [AdvancedPerformanceProfiler] purely to keep that object under the project's function-count
 * threshold.
 */
class FrameStatsTracker : FrameStatsProvider {

    companion object {
        private const val FRAME_TIME_BUFFER_SIZE = 120 // 2 seconds at 60fps

        // Time unit conversions used by the frame-timing math below
        private const val NANOS_PER_MILLISECOND = 1_000_000.0
        private const val MILLISECONDS_PER_SECOND = 1000.0
        private const val TARGET_FPS = 60.0

        // Smoothing factor for the emulator FPS moving average (higher = reacts faster to changes)
        private const val FPS_SMOOTHING_FACTOR = 0.1
    }

    private var frameTimeData = mutableListOf<Long>()

    // Frame timing for emulator FPS calculation
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var emulatorFps = 0.0

    override fun getFrameStats(): FrameStats {
        synchronized(frameTimeData) {
            val frameTimesMs =
                    frameTimeData.map { it / NANOS_PER_MILLISECOND } // Convert to milliseconds
            val averageFrameTimeMs = if (frameTimesMs.isNotEmpty()) frameTimesMs.average() else 0.0

            // Use emulator FPS if available, otherwise calculate from frame times
            val fps =
                    if (emulatorFps > 0) {
                        emulatorFps
                    } else {
                        if (averageFrameTimeMs > 0) MILLISECONDS_PER_SECOND / averageFrameTimeMs
                        else 0.0
                    }

            // Count dropped frames (frames that took longer than 16.67ms for 60fps)
            val targetFrameTimeMs = MILLISECONDS_PER_SECOND / TARGET_FPS // 16.67ms for 60fps
            val droppedFrames = frameTimesMs.count { it > targetFrameTimeMs }

            return FrameStats(fps, averageFrameTimeMs, droppedFrames)
        }
    }

    override fun recordFrameTime(frameTimeNs: Long) {
        synchronized(frameTimeData) {
            frameTimeData.add(frameTimeNs)
            if (frameTimeData.size > FRAME_TIME_BUFFER_SIZE) {
                frameTimeData.removeAt(0)
            }
        }
    }

    override fun onFrameRendered() {
        val currentTime = System.nanoTime()
        frameCount++

        if (lastFrameTime > 0) {
            val frameTimeMs = (currentTime - lastFrameTime) / NANOS_PER_MILLISECOND
            // Calculate FPS based on recent frames (simple moving average)
            val alpha = FPS_SMOOTHING_FACTOR
            val instantFps = MILLISECONDS_PER_SECOND / frameTimeMs
            emulatorFps = emulatorFps * (1 - alpha) + instantFps * alpha
        }

        lastFrameTime = currentTime
    }
}
