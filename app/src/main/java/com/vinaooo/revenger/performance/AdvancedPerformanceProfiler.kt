package com.vinaooo.revenger.performance

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

// Collaborator state lives at file scope, constructed top-to-bottom in dependency order, so each
// collaborator receives the instances it needs directly through its constructor. This is safe
// here (unlike a delegate that needs to call back into the *object's own* not-yet-initialized
// members): none of these collaborators depend on AdvancedPerformanceProfiler itself, only on
// each other and on plain state (a Handler and a shared performance-data map) declared earlier in
// this same file.
private val performanceData = ConcurrentHashMap<String, Any>()
private val handler = Handler(Looper.getMainLooper())
private val hardwareMetricsCollector = HardwareMetricsCollector(performanceData)

// internal (not private): AdvancedPerformanceProfiler_test resets this collaborator's private
// fields via reflection between tests, following this codebase's convention for stateful
// singletons (see definitions/Code.md).
internal val profilingSessionController =
        ProfilingSessionController(handler, performanceData, hardwareMetricsCollector)
internal val frameStatsTracker = FrameStatsTracker()

private val debugOverlayController =
        DebugOverlayController(handler, performanceData, frameStatsTracker) {
            profilingSessionController.isActive()
        }

/**
 * Advanced Performance Profiler for SDK 36 Phase 9.4: Target SDK 36 Features. Real-time
 * performance monitoring with progressive enhancement.
 *
 * Split into focused collaborators, re-exposed on this object via Kotlin interface delegation
 * (`by`) purely to keep it under the project's function-count threshold:
 * - [ProfilingSessionController] -- session start/stop and the per-SDK-level monitoring loop.
 * - [HardwareMetricsCollector] -- the per-level (GPU/memory/thermal/frame-pacing) metric
 *   collection, used internally by the session controller. None of its methods were part of the
 *   original public API, so it is not re-exposed here.
 * - [FrameStatsTracker] -- frame-time bookkeeping and FPS calculation.
 * - [DebugOverlayController] -- the on-screen debug overlay.
 */
object AdvancedPerformanceProfiler :
        ProfilingSession by profilingSessionController,
        FrameStatsProvider by frameStatsTracker,
        DebugOverlay by debugOverlayController {

    // These three types were nested inside this object before the collaborator split, so
    // AdvancedPerformanceProfiler.ProfileLevel / .MemoryInfo / .FrameStats were part of its public
    // type surface. They're kept nested here (rather than made top-level in each collaborator's
    // own file) purely to preserve that surface -- the collaborators reference them qualified.
    // A nested (non-inner) type like this compiles to its own class file, and referencing it as a
    // type does not trigger this object's initialization, so nesting them back does not
    // reintroduce the circular-init problem the collaborator split otherwise had to avoid.

    /** Profiling depth dispatched based on the running Android version. */
    enum class ProfileLevel {
        BASIC,
        STANDARD,
        ADVANCED
    }

    /** Memory snapshot returned by [HardwareMetricsCollector.getMemoryInfo]. */
    data class MemoryInfo(val used: Long, val available: Long, val total: Long)

    /** Aggregated frame-timing statistics, see [FrameStatsProvider.getFrameStats]. */
    data class FrameStats(
            val averageFps: Double,
            val averageFrameTimeMs: Double,
            val droppedFrames: Int
    )
}
