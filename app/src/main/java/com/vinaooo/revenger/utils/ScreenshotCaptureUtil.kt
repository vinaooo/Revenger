package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView

/**
 * Utility class for capturing screenshots from the emulator's GLRetroView.
 *
 * Uses PixelCopy API to capture only the game viewport, excluding black borders and gamepad
 * overlays.
 */
object ScreenshotCaptureUtil {

    private const val TAG = "ScreenshotCaptureUtil"

    /**
     * Cached screenshot from when the menu was opened. This bitmap is captured at pause time and
     * used when saving.
     */
    private var cachedScreenshot: Bitmap? = null

    /**
     * Capture screenshot of the GLRetroView game area.
     *
     * This method captures the visible game content, excluding black borders by using the viewport
     * configuration.
     *
     * @param glRetroView The GLRetroView instance to capture
     * @param window The window for pixel copy operation
     * @param callback Called with the captured Bitmap or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureGameScreen(glRetroView: GLRetroView, window: Window, callback: (Bitmap?) -> Unit) {
        try {
            // Get the visible area of the GLRetroView
            val width = glRetroView.width
            val height = glRetroView.height

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions: ${width}x$height")
                callback(null)
                return
            }

            // Create bitmap with view dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Use PixelCopy for hardware-accelerated capture
            val location = IntArray(2)
            glRetroView.getLocationInWindow(location)

            val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)

            PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            // Crop out black borders if present
                            val croppedBitmap = cropBlackBorders(bitmap)
                            callback(croppedBitmap)
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
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(glRetroView: GLRetroView, window: Window) {
        captureGameScreen(glRetroView, window) { bitmap ->
            cachedScreenshot = bitmap
            Log.d(TAG, "Screenshot cached: ${bitmap != null}")
        }
    }

    /** Get the cached screenshot for saving. */
    fun getCachedScreenshot(): Bitmap? {
        return cachedScreenshot
    }

    /** Clear the cached screenshot (call when menu closes without saving). */
    fun clearCachedScreenshot() {
        cachedScreenshot?.recycle()
        cachedScreenshot = null
        Log.d(TAG, "Cached screenshot cleared")
    }

    /**
     * Crop black borders from the screenshot.
     *
     * Analyzes the bitmap to find the actual game content area, removing any black (or near-black)
     * borders.
     */
    private fun cropBlackBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = 0
        var top = 0
        var right = width
        var bottom = height

        val threshold = 10 // Color threshold for "black"

        // Find left border
        outer@ for (x in 0 until width) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    left = x
                    break@outer
                }
            }
        }

        // Find right border
        outer@ for (x in (width - 1) downTo 0) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    right = x + 1
                    break@outer
                }
            }
        }

        // Find top border
        outer@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    top = y
                    break@outer
                }
            }
        }

        // Find bottom border
        outer@ for (y in (height - 1) downTo 0) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    bottom = y + 1
                    break@outer
                }
            }
        }

        // Validate bounds
        val newWidth = right - left
        val newHeight = bottom - top

        if (newWidth <= 0 || newHeight <= 0 || newWidth > width || newHeight > height) {
            Log.w(TAG, "Invalid crop bounds, returning original bitmap")
            return bitmap
        }

        return Bitmap.createBitmap(bitmap, left, top, newWidth, newHeight)
    }

    private fun isNearBlack(pixel: Int, threshold: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r < threshold && g < threshold && b < threshold
    }
}
