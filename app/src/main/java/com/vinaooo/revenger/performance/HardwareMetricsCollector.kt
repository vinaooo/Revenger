package com.vinaooo.revenger.performance

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.RequiresApi
import java.util.concurrent.ConcurrentHashMap

/** SDK level gating the "advanced" (Android 16+) profiling path. */
internal const val ANDROID_16_API_LEVEL = 36

/**
 * Per-SDK-level hardware metric collection (GPU/memory/thermal/frame-pacing stubs, plus the basic
 * memory/CPU sampling used on every level), extracted from [AdvancedPerformanceProfiler] purely to
 * keep that object under the project's function-count threshold.
 *
 * None of this class's methods were part of [AdvancedPerformanceProfiler]'s public API (they were
 * all `private fun`s there), so this collaborator is used internally by
 * [ProfilingSessionController] and is not re-exposed via interface delegation on the object.
 */
class HardwareMetricsCollector(private val performanceData: ConcurrentHashMap<String, Any>) {

    companion object {
        // Placeholder values for SDK 36 metrics without a real data source yet
        private const val HYPOTHETICAL_GPU_UTILIZATION = 45.0
        private const val HYPOTHETICAL_THERMAL_STATE = "NORMAL"
        private const val HYPOTHETICAL_FRAME_PACING = 95.0

        // Multiplier to convert a used/max memory ratio into a percentage
        private const val PERCENTAGE_MULTIPLIER = 100.0
    }

    /**
     * Enhanced GPU profiling, advanced memory profiling, CPU thermal monitoring and frame pacing
     * analysis for Android 16 -- kept as a single no-op entry point (called together, from the
     * same Android 16+ start-up path) until real profiling APIs exist for each of these.
     */
    @RequiresApi(ANDROID_16_API_LEVEL)
    fun startAdvancedProfilingStubs() {
        // Hypothetical advanced GPU profiling APIs
        // Hypothetical advanced memory profiling APIs
        // Hypothetical thermal monitoring APIs
        // Hypothetical frame pacing APIs
    }

    /** Basic GPU profiling */
    fun startBasicGpuProfiling() {
        // Symbolic initialization preserved
    }

    /** Standard memory profiling */
    fun startStandardMemoryProfiling() {
        // Symbolic initialization preserved
    }

    /** Basic system monitoring */
    fun startBasicSystemMonitoring() {
        // Symbolic initialization preserved
    }

    /** Collect advanced metrics for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    fun collectAdvancedMetrics() {
        // Hypothetical advanced metrics collection
        performanceData["gpu_utilization"] = HYPOTHETICAL_GPU_UTILIZATION
        performanceData["thermal_state"] = HYPOTHETICAL_THERMAL_STATE
        performanceData["frame_pacing_score"] = HYPOTHETICAL_FRAME_PACING
    }

    /** Collect standard metrics for Android 12+ */
    fun collectStandardMetrics() {
        // Basic GPU metrics if available
        performanceData["basic_gpu_info"] = "Available"
    }

    /** Get memory information */
    fun getMemoryInfo(context: Context): AdvancedPerformanceProfiler.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return AdvancedPerformanceProfiler.MemoryInfo(
                used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                available = memInfo.availMem,
                total = memInfo.totalMem
        )
    }

    /** Get CPU usage (basic approximation) */
    fun getCpuUsage(): Double {
        // Basic CPU usage estimation
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        // This is a simplified estimation
        return (usedMemory.toDouble() / maxMemory.toDouble()) * PERCENTAGE_MULTIPLIER
    }
}
