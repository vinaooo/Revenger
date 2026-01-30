package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView

object ScreenshotCaptureUtil {

    private const val TAG = "ScreenshotCaptureUtil"

    private var cachedScreenshot: Bitmap? = null

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

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val location = IntArray(2)
            glRetroView.getLocationInWindow(location)

            val activity = glRetroView.context as? android.app.Activity
            if (activity == null) {
                Log.e(TAG, "Could not get Activity from context")
                callback(null)
                return
            }

            try {
                if (glRetroView is SurfaceView) {
                    // Directly request PixelCopy from the SurfaceView backing the GL view.
                    PixelCopy.request(
                            glRetroView,
                            bitmap,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    val croppedBitmap = cropBlackBorders(bitmap)
                                    callback(croppedBitmap)
                                } else {
                                    Log.e(
                                            TAG,
                                            "PixelCopy (SurfaceView) failed with result: $copyResult"
                                    )
                                    callback(null)
                                }
                            },
                            Handler(Looper.getMainLooper())
                    )
                    return
                }
            } catch (e: Exception) {
                Log.w(
                        TAG,
                        "SurfaceView PixelCopy attempt failed, falling back to window capture",
                        e
                )
            }

            val rect = Rect(location[0], location[1], location[0] + width, location[1] + height)

            PixelCopy.request(
                    activity.window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            val croppedBitmap = cropBlackBorders(bitmap)
                            callback(croppedBitmap)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $copyResult")
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(glRetroView: GLRetroView) {
        captureGameScreenWithRetries(glRetroView, maxAttempts = 3, delayMs = 150) { bitmap ->
            cachedScreenshot = bitmap
            Log.d(TAG, "Screenshot cached (with retries): ${bitmap != null}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun captureGameScreenWithRetries(
            glRetroView: GLRetroView,
            maxAttempts: Int = 3,
            delayMs: Long = 150,
            callback: (Bitmap?) -> Unit
    ) {
        var attempt = 1

        val handler = Handler(Looper.getMainLooper())

        fun doAttempt() {
            captureGameScreen(glRetroView) { bitmap ->
                if (bitmap == null) {
                    Log.w(TAG, "capture attempt $attempt returned null")
                    if (attempt < maxAttempts) {
                        attempt++
                        handler.postDelayed({ doAttempt() }, delayMs)
                    } else {
                        callback(null)
                    }
                    return@captureGameScreen
                }

                try {
                    if (isMostlyTransparent(bitmap) && attempt < maxAttempts) {
                        Log.w(
                                TAG,
                                "capture attempt $attempt produced mostly-transparent image, retrying..."
                        )
                        attempt++
                        bitmap.recycle()
                        handler.postDelayed({ doAttempt() }, delayMs)
                        return@captureGameScreen
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing bitmap transparency", e)
                }

                callback(bitmap)
            }
        }

        doAttempt()
    }

    fun getCachedScreenshot(): Bitmap? = cachedScreenshot

    fun clearCachedScreenshot() {
        cachedScreenshot?.recycle()
        cachedScreenshot = null
        Log.d(TAG, "Cached screenshot cleared")
    }

    private fun cropBlackBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = 0
        var top = 0
        var right = width
        var bottom = height

        val threshold = 10

        outer@ for (x in 0 until width) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    left = x
                    break@outer
                }
            }
        }

        outer@ for (x in (width - 1) downTo 0) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    right = x + 1
                    break@outer
                }
            }
        }

        outer@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    top = y
                    break@outer
                }
            }
        }

        outer@ for (y in (height - 1) downTo 0) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    bottom = y + 1
                    break@outer
                }
            }
        }

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

    private fun isMostlyTransparent(
            bitmap: Bitmap,
            alphaThreshold: Int = 10,
            fractionThreshold: Double = 0.9
    ): Boolean {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val total = width * height
            val pixels = IntArray(total)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var transparentCount = 0
            for (i in 0 until total step Math.max(1, total / 1000)) { // sample up to 1000 pixels
                val a = (pixels[i] ushr 24) and 0xFF
                if (a <= alphaThreshold) transparentCount++
            }

            val sampled = Math.max(1, total / Math.max(1, total / 1000))
            val fraction = transparentCount.toDouble() / sampled.toDouble()
            return fraction >= fractionThreshold
        } catch (e: Exception) {
            Log.e(TAG, "Failed to determine transparency", e)
            return false
        }
    }
}
