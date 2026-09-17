package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView

/**
 * Cache for the cropped/full menu screenshots (thumbnails + load-preview overlay), split out of
 * [ScreenshotCaptureUtil] purely to keep that object under the project's function-count
 * threshold. Exposed back on it via Kotlin interface delegation (`by`), the same pattern already
 * used there for [PipFrameCache]/[PipFrameStore].
 */
interface CroppedScreenshotCache {
    /**
     * Capture and cache both cropped and full screenshots when the menu opens. Cropped screenshot
     * (no black bars) is used for slot thumbnails; full screenshot (with black bars) is used for
     * the load preview overlay.
     *
     * @param glRetroView The GLRetroView to capture from
     * @param onCaptured Optional callback when capture completes
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(glRetroView: GLRetroView, onCaptured: ((Boolean) -> Unit)? = null)

    /** Get the cached cropped screenshot for saving. Returns null if no screenshot was cached. */
    fun getCachedScreenshot(): Bitmap?

    /** Manually inject bitmaps. Useful for PiP. */
    fun setManualScreenshots(screenshot: Bitmap?, fullScreenshot: Bitmap?)

    /** Get the cached full-screen screenshot (with black bars) for preview overlay. */
    fun getCachedFullScreenshot(): Bitmap?

    /** Check if a cached screenshot exists. */
    fun hasCachedScreenshot(): Boolean

    /** Clear all cached screenshots. Call when menu closes without saving to free memory. */
    fun clearCachedScreenshot()
}

class CroppedScreenshotStore(
        /**
         * `var`s (not constructor `val`s) for the same reason as [PipFrameStore]'s collaborators:
         * [ScreenshotCaptureUtil] wires them up from an `init` block *after* delegating to this
         * store in its supertype list, since its own later-declared members aren't visible from
         * within its own supertype constructor arguments.
         */
        private var captureGameScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback ->
                    callback(null)
                },
        private var captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback ->
                    callback(null)
                }
) : CroppedScreenshotCache {

    /** Wires up the real collaborators once the owning object has finished constructing. */
    fun configure(
            captureGameScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit,
            captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit
    ) {
        this.captureGameScreen = captureGameScreen
        this.captureFullScreen = captureFullScreen
    }

    companion object {
        private const val TAG = "ScreenshotCaptureUtil"
    }

    /**
     * Cached screenshot from when the menu was opened. Captured at pause time and used when
     * saving. This is the cropped version (no black bars) used for slot thumbnails.
     */
    @Volatile private var cachedScreenshot: Bitmap? = null

    /**
     * Cached full-screen screenshot including black bars. Used as load preview overlay -- matches
     * the screen pixel-perfectly with fitXY.
     */
    @Volatile private var cachedFullScreenshot: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun captureAndCacheScreenshot(glRetroView: GLRetroView, onCaptured: ((Boolean) -> Unit)?) {
        // Capture cropped screenshot for slot thumbnails
        captureGameScreen(glRetroView) { bitmap ->
            synchronized(this) {
                cachedScreenshot?.recycle()
                cachedScreenshot = bitmap
            }
            Log.d(TAG, "Cropped screenshot cached: ${bitmap != null}")
        }

        // Capture full screenshot for load preview overlay
        captureFullScreen(glRetroView) { fullBitmap ->
            synchronized(this) {
                cachedFullScreenshot?.recycle()
                cachedFullScreenshot = fullBitmap
            }
            Log.d(TAG, "Full screenshot cached: ${fullBitmap != null}")
            onCaptured?.invoke(fullBitmap != null || cachedScreenshot != null)
        }
    }

    override fun getCachedScreenshot(): Bitmap? = cachedScreenshot

    override fun setManualScreenshots(screenshot: Bitmap?, fullScreenshot: Bitmap?) {
        synchronized(this) {
            cachedScreenshot?.recycle()
            cachedScreenshot = screenshot
            cachedFullScreenshot?.recycle()
            cachedFullScreenshot = fullScreenshot
        }
    }

    override fun getCachedFullScreenshot(): Bitmap? = cachedFullScreenshot

    override fun hasCachedScreenshot(): Boolean = cachedScreenshot != null

    override fun clearCachedScreenshot() {
        synchronized(this) {
            cachedScreenshot?.recycle()
            cachedScreenshot = null
            cachedFullScreenshot?.recycle()
            cachedFullScreenshot = null
        }
        Log.d(TAG, "All cached screenshots cleared")
    }
}
