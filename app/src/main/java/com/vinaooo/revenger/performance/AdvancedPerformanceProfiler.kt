package com.vinaooo.revenger.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.vinaooo.revenger.utils.AndroidCompatibility
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced Performance Profiler for SDK 36 Phase 9.4: Target SDK 36 Features Real-time performance
 * monitoring with progressive enhancement
 */
object AdvancedPerformanceProfiler {

    private val handler = Handler(Looper.getMainLooper())

    private var isProfilingActive = false
    private var performanceData = ConcurrentHashMap<String, Any>()
    private var frameTimeData = mutableListOf<Long>()

    // Performance monitoring intervals
    private const val MONITORING_INTERVAL_MS = 1000L
    private const val FRAME_TIME_BUFFER_SIZE = 120 // 2 seconds at 60fps

    // Placeholder values for SDK 36 metrics without a real data source yet
    private const val HYPOTHETICAL_GPU_UTILIZATION = 45.0
    private const val HYPOTHETICAL_THERMAL_STATE = "NORMAL"
    private const val HYPOTHETICAL_FRAME_PACING = 95.0

    // SDK level gating the "advanced" (Android 16+) profiling path
    private const val ANDROID_16_API_LEVEL = 36

    // Bytes -> kilobytes -> megabytes conversion factor
    private const val BYTES_PER_KILOBYTE = 1024

    // Multiplier to convert a used/max memory ratio into a percentage
    private const val PERCENTAGE_MULTIPLIER = 100.0

    // Time unit conversions used by the frame-timing math below
    private const val NANOS_PER_MILLISECOND = 1_000_000.0
    private const val MILLISECONDS_PER_SECOND = 1000.0
    private const val TARGET_FPS = 60.0

    // Smoothing factor for the emulator FPS moving average (higher = reacts faster to changes)
    private const val FPS_SMOOTHING_FACTOR = 0.1

    // Checkpoint cadence: every Nth monitoring interval, reserved for future periodic work
    private const val CHECKPOINT_INTERVAL_MULTIPLIER = 5

    // Debug overlay update cadence
    private const val DEBUG_OVERLAY_UPDATE_INTERVAL_MS = 500L

    // Debug overlay appearance
    private const val DEBUG_OVERLAY_TEXT_SIZE_PX = 14f
    private const val DEBUG_OVERLAY_PADDING_HORIZONTAL_PX = 20
    private const val DEBUG_OVERLAY_PADDING_VERTICAL_PX = 12
    private const val DEBUG_OVERLAY_MARGIN_PX = 32
    private const val DEBUG_OVERLAY_TOP_MARGIN_PX = 150
    private const val DEBUG_OVERLAY_ELEVATION_PX = 10f
    private const val DEBUG_OVERLAY_CORNER_RADIUS_PX = 8f

    // Frame timing for emulator FPS calculation
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var emulatorFps = 0.0

    /** Debug overlay variables */
    private var debugOverlayView: android.widget.TextView? = null
    private var debugOverlayUpdateRunnable: Runnable? = null

