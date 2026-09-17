package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.swordfish.libretrodroid.GLRetroView

/**
 * Dedicated full-screen frame cache kept fresh for the Picture-in-Picture overlay, split out of
 * [ScreenshotCaptureUtil] purely to keep that object under the project's function-count
 * threshold. Exposed back on it via Kotlin interface delegation (`by`) since these are called
 * from `GameActivity`/`GameActivityViewModel`.
 *
 * The GL surface goes black during a PiP transition (libretrodroid pauses its GL thread on
 * ON_PAUSE and does not redraw the recreated surface), so the PiP window relies entirely on this
 * captured bitmap. Its lifetime is independent of [ScreenshotCaptureUtil]'s menu screenshot cache
 * -- it is only released via [clearPipFrame] (`GameActivity.onDestroy`).
 */
interface PipFrameCache {
    /**
     * Capture a fresh full-screen frame for the Picture-in-Picture overlay.
     *
     * Uses only the full-screen capture (no per-pixel border scan, safe to call on input events).
     * Throttled unless [force] is set (used right before entering PiP, when a stale-by-800ms
     * frame is not good enough).
     *
     * @param glRetroView The GLRetroView to capture from
     * @param force Bypass the throttle
     */
    fun capturePipFrame(glRetroView: GLRetroView, force: Boolean)

    /**
     * Replace the cached PiP frame. The previous bitmap is dropped for GC rather than recycled --
     * the PiP overlay ImageView may still be drawing it (PiP-exit ordering between
     * onPictureInPictureModeChanged, onResume and the first touch is not guaranteed), and
     * recycling a displayed bitmap crashes the next draw pass.
     */
    fun updatePipFrame(bitmap: Bitmap?)

    /** Get the cached PiP frame (full surface, with black bars). */
    fun getPipFrame(): Bitmap?

    /**
     * Copy the current full-screen menu cache into the PiP frame. Call right before the menu
     * screenshot cache is cleared (menu close) so the freshest known frame survives as the PiP
     * still.
     */
    fun promoteCachedFullToPipFrame()

    /** Release the cached PiP frame. Call from GameActivity.onDestroy(). */
    fun clearPipFrame()
}

class PipFrameStore(
        /**
         * These two collaborators are `var`s (not constructor `val`s) so [ScreenshotCaptureUtil]
         * can wire them up from an `init` block *after* delegating to this store in its
         * supertype list -- an object's own later-declared members (`captureFullScreen`,
         * `cachedFullScreenshot`) aren't visible from within its own supertype constructor
         * arguments (there is no `this` yet at that point), so the dependency has to be injected
         * post-construction instead.
         */
        private var captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback -> callback(null) },
        private var getCachedFullScreenshot: () -> Bitmap? = { null }
) : PipFrameCache {

    /** Wires up the real collaborators once the owning object has finished constructing. */
    fun configure(
            captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit,
            getCachedFullScreenshot: () -> Bitmap?
    ) {
        this.captureFullScreen = captureFullScreen
        this.getCachedFullScreenshot = getCachedFullScreenshot
    }

    companion object {
        private const val TAG = "ScreenshotCaptureUtil"

        // A paused-game still does not need to be sub-second fresh; keep the recurring cost low.
        private const val PIP_FRAME_MIN_INTERVAL_MS = 2500L
    }

    @Volatile private var pipFrame: Bitmap? = null

    /** Last time a PixelCopy was actually issued, for throttling frequent triggers. */
    @Volatile private var lastPipFrameCaptureAt = 0L

    override fun capturePipFrame(glRetroView: GLRetroView, force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPipFrameCaptureAt < PIP_FRAME_MIN_INTERVAL_MS) return
        lastPipFrameCaptureAt = now

        captureFullScreen(glRetroView) { bitmap ->
            if (bitmap != null) {
                updatePipFrame(bitmap)
                Log.d(TAG, "PiP frame updated (${bitmap.width}x${bitmap.height})")
            } else {
                Log.w(TAG, "PiP frame capture failed; keeping previous frame")
            }
        }
    }

    override fun updatePipFrame(bitmap: Bitmap?) {
        synchronized(this) {
            if (pipFrame === bitmap) return
            pipFrame = bitmap
        }
    }

    override fun getPipFrame(): Bitmap? = pipFrame

    override fun promoteCachedFullToPipFrame() {
        val source = getCachedFullScreenshot() ?: return
        if (source.isRecycled) return
        val copy = try {
            source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
            // Bitmap.copy() throws IllegalStateException ("Can't copy a recycled bitmap") if the
            // bitmap is recycled concurrently between the isRecycled check above and this call --
            // there is no lock across the two, so this is a real, reachable race.
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not copy cached frame for PiP", e)
            return
        }
        updatePipFrame(copy)
    }

    override fun clearPipFrame() {
        synchronized(this) {
            pipFrame?.recycle()
            pipFrame = null
        }
        lastPipFrameCaptureAt = 0L
        Log.d(TAG, "PiP frame cleared")
    }
}
