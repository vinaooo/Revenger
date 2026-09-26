package com.vinaooo.revenger.performance

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.vinaooo.revenger.RevengerApplication
import java.lang.ref.WeakReference
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
 * The overlay text is formatted by [DebugOverlayText].
 *
 * @param isOverlayEnabled whether the overlay may be shown at all: the `performance_overlay`
 *   config key by default.
 */
class DebugOverlayController(
        private val handler: Handler,
        private val performanceData: ConcurrentHashMap<String, Any>,
        private val frameStatsProvider: FrameStatsProvider,
        private val isOverlayEnabled: () -> Boolean = ::isPerformanceOverlayConfigured,
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

    // Held weakly: this controller lives in the process-wide AdvancedPerformanceProfiler object,
    // while the overlay TextView belongs to an Activity's view tree, which keeps it alive for as
    // long as it is attached (lint StaticFieldLeak).
    private var debugOverlayViewRef: WeakReference<TextView>? = null
    private var debugOverlayView: TextView?
        get() = debugOverlayViewRef?.get()
        set(value) {
            debugOverlayViewRef = value?.let(::WeakReference)
        }
    private var debugOverlayUpdateRunnable: Runnable? = null

    override fun showDebugOverlay(context: Context) {
        if (!isOverlayEnabled()) {
            Log.d(TAG, "performance_overlay is off, not showing the overlay")
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
                            val debugText =
                                    DebugOverlayText.format(frameStatsProvider.getFrameStats(), performanceData)
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
}

/**
 * The `performance_overlay` config key, read through [RevengerApplication.appConfig] like every
 * other setting. False while the config isn't initialized yet.
 */
internal fun isPerformanceOverlayConfigured(): Boolean =
        try {
            RevengerApplication.appConfig.getPerformanceOverlay()
            // appConfig is a lateinit var set in Application.onCreate().
        } catch (e: UninitializedPropertyAccessException) {
            Log.w("PerformanceProfiler", "AppConfig not initialized, overlay off", e)
            false
        }