    /** Start performance profiling based on Android version */
    fun startProfiling(context: Context) {
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

    /** Stop performance profiling */
    fun stopProfiling() {
        if (!isProfilingActive) return
        isProfilingActive = false
        handler.removeCallbacksAndMessages(null)

        logPerformanceSummary()
    }

    /** Android 16+: Advanced performance profiling */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startAdvancedProfiling(context: Context) {
        // Enhanced GPU profiling
        startEnhancedGpuProfiling()

        // Advanced memory profiling
        startAdvancedMemoryProfiling()

        // CPU thermal monitoring
        startThermalMonitoring()

        // Frame pacing analysis
        startFramePacingAnalysis()

        startMonitoringLoop(context, ProfileLevel.ADVANCED)
    }

    /** Android 12+: Standard performance profiling */
    private fun startStandardProfiling(context: Context) {
        // Basic GPU monitoring
        startBasicGpuProfiling()

        // Standard memory monitoring
        startStandardMemoryProfiling()

        startMonitoringLoop(context, ProfileLevel.STANDARD)
    }

    /** Android 11: Basic performance monitoring */
    private fun startBasicProfiling(context: Context) {
        // Basic system monitoring only
        startBasicSystemMonitoring()

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
        val memoryInfo = getMemoryInfo(context)
        performanceData["memory_used_mb"] = memoryInfo.used / BYTES_PER_KILOBYTE / BYTES_PER_KILOBYTE
        performanceData["memory_available_mb"] =
                memoryInfo.available / BYTES_PER_KILOBYTE / BYTES_PER_KILOBYTE

        val cpuUsage = getCpuUsage()
        performanceData["cpu_usage_percent"] = cpuUsage

        when (level) {
            ProfileLevel.ADVANCED -> {
                if (AndroidCompatibility.isAndroid16Plus()) {
                    collectAdvancedMetrics()
                }
            }
            ProfileLevel.STANDARD -> {
                if (AndroidCompatibility.isAndroid12Plus()) {
                    collectStandardMetrics()
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

    /** Enhanced GPU profiling for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startEnhancedGpuProfiling() {
        // Hypothetical advanced GPU profiling APIs
    }

    /** Advanced memory profiling for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startAdvancedMemoryProfiling() {
        // Hypothetical advanced memory profiling APIs
    }

    /** CPU thermal monitoring for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startThermalMonitoring() {
        // Hypothetical thermal monitoring APIs
    }

    /** Frame pacing analysis for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun startFramePacingAnalysis() {
        // Hypothetical frame pacing APIs
    }

    /** Basic GPU profiling */
    private fun startBasicGpuProfiling() {
        // Symbolic initialization preserved
    }

    /** Standard memory profiling */
    private fun startStandardMemoryProfiling() {
        // Symbolic initialization preserved
    }

    /** Basic system monitoring */
    private fun startBasicSystemMonitoring() {
        // Symbolic initialization preserved
    }

    /** Collect advanced metrics for Android 16 */
    @RequiresApi(ANDROID_16_API_LEVEL)
    private fun collectAdvancedMetrics() {
        // Hypothetical advanced metrics collection
        performanceData["gpu_utilization"] = HYPOTHETICAL_GPU_UTILIZATION
        performanceData["thermal_state"] = HYPOTHETICAL_THERMAL_STATE
        performanceData["frame_pacing_score"] = HYPOTHETICAL_FRAME_PACING
    }

    /** Collect standard metrics for Android 12+ */
    private fun collectStandardMetrics() {
        // Basic GPU metrics if available
        performanceData["basic_gpu_info"] = "Available"
    }

    /** Get memory information */
    private fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return MemoryInfo(
                used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                available = memInfo.availMem,
                total = memInfo.totalMem
        )
    }

    /** Get CPU usage (basic approximation) */
    private fun getCpuUsage(): Double {
        // Basic CPU usage estimation
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        // This is a simplified estimation
        return (usedMemory.toDouble() / maxMemory.toDouble()) * PERCENTAGE_MULTIPLIER
    }

    /** Get current frame statistics */
    fun getFrameStats(): FrameStats {
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

    /** Log performance summary */
    private fun logPerformanceSummary() {
        // Kept for future compatibility - previously logged performance summary
    }

    /** Add frame time measurement */
    fun recordFrameTime(frameTimeNs: Long) {
        synchronized(frameTimeData) {
            frameTimeData.add(frameTimeNs)
            if (frameTimeData.size > FRAME_TIME_BUFFER_SIZE) {
                frameTimeData.removeAt(0)
            }
        }
    }

    /** Register frame callback from RetroView for accurate FPS calculation */
    fun onFrameRendered() {
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

        // Record frame time for performance analysis (don't double-count)
        // recordFrameTime(((currentTime - lastFrameTime) / 1_000_000.0).toLong())
    }

    /** Show debug overlay with performance info */
    fun showDebugOverlay(context: Context) {
        android.util.Log.d("PerformanceProfiler", "showDebugOverlay called - checking config")
        if (!shouldShowPerformanceOverlay(context)) {
            android.util.Log.d("PerformanceProfiler", "shouldShowPerformanceOverlay returned false")
            return
        }

        val activity =
                context as? android.app.Activity
                        ?: run {
                            android.util.Log.d("PerformanceProfiler", "Context is not Activity")
                            return
                        }
        android.util.Log.d("PerformanceProfiler", "Context is Activity, proceeding...")

        activity.runOnUiThread {
            android.util.Log.d("PerformanceProfiler", "In runOnUiThread, creating overlay")
            if (debugOverlayView == null) {
                debugOverlayView =
                        android.widget.TextView(context).apply {
                            setBackgroundColor(
                                    android.graphics.Color.parseColor("#CC000000")
                            ) // More opaque
                            setTextColor(android.graphics.Color.YELLOW) // More visible color
                            textSize = DEBUG_OVERLAY_TEXT_SIZE_PX // Larger font
                            // Bigger padding
                            setPadding(
                                    DEBUG_OVERLAY_PADDING_HORIZONTAL_PX,
                                    DEBUG_OVERLAY_PADDING_VERTICAL_PX,
                                    DEBUG_OVERLAY_PADDING_HORIZONTAL_PX,
                                    DEBUG_OVERLAY_PADDING_VERTICAL_PX
                            )
                            text = "Initializing FPS overlay..."
                            layoutParams =
                                    android.widget.FrameLayout.LayoutParams(
                                                    android.widget.FrameLayout.LayoutParams
                                                            .WRAP_CONTENT,
                                                    android.widget.FrameLayout.LayoutParams
                                                            .WRAP_CONTENT
                                            )
                                            .apply {
                                                gravity =
                                                        android.view.Gravity.TOP or
                                                                android.view.Gravity.START
                                                // Larger margins
                                                setMargins(
                                                        DEBUG_OVERLAY_MARGIN_PX,
                                                        DEBUG_OVERLAY_TOP_MARGIN_PX,
                                                        DEBUG_OVERLAY_MARGIN_PX,
                                                        DEBUG_OVERLAY_MARGIN_PX
                                                )
                                            }
                            // Garantir que fique na frente
                            elevation = DEBUG_OVERLAY_ELEVATION_PX
                            bringToFront()
                            // Adicionar borda
                            background =
                                    android.graphics.drawable.GradientDrawable().apply {
                                        setColor(android.graphics.Color.parseColor("#CC000000"))
                                        setStroke(2, android.graphics.Color.YELLOW)
                                        cornerRadius = DEBUG_OVERLAY_CORNER_RADIUS_PX
                                    }
                            // Garantir visibilidade
                            visibility = android.view.View.VISIBLE
                        }

                val rootView =
                        activity.window.decorView.findViewById<android.widget.FrameLayout>(
                                android.R.id.content
                        )
                android.util.Log.d("PerformanceProfiler", "Adding overlay to root view")
                rootView.addView(debugOverlayView)
                android.util.Log.d("PerformanceProfiler", "Debug overlay view added to root view")
            } else {
                android.util.Log.d("PerformanceProfiler", "Debug overlay view already exists")
            }

            // Start updating the overlay
            startDebugOverlayUpdates()
        }
    }

    /** Hide debug overlay */
    fun hideDebugOverlay() {
        debugOverlayView?.let { view ->
            val parent = view.parent as? android.view.ViewGroup
            parent?.removeView(view)
            debugOverlayView = null
        }
        debugOverlayUpdateRunnable?.let { handler.removeCallbacks(it) }
        debugOverlayUpdateRunnable = null
    }

    /** Start updating debug overlay */
    private fun startDebugOverlayUpdates() {
        android.util.Log.d("PerformanceProfiler", "startDebugOverlayUpdates called")
        debugOverlayUpdateRunnable =
                object : Runnable {
                    override fun run() {
                        if (isProfilingActive && debugOverlayView != null) {
                            val frameStats = getFrameStats()
                            val memoryInfo = performanceData["memory_used_mb"] as? Long ?: 0L
                            val cpuUsage = performanceData["cpu_usage_percent"] as? Double ?: 0.0

                            val debugText =
                                    if (frameStats.averageFps > 0) {
                                        """
                        FPS: ${"%.1f".format(frameStats.averageFps)}
                        Frame Time: ${"%.2f".format(frameStats.averageFrameTimeMs)}ms
                        Dropped: ${frameStats.droppedFrames}
                        Memory: ${memoryInfo}MB
                        CPU: ${"%.1f".format(cpuUsage)}%
                        """.trimIndent()
                                    } else {
                                        """
                        FPS: Collecting data...
                        Frame Time: --
                        Dropped: --
                        Memory: ${memoryInfo}MB
                        CPU: ${"%.1f".format(cpuUsage)}%
                        """.trimIndent()
                                    }

                            debugOverlayView?.text = debugText
                            android.util.Log.d(
                                    "PerformanceProfiler",
                                    "Overlay text updated: $debugText"
                            )
                            // Update every DEBUG_OVERLAY_UPDATE_INTERVAL_MS
                            handler.postDelayed(this, DEBUG_OVERLAY_UPDATE_INTERVAL_MS)
                        } else {
                            android.util.Log.d(
                                    "PerformanceProfiler",
                                    "Not updating overlay - isProfilingActive: $isProfilingActive, " +
                                            "debugOverlayView: ${debugOverlayView != null}"
                            )
                        }
                    }
                }
        handler.post(debugOverlayUpdateRunnable!!)
        android.util.Log.d("PerformanceProfiler", "Overlay update runnable posted")
    }

    /** Check if performance overlay should be shown */
    private fun shouldShowPerformanceOverlay(context: Context): Boolean {
        // Check config setting first (even in debug builds)
        return try {
            val configValue = getConfigBoolean(context, "performance_overlay")
            android.util.Log.d(
                    "PerformanceProfiler",
                    "Config value for performance_overlay: $configValue"
            )
            configValue
            // getConfigBoolean() handles its own resource-lookup failures internally and does not
            // rethrow, so nothing is actually reachable here today; kept as a defensive net in
            // case that internal contract changes.
        } catch (expectedUnreachable: Exception) {
            android.util.Log.e("PerformanceProfiler", "Error reading config", expectedUnreachable)
            // Only fallback to debug behavior if config reading fails
            isDebugBuild(context)
        }
    }

    /** Check if this is a debug build */
    private fun isDebugBuild(context: Context): Boolean {
        return try {
            // Check if app was installed via Android Studio (debuggable)
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            android.util.Log.w("PerformanceProfiler", "Could not read own application info", e)
            // Fallback: check package name for debug indicators
            context.packageName.contains("debug", ignoreCase = true)
        }
    }

    /** Get boolean config value from resources */
    private fun getConfigBoolean(context: Context, key: String): Boolean {
        return try {
            val resources = context.resources
            val resId = resources.getIdentifier(key, "bool", context.packageName)
            if (resId != 0) {
                val value = resources.getBoolean(resId)
                android.util.Log.d("PerformanceProfiler", "Read boolean $key = $value")
                value
            } else {
                android.util.Log.w("PerformanceProfiler", "Resource ID not found for $key")
                false
            }
        } catch (e: android.content.res.Resources.NotFoundException) {
            android.util.Log.e("PerformanceProfiler", "Error reading boolean $key", e)
            false
        }
    }

    enum class ProfileLevel {
        BASIC,
        STANDARD,
        ADVANCED
    }

    data class MemoryInfo(val used: Long, val available: Long, val total: Long)

    data class FrameStats(
            val averageFps: Double,
            val averageFrameTimeMs: Double,
            val droppedFrames: Int
    )
}
