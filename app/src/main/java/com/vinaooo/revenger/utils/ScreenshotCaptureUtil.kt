package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView

/**
 * Utility class for capturing screenshots from the emulator's GLRetroView.
 *
 * Uses PixelCopy API for hardware-accelerated capture from GL surfaces. Screenshots are cached when
 * the menu opens and used when saving states.
 *
 * Performance considerations:
 * - PixelCopy is async and doesn't block the GL thread
 * - Screenshots are cached to avoid capturing during save dialog
 * - WebP compression is deferred to SaveStateManager
 *
 * IMPORTANT: GLRetroView is a GLSurfaceView, so we must use PixelCopy.request(SurfaceView, ...)
 * instead of PixelCopy.request(Window, ...) to capture the actual GL content.
 */
object ScreenshotCaptureUtil {

    private const val TAG = "ScreenshotCaptureUtil"

    /**
     * Cached screenshot from when the menu was opened. Captured at pause time and used when saving.
     */
    @Volatile private var cachedScreenshot: Bitmap? = null

    /**
     * Capture screenshot of the GLRetroView game area using PixelCopy API.
     *
     * This method captures the visible game content asynchronously. Works with GL surfaces via
     * hardware-accelerated capture.
     *
     * NOTE: GLRetroView extends GLSurfaceView, which extends SurfaceView. We MUST use
     * PixelCopy.request(SurfaceView, ...) to capture the GL content, not PixelCopy.request(Window,
     * ...) which only captures the window compositor layer.
     *
     * @param glRetroView The GLRetroView instance to capture
     * @param callback Called with the captured Bitmap or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureGameScreen(glRetroView: GLRetroView, callback: (Bitmap?) -> Unit) {
        try {
            val width = glRetroView.width
            val height = glRetroView.height

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions: ${width}x$height")
                callback(null)
                return
            }

            // Create bitmap with view dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // GLRetroView extends GLSurfaceView which extends SurfaceView
            // We need to use the SurfaceView variant of PixelCopy to capture GL content
            val surfaceView = glRetroView as SurfaceView

            // Check if surface is valid
            if (!surfaceView.holder.surface.isValid) {
                Log.e(TAG, "Surface is not valid for capture")
                bitmap.recycle()
                callback(null)
                return
            }

            // Use PixelCopy with SurfaceView to capture the actual GL rendered content
            PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            Log.d(TAG, "Screenshot captured successfully: ${width}x$height")
                            callback(bitmap)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $copyResult")
                            bitmap.recycle()
                            callback(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
            callback(null)
        }
    }

    /**
     * Capture and cache screenshot when menu opens. This should be called when the game pauses for
     * menu.
     *
     * @param glRetroView The GLRetroView to capture from
     * @param onCaptured Optional callback when capture completes
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(
            glRetroView: GLRetroView,
            onCaptured: ((Boolean) -> Unit)? = null
    ) {
        captureGameScreen(glRetroView) { bitmap ->
            synchronized(this) {
                // Recycle old cached screenshot
                cachedScreenshot?.recycle()
                cachedScreenshot = bitmap
            }
            Log.d(TAG, "Screenshot cached: ${bitmap != null}")
            onCaptured?.invoke(bitmap != null)
        }
    }

    /** Get the cached screenshot for saving. Returns null if no screenshot was cached. */
    fun getCachedScreenshot(): Bitmap? {
        return cachedScreenshot
    }

    /** Check if a cached screenshot exists. */
    fun hasCachedScreenshot(): Boolean {
        return cachedScreenshot != null
    }

    /** Clear the cached screenshot. Call when menu closes without saving to free memory. */
    fun clearCachedScreenshot() {
        synchronized(this) {
            cachedScreenshot?.recycle()
            cachedScreenshot = null
        }
        Log.d(TAG, "Cached screenshot cleared")
    }

    /**
     * Capture screenshot synchronously using View.drawToBitmap fallback. Use only when PixelCopy is
     * not available or fails.
     *
     * @param view The view to capture
     * @return Bitmap of the view, or null on failure
     */
    fun captureViewFallback(view: View): Bitmap? {
        return try {
            val width = view.width
            val height = view.height

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions for fallback: ${width}x$height")
                return null
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Fallback screenshot capture failed", e)
            null
        }
    }
}
