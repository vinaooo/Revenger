package com.vinaooo.revenger.performance

import android.content.Context
import android.os.Handler
import androidx.annotation.RequiresApi
import com.vinaooo.revenger.utils.AndroidCompatibility
import java.util.concurrent.ConcurrentHashMap

/** Profiling depth dispatched based on the running Android version. */
enum class ProfileLevel {
    BASIC,
    STANDARD,
    ADVANCED
}

/**
 * Session lifecycle (start/stop) for [AdvancedPerformanceProfiler], re-exposed on it via Kotlin
 * interface delegation (`by`).
 */
interface ProfilingSession {
    /** Start performance profiling based on Android version */
    fun startProfiling(context: Context)

    /** Stop performance profiling */
    fun stopProfiling()
}

/**
 * Owns the profiling session state (start/stop, active flag) and the per-SDK-level monitoring
 * loop, extracted from [AdvancedPerformanceProfiler] purely to keep that object under the
 * project's function-count threshold. The actual (SDK-gated) metric collection is delegated to
 * [hardwareMetrics].
 */
class ProfilingSessionController(
        private val handler: Handler,
        private val performanceData: ConcurrentHashMap<String, Any>,
        private val hardwareMetrics: HardwareMetricsCollector
) : ProfilingSession {

    companion object {
        // Performance monitoring intervals
        private const val MONITORING_INTERVAL_MS = 1000L

        // Bytes -> kilobytes -> megabytes conversion factor
        private const val BYTES_PER_KILOBYTE = 1024

        // Checkpoint cadence: every Nth monitoring interval, reserved for future periodic work
        private const val CHECKPOINT_INTERVAL_MULTIPLIER = 5
    }

    private var isProfilingActive = false

    /** Whether a profiling session is currently running. Used by the debug overlay. */
    fun isActive(): Boolean = isProfilingActive

    override fun startProfiling(context: Context) {
        if (isProfilingActive) return
        isProfilingActive = true

        when {
            AndroidCompatibility.isAndroid16Plus() -> {
                startAdvancedProfiling(context)
            }
            AndroidCompatibility.isAndroid12Plus() -> {
                startStandardProfiling(context)
            }
            else -> {
                startBasicProfiling(context)
            }
        }
    }

    override fun stopProfiling() {
        if (!isProfilingActive) return
        isProfilingActive = false
        handler.removeCallbacksAndMessages(null)

        logPerformanceSummary()
    }

    /** Android 16+: Advanced performance profiling */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startAdvancedProfiling(context: Context) {
        hardwareMetrics.startAdvancedProfilingStubs()

        startMonitoringLoop(context, ProfileLevel.ADVANCED)
    }

    /** Android 12+: Standard performance profiling */
    private fun startStandardProfiling(context: Context) {
        hardwareMetrics.startBasicGpuProfiling()
        hardwareMetrics.startStandardMemoryProfiling()

        startMonitoringLoop(context, ProfileLevel.STANDARD)
    }

    /** Android 11: Basic performance monitoring */
    private fun startBasicProfiling(context: Context) {
        hardwareMetrics.startBasicSystemMonitoring()

        startMonitoringLoop(context, ProfileLevel.BASIC)
    }

    /** Main monitoring loop */
    private fun startMonitoringLoop(context: Context, level: ProfileLevel) {
        val runnable =
                object : Runnable {
                    override fun run() {
                        if (isProfilingActive) {
                            collectPerformanceData(context, level)
                            handler.postDelayed(this, MONITORING_INTERVAL_MS)
                        }
                    }
                }
        handler.post(runnable)
    }

    /** Collect performance data based on profile level */
    private fun collectPerformanceData(context: Context, level: ProfileLevel) {
        val timestamp = System.currentTimeMillis()

        // Basic metrics available on all versions
        val memoryInfo = hardwareMetrics.getMemoryInfo(context)
        performanceData["memory_used_mb"] = memoryInfo.used / BYTES_PER_KILOBYTE / BYTES_PER_KILOBYTE
        performanceData["memory_available_mb"] =
                memoryInfo.available / BYTES_PER_KILOBYTE / BYTES_PER_KILOBYTE

        val cpuUsage = hardwareMetrics.getCpuUsage()
        performanceData["cpu_usage_percent"] = cpuUsage

        when (level) {
            ProfileLevel.ADVANCED -> {
                if (AndroidCompatibility.isAndroid16Plus()) {
                    hardwareMetrics.collectAdvancedMetrics()
                }
            }
            ProfileLevel.STANDARD -> {
                if (AndroidCompatibility.isAndroid12Plus()) {
                    hardwareMetrics.collectStandardMetrics()
                }
            }
            ProfileLevel.BASIC -> {
                // Only basic metrics already collected
            }
        }

        performanceData["timestamp"] = timestamp

        if (timestamp % (MONITORING_INTERVAL_MS * CHECKPOINT_INTERVAL_MULTIPLIER) == 0L) {
            // Checkpoint maintained for future monitoring integrations without logs
        }
    }

    /** Log performance summary */
    private fun logPerformanceSummary() {
        // Kept for future compatibility - previously logged performance summary
    }
}
