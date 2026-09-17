package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

/**
 * Pure black-border / aspect-ratio geometry math for [ScreenshotCaptureUtil]'s screen capture,
 * split out purely to keep that object under the project's function-count threshold. Both methods
 * were private implementation details of the capture flow; they're public here (called directly,
 * no interface delegation) since nothing outside [ScreenshotCaptureUtil] itself ever called them.
 */
object ScreenshotGeometry {

    private const val TAG = "ScreenshotGeometry"

    // Auto-crop border detection (see autoCropBlackBorders below)
    private const val MAX_BORDER_CROP_RATIO = 0.05f // Max 5% crop on each side
    private const val BORDER_SCAN_SAMPLE_COUNT = 20 // Sample ~20 rows/columns for performance
    private const val BLACK_BORDER_BRIGHTNESS_THRESHOLD = 10
    private const val RED_CHANNEL_SHIFT_BITS = 16
    private const val GREEN_CHANNEL_SHIFT_BITS = 8
    private const val COLOR_CHANNEL_MASK = 0xFF

    /**
     * Calculate the game content rectangle within the GLRetroView.
     * This removes the black bars (letterbox/pillarbox) based on the game's aspect ratio.
     *
     * @param viewWidth The width of the GLRetroView
     * @param viewHeight The height of the GLRetroView
     * @param gameAspectRatio The aspect ratio of the game content
     * @return Rect representing the game content area (excluding black bars)
     */
    fun calculateGameContentRect(viewWidth: Int, viewHeight: Int, gameAspectRatio: Float): Rect {
        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()

        val contentWidth: Int
        val contentHeight: Int
        val offsetX: Int
        val offsetY: Int

        if (gameAspectRatio > viewAspectRatio) {
            // Game is wider than view - has black bars on top/bottom (letterbox)
            contentWidth = viewWidth
            contentHeight = (viewWidth / gameAspectRatio).toInt()
            offsetX = 0
            offsetY = (viewHeight - contentHeight) / 2
        } else {
            // Game is taller than view - has black bars on left/right (pillarbox)
            contentHeight = viewHeight
            contentWidth = (viewHeight * gameAspectRatio).toInt()
            offsetX = (viewWidth - contentWidth) / 2
            offsetY = 0
        }

        return Rect(offsetX, offsetY, offsetX + contentWidth, offsetY + contentHeight)
    }

    /**
     * Auto-crop black borders from a bitmap.
     *
     * Some LibRetro cores (e.g., picodrive for Master System) render frames with
     * small black borders that don't match the reported aspect ratio exactly.
     * This method detects and removes those borders by scanning pixel brightness.
     *
     * Only crops if borders are detected (brightness threshold < 10).
     * Limits cropping to max 5% of each dimension to avoid false positives.
     *
     * @param bitmap The source bitmap to auto-crop
     * @return A new cropped bitmap, or the original if no cropping was needed
     */
    fun autoCropBlackBorders(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxCropX = (w * MAX_BORDER_CROP_RATIO).toInt()
        val maxCropY = (h * MAX_BORDER_CROP_RATIO).toInt()
        val sampleStep = maxOf(h / BORDER_SCAN_SAMPLE_COUNT, 1)
        val brightnessThreshold = BLACK_BORDER_BRIGHTNESS_THRESHOLD

        // Find left border
        var left = 0
        for (x in 0 until minOf(maxCropX, w)) {
            var hasContent = false
            for (y in 0 until h step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr RED_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val g = (pixel shr GREEN_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val b = pixel and COLOR_CHANNEL_MASK
                if (r + g + b > brightnessThreshold) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                left = x
                break
            }
        }

        // Find right border
        var right = w - 1
        for (x in w - 1 downTo maxOf(w - maxCropX, 0)) {
            var hasContent = false
            for (y in 0 until h step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr RED_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val g = (pixel shr GREEN_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val b = pixel and COLOR_CHANNEL_MASK
                if (r + g + b > brightnessThreshold) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                right = x
                break
            }
        }

        // Find top border
        val sampleStepX = maxOf(w / BORDER_SCAN_SAMPLE_COUNT, 1)
        var top = 0
        for (y in 0 until minOf(maxCropY, h)) {
            var hasContent = false
            for (x in 0 until w step sampleStepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr RED_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val g = (pixel shr GREEN_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val b = pixel and COLOR_CHANNEL_MASK
                if (r + g + b > brightnessThreshold) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                top = y
                break
            }
        }

        // Find bottom border
        var bottom = h - 1
        for (y in h - 1 downTo maxOf(h - maxCropY, 0)) {
            var hasContent = false
            for (x in 0 until w step sampleStepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr RED_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val g = (pixel shr GREEN_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
                val b = pixel and COLOR_CHANNEL_MASK
                if (r + g + b > brightnessThreshold) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                bottom = y
                break
            }
        }

        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1

        // Only crop if we actually found borders (at least 2px on any side)
        if (left < 2 && (w - 1 - right) < 2 && top < 2 && (h - 1 - bottom) < 2) {
            Log.d(TAG, "Auto-crop: No significant black borders detected")
            return bitmap
        }

        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.w(TAG, "Auto-crop: Invalid crop dimensions, skipping")
            return bitmap
        }

        Log.d(
                TAG,
                "Auto-crop: Removing borders L=$left T=$top R=${w - 1 - right} " +
                        "B=${h - 1 - bottom} -> ${cropWidth}x$cropHeight"
        )
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }
}
