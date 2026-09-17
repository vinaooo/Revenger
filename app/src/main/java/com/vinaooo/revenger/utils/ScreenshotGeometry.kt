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

    // A detected inset smaller than this (px) on every side is treated as "no real border".
    private const val MIN_SIGNIFICANT_BORDER_PX = 2

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
        val bounds = detectContentBounds(bitmap)
        val cropSize = resolveCropSize(bounds, bitmap.width, bitmap.height) ?: return bitmap

        return cropBitmap(bitmap, bounds, cropSize.first, cropSize.second)
    }

    /**
     * Decides whether [bounds] represents a real border worth cropping and, if so, the resulting
     * (width, height). Returns null -- logging why -- when there's nothing to crop.
     */
    private fun resolveCropSize(bounds: ContentBounds, width: Int, height: Int): Pair<Int, Int>? {
        if (isBorderNegligible(bounds, width, height)) {
            return logCropSkipped(isError = false, "Auto-crop: No significant black borders detected")
        }

        val cropWidth = bounds.right - bounds.left + 1
        val cropHeight = bounds.bottom - bounds.top + 1
        return if (cropWidth <= 0 || cropHeight <= 0) {
            logCropSkipped(isError = true, "Auto-crop: Invalid crop dimensions, skipping")
        } else {
            cropWidth to cropHeight
        }
    }

    private fun logCropSkipped(isError: Boolean, message: String): Pair<Int, Int>? {
        if (isError) Log.w(TAG, message) else Log.d(TAG, message)
        return null
    }

    /** The detected non-black content box, in the source bitmap's own pixel coordinates. */
    private data class ContentBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

    /**
     * Scans inward from each of the bitmap's four edges (within [MAX_BORDER_CROP_RATIO] of that
     * dimension) for the first row/column with non-black content. A side where no content is
     * found within the scan window keeps its full-bitmap default (0 or `width/height - 1`), i.e.
     * "no border detected on that side".
     */
    private fun detectContentBounds(bitmap: Bitmap): ContentBounds {
        val w = bitmap.width
        val h = bitmap.height
        val maxCropX = (w * MAX_BORDER_CROP_RATIO).toInt()
        val maxCropY = (h * MAX_BORDER_CROP_RATIO).toInt()
        val sampleStep = maxOf(h / BORDER_SCAN_SAMPLE_COUNT, 1)
        val sampleStepX = maxOf(w / BORDER_SCAN_SAMPLE_COUNT, 1)

        val left =
                findFirstContentColumn(bitmap, 0 until minOf(maxCropX, w), h, sampleStep, default = 0)
        val right =
                findFirstContentColumn(
                        bitmap,
                        w - 1 downTo maxOf(w - maxCropX, 0),
                        h,
                        sampleStep,
                        default = w - 1
                )
        val top =
                findFirstContentRow(bitmap, 0 until minOf(maxCropY, h), w, sampleStepX, default = 0)
        val bottom =
                findFirstContentRow(
                        bitmap,
                        h - 1 downTo maxOf(h - maxCropY, 0),
                        w,
                        sampleStepX,
                        default = h - 1
                )

        return ContentBounds(left, top, right, bottom)
    }

    /** First x in [xRange] whose sampled column (step [sampleStep] over [h]) has content. */
    private fun findFirstContentColumn(
            bitmap: Bitmap,
            xRange: IntProgression,
            h: Int,
            sampleStep: Int,
            default: Int
    ): Int {
        for (x in xRange) {
            val hasContent =
                    (0 until h step sampleStep).any { y ->
                        pixelBrightness(bitmap.getPixel(x, y)) > BLACK_BORDER_BRIGHTNESS_THRESHOLD
                    }
            if (hasContent) return x
        }
        return default
    }

    /** First y in [yRange] whose sampled row (step [sampleStepX] over [w]) has content. */
    private fun findFirstContentRow(
            bitmap: Bitmap,
            yRange: IntProgression,
            w: Int,
            sampleStepX: Int,
            default: Int
    ): Int {
        for (y in yRange) {
            val hasContent =
                    (0 until w step sampleStepX).any { x ->
                        pixelBrightness(bitmap.getPixel(x, y)) > BLACK_BORDER_BRIGHTNESS_THRESHOLD
                    }
            if (hasContent) return y
        }
        return default
    }

    /** Sum of the R, G and B channels of an ARGB [pixel] -- higher means brighter/less black. */
    private fun pixelBrightness(pixel: Int): Int {
        val r = (pixel shr RED_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
        val g = (pixel shr GREEN_CHANNEL_SHIFT_BITS) and COLOR_CHANNEL_MASK
        val b = pixel and COLOR_CHANNEL_MASK
        return r + g + b
    }

    /** True when every detected inset is smaller than [MIN_SIGNIFICANT_BORDER_PX]. */
    private fun isBorderNegligible(bounds: ContentBounds, width: Int, height: Int): Boolean {
        val insets =
                listOf(
                        bounds.left,
                        width - 1 - bounds.right,
                        bounds.top,
                        height - 1 - bounds.bottom
                )
        return insets.all { it < MIN_SIGNIFICANT_BORDER_PX }
    }

    private fun cropBitmap(
            bitmap: Bitmap,
            bounds: ContentBounds,
            cropWidth: Int,
            cropHeight: Int
    ): Bitmap {
        Log.d(
                TAG,
                "Auto-crop: Removing borders L=${bounds.left} T=${bounds.top} " +
                        "R=${bitmap.width - 1 - bounds.right} B=${bitmap.height - 1 - bounds.bottom} " +
                        "-> ${cropWidth}x$cropHeight"
        )
        return Bitmap.createBitmap(bitmap, bounds.left, bounds.top, cropWidth, cropHeight)
    }
}
