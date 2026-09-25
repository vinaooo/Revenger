package com.vinaooo.revenger.performance

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.vinaooo.revenger.utils.BuildTypeDetector
import java.util.concurrent.ConcurrentHashMap

/**
 * The on-screen performance debug overlay for [AdvancedPerformanceProfiler], re-exposed on it via
 * Kotlin interface delegation (`by`).
 */
interface DebugOverlay {
    /** Show debug overlay with performance info */
    fun showDebugOverlay(context: Context)

    /** Hide debug overlay */
    fun hideDebugOverlay()
}

/**
 * Builds, updates and tears down the on-screen FPS/frame-time/memory/CPU debug overlay, extracted
 * from [AdvancedPerformanceProfiler] purely to keep that object under the project's function-count
 * threshold (and to break up the overlay-construction logic that used to live in one long method).
 */
class DebugOverlayController(
        private val handler: Handler,
        private val performanceData: ConcurrentHashMap<String, Any>,
        private val frameStatsProvider: FrameStatsProvider,
        private val isProfilingActive: () -> Boolean
) : DebugOverlay {

    companion object {
        private const val TAG = "PerformanceProfiler"

        // Debug overlay update cadence
        private const val UPDATE_INTERVAL_MS = 500L

        // Debug overlay appearance
        private const val TEXT_SIZE_PX = 14f
        private const val PADDING_HORIZONTAL_PX = 20
        private const val PADDING_VERTICAL_PX = 12
        private const val MARGIN_PX = 32
        private const val TOP_MARGIN_PX = 150
        private const val ELEVATION_PX = 10f
        private const val CORNER_RADIUS_PX = 8f
        private const val BORDER_STROKE_PX = 2
        private const val BACKGROUND_COLOR = "#CC000000"
    }

    private var debugOverlayView: TextView? = null
    private var debugOverlayUpdateRunnable: Runnable? = null

    override fun showDebugOverlay(context: Context) {
        Log.d(TAG, "showDebugOverlay called - checking config")
        if (!shouldShowPerformanceOverlay(context)) {
            Log.d(TAG, "shouldShowPerformanceOverlay returned false")
            return
        }

        val activity =
                context as? Activity
                        ?: run {
                            Log.d(TAG, "Context is not Activity")
                            return
                        }
        Log.d(TAG, "Context is Activity, proceeding...")

        activity.runOnUiThread {
            Log.d(TAG, "In runOnUiThread, creating overlay")
            if (debugOverlayView == null) {
                val view = buildDebugOverlayView(context)
                debugOverlayView = view
                attachOverlayToRoot(activity, view)
            } else {
                Log.d(TAG, "Debug overlay view already exists")
            }
            startDebugOverlayUpdates()
        }
    }

    override fun hideDebugOverlay() {
        debugOverlayView?.let { view ->
            val parent = view.parent as? ViewGroup
            parent?.removeView(view)
            debugOverlayView = null
        }
        debugOverlayUpdateRunnable?.let { handler.removeCallbacks(it) }
        debugOverlayUpdateRunnable = null
    }

    /** Builds the styled overlay [TextView] (colors, padding, margins, border). */
    private fun buildDebugOverlayView(context: Context): TextView {
        return TextView(context).apply {
            setBackgroundColor(Color.parseColor(BACKGROUND_COLOR))
            setTextColor(Color.YELLOW)
            textSize = TEXT_SIZE_PX
            setPadding(
                    PADDING_HORIZONTAL_PX,
                    PADDING_VERTICAL_PX,
                    PADDING_HORIZONTAL_PX,
                    PADDING_VERTICAL_PX
            )
            text = "Initializing FPS overlay..."
            layoutParams =
                    FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                            .apply {
                                gravity = Gravity.TOP or Gravity.START
                                setMargins(MARGIN_PX, TOP_MARGIN_PX, MARGIN_PX, MARGIN_PX)
                            }
            elevation = ELEVATION_PX
            bringToFront()
            background =
                    GradientDrawable().apply {
                        setColor(Color.parseColor(BACKGROUND_COLOR))
                        setStroke(BORDER_STROKE_PX, Color.YELLOW)
                        cornerRadius = CORNER_RADIUS_PX
                    }
            visibility = View.VISIBLE
        }
    }

    /** Adds the overlay view on top of the activity's content root. */
    private fun attachOverlayToRoot(activity: Activity, view: TextView) {
        val rootView = activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)
        Log.d(TAG, "Adding overlay to root view")
        rootView.addView(view)
        Log.d(TAG, "Debug overlay view added to root view")
    }

    /** Start updating debug overlay */
    private fun startDebugOverlayUpdates() {
        Log.d(TAG, "startDebugOverlayUpdates called")
        val runnable =
                object : Runnable {
                    override fun run() {
                        val view = debugOverlayView
                        if (isProfilingActive() && view != null) {
                            val debugText = buildOverlayText()
                            view.text = debugText
                            Log.d(TAG, "Overlay text updated: $debugText")
                            // Update every UPDATE_INTERVAL_MS
                            handler.postDelayed(this, UPDATE_INTERVAL_MS)
                        } else {
                            Log.d(
                                    TAG,
                                    "Not updating overlay - isProfilingActive: " +
                                            "${isProfilingActive()}, debugOverlayView: " +
                                            "${view != null}"
                            )
                        }
                    }
                }
        debugOverlayUpdateRunnable = runnable
        handler.post(runnable)
        Log.d(TAG, "Overlay update runnable posted")
    }

    /** Formats the FPS/frame-time/memory/CPU text shown on the overlay. */
    private fun buildOverlayText(): String {
        val frameStats = frameStatsProvider.getFrameStats()
        val memoryInfo = performanceData["memory_used_mb"] as? Long ?: 0L
        val cpuUsage = performanceData["cpu_usage_percent"] as? Double ?: 0.0

        return if (frameStats.averageFps > 0) {
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
    }

    /** Check if performance overlay should be shown */
    private fun shouldShowPerformanceOverlay(context: Context): Boolean {
        // Check config setting first (even in debug builds)
        return try {
            val configValue = getConfigBoolean(context, "performance_overlay")
            Log.d(TAG, "Config value for performance_overlay: $configValue")
            configValue
            // getConfigBoolean() handles its own resource-lookup failures internally and does not
            // rethrow, so nothing is actually reachable here today; kept as a defensive net in
            // case that internal contract changes.
        } catch (expectedUnreachable: Exception) {
            Log.e(TAG, "Error reading config", expectedUnreachable)
            // Only fallback to debug behavior if config reading fails
            isDebugBuild(context)
        }
    }

    /** Check if this is a debug build */
    private fun isDebugBuild(context: Context): Boolean = BuildTypeDetector.isDebuggable(context)

    /** Get boolean config value from resources */
    private fun getConfigBoolean(context: Context, key: String): Boolean {
        return try {
            val resources = context.resources
            val resId = resources.getIdentifier(key, "bool", context.packageName)
            if (resId != 0) {
                resources.getBoolean(resId)
            } else {
                Log.w(TAG, "Resource ID not found for $key")
                false
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Error reading boolean $key", e)
            false
        }
    }
}
