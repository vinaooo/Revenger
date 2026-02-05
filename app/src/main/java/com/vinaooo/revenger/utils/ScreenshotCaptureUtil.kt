package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Utility class for capturing screenshots from the emulator's GLRetroView.
 *
 * Uses PixelCopy API for hardware-accelerated capture from GL surfaces. Screenshots are cached when
 * the menu opens and used when saving states.
 *
 * Features:
 * - Automatically crops black bars (letterbox/pillarbox) based on game aspect ratio
 * - Uses PixelCopy API for accurate GL surface capture
 * - Caches screenshots for save state operations
 * - Determines aspect ratio from LibRetro core name in config
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
     * Cached context for reading config values.
     */
    private var cachedContext: Context? = null

    /**
     * Set the context for reading config values.
     * Should be called from GameActivity.onCreate().
     */
    fun setContext(context: Context) {
        cachedContext = context.applicationContext
    }

    /**
     * Known aspect ratios for LibRetro cores.
     * These are the standard PAR-corrected aspect ratios for each system.
     */
    private object AspectRatios {
        // SNES: 8:7 pixel aspect ratio, 256x224 -> 4:3 display
        const val SNES = 4f / 3f
        
        // Game Boy: 160x144 -> 10:9 display
        const val GAME_BOY = 10f / 9f
        
        // Game Boy Color: Same as Game Boy
        const val GAME_BOY_COLOR = GAME_BOY
        
        // Game Boy Advance: 240x160 -> 3:2 display
        const val GAME_BOY_ADVANCE = 3f / 2f
        
        // Sega Master System: 256x192 -> 4:3 display
        const val MASTER_SYSTEM = 4f / 3f
        
        // Sega Mega Drive / Genesis: 320x224 -> 4:3 display
        const val MEGA_DRIVE = 4f / 3f
        
        // NES: 256x240 -> 4:3 display
        const val NES = 4f / 3f
        
        // Default fallback
        const val DEFAULT = 4f / 3f
    }

    /**
     * Get the aspect ratio for a given LibRetro core name.
     * 
     * @param coreName The core name from config (e.g., "gambatte", "snes9x", "genesis_plus_gx")
     * @return The aspect ratio for the core's target system
     */
    private fun getAspectRatioForCore(coreName: String): Float {
        return when (coreName.lowercase()) {
            // SNES cores
            "snes9x", "bsnes", "snes9x_next", "mednafen_snes", "mesen-s" -> AspectRatios.SNES
            
            // Game Boy / Game Boy Color cores
            "gambatte", "mgba", "vba_next", "sameboy", "gearboy" -> AspectRatios.GAME_BOY
            
            // Game Boy Advance cores
            "gpsp", "vba-m", "meteor" -> AspectRatios.GAME_BOY_ADVANCE
            
            // Master System cores
            "gearsystem", "genesis_plus_gx", "picodrive", "smsplus" -> AspectRatios.MASTER_SYSTEM
            
            // Mega Drive / Genesis cores (same cores as Master System, but different aspect)
            // Note: picodrive and genesis_plus_gx support both, so we use Master System default
            // The actual aspect depends on the ROM being played
            
            // NES cores
            "nestopia", "fceumm", "quicknes", "mesen" -> AspectRatios.NES
            
            // Default fallback
            else -> {
                Log.w(TAG, "Unknown core '$coreName', using default aspect ratio")
                AspectRatios.DEFAULT
            }
        }
    }

    /**
     * Calculate the game content rectangle within the GLRetroView.
     * This removes the black bars (letterbox/pillarbox) based on the game's aspect ratio.
     *
     * @param viewWidth The width of the GLRetroView
     * @param viewHeight The height of the GLRetroView
     * @param gameAspectRatio The aspect ratio of the game content
     * @return Rect representing the game content area (excluding black bars)
     */
    private fun calculateGameContentRect(viewWidth: Int, viewHeight: Int, gameAspectRatio: Float): Rect {
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
     * Capture screenshot of the GLRetroView game area using PixelCopy API.
     * Automatically crops black bars based on game aspect ratio.
     *
     * This method captures only the visible game content, excluding any letterbox/pillarbox
     * black bars that may be present due to aspect ratio differences.
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

            // Get game aspect ratio based on the core name from config
            val gameAspectRatio = try {
                val context = cachedContext
                if (context != null) {
                    val coreName = context.getString(R.string.conf_core)
                    val aspectRatio = getAspectRatioForCore(coreName)
                    Log.d(TAG, "Core: $coreName, Aspect ratio: $aspectRatio")
                    aspectRatio
                } else {
                    Log.w(TAG, "Context not set, using default aspect ratio")
                    AspectRatios.DEFAULT
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine aspect ratio: ${e.message}")
                AspectRatios.DEFAULT
            }

            // Calculate the game content rectangle (excluding black bars)
            val gameRect = calculateGameContentRect(width, height, gameAspectRatio)
            
            Log.d(TAG, "View: ${width}x$height, Game aspect: $gameAspectRatio, Content rect: $gameRect")

            // Create bitmap for the cropped game content
            val croppedWidth = gameRect.width()
            val croppedHeight = gameRect.height()
            
            if (croppedWidth <= 0 || croppedHeight <= 0) {
                Log.w(TAG, "Invalid cropped dimensions: ${croppedWidth}x$croppedHeight")
                callback(null)
                return
            }

            val bitmap = Bitmap.createBitmap(croppedWidth, croppedHeight, Bitmap.Config.ARGB_8888)

            // GLRetroView extends GLSurfaceView which extends SurfaceView
            val surfaceView = glRetroView as SurfaceView

            // Check if surface is valid
            if (!surfaceView.holder.surface.isValid) {
                Log.e(TAG, "Surface is not valid for capture")
                bitmap.recycle()
                callback(null)
                return
            }

            // Use PixelCopy with source rect to capture only the game content area
            PixelCopy.request(
                    surfaceView,
                    gameRect, // Only capture the game content area
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            Log.d(TAG, "Screenshot captured: ${croppedWidth}x$croppedHeight (cropped from ${width}x$height)")
                            // Auto-crop any remaining black borders from the core output
                            val autoCropped = autoCropBlackBorders(bitmap)
                            if (autoCropped !== bitmap) {
                                bitmap.recycle()
                            }
                            callback(autoCropped)
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
    private fun autoCropBlackBorders(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxCropX = (w * 0.05f).toInt() // Max 5% crop on each side
        val maxCropY = (h * 0.05f).toInt()
        val sampleStep = maxOf(h / 20, 1) // Sample ~20 rows for performance
        val brightnessThreshold = 10

        // Find left border
        var left = 0
        for (x in 0 until minOf(maxCropX, w)) {
            var hasContent = false
            for (y in 0 until h step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
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
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
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
        val sampleStepX = maxOf(w / 20, 1)
        var top = 0
        for (y in 0 until minOf(maxCropY, h)) {
            var hasContent = false
            for (x in 0 until w step sampleStepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
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
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
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

        Log.d(TAG, "Auto-crop: Removing borders L=$left T=$top R=${w - 1 - right} B=${h - 1 - bottom} -> ${cropWidth}x$cropHeight")
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
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
