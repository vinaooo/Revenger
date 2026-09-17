package com.vinaooo.revenger.utils

import android.content.Context
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
import com.vinaooo.revenger.R

// An object's own body members aren't visible from within its own supertype constructor
// arguments (there is no `this` yet at that point), so this store is instantiated at file scope
// -- with placeholder no-op collaborators -- and wired up to the real ones from an `init` block
// inside [ScreenshotCaptureUtil] below, once that object has finished constructing.
private val pipFrameStore = PipFrameStore()

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
 *
 * The black-border/aspect-ratio math lives in [ScreenshotGeometry] and the Picture-in-Picture
 * frame cache lives in [PipFrameStore] -- both split out purely to keep this object under the
 * project's function-count threshold; see those classes for why.
 */
object ScreenshotCaptureUtil : PipFrameCache by pipFrameStore {

    init {
        pipFrameStore.configure(
                captureFullScreen = { glRetroView, callback -> captureFullScreen(glRetroView, callback) },
                getCachedFullScreenshot = { cachedFullScreenshot }
        )
    }

    private const val TAG = "ScreenshotCaptureUtil"

    /**
     * Cached screenshot from when the menu was opened. Captured at pause time and used when saving.
     * This is the cropped version (no black bars) used for slot thumbnails.
     */
    @Volatile private var cachedScreenshot: Bitmap? = null

    /**
     * Cached full-screen screenshot including black bars.
     * Used as load preview overlay — matches the screen pixel-perfectly with fitXY.
     */
    @Volatile private var cachedFullScreenshot: Bitmap? = null

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

            // Get game aspect ratio based on the platform config (matching PiP ratio)
            val gameAspectRatio = try {
                val context = cachedContext
                if (context != null) {
                    val platformId = com.vinaooo.revenger.RevengerApplication.appConfig.getPlatformId()
                    val pipProfile = com.vinaooo.revenger.repositories.PipConfigRepository.getProfile(platformId)
                    val aspectRatio = pipProfile.ratioW.toFloat() / pipProfile.ratioH.toFloat()
                    Log.d(TAG, "Platform: $platformId, Aspect ratio: $aspectRatio")
                    aspectRatio
                } else {
                    Log.w(TAG, "Context not set, using default aspect ratio")
                    AspectRatios.DEFAULT
                }
                // RevengerApplication.appConfig is a lateinit var; accessing it before
                // Application.onCreate() completes throws UninitializedPropertyAccessException.
            } catch (e: UninitializedPropertyAccessException) {
                Log.w(TAG, "Could not determine aspect ratio", e)
                AspectRatios.DEFAULT
            }

            // Calculate the game content rectangle (excluding black bars)
            val gameRect = ScreenshotGeometry.calculateGameContentRect(width, height, gameAspectRatio)

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
                            Log.d(
                                    TAG,
                                    "Screenshot captured: ${croppedWidth}x$croppedHeight " +
                                            "(cropped from ${width}x$height)"
                            )
                            // Auto-crop any remaining black borders from the core output
                            val autoCropped = ScreenshotGeometry.autoCropBlackBorders(bitmap)
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
            // PixelCopy.request() documents throwing IllegalArgumentException when the source
            // surface isn't laid out or attached to a window; the dimension/validity checks above
            // rule out the other documented causes (invalid rect, immutable/hardware bitmap).
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to capture screenshot", e)
            callback(null)
        }
    }

    /**
     * Capture a full-screen screenshot of the GLRetroView WITHOUT cropping.
     * Includes black bars (letterbox/pillarbox) so the image matches the screen pixel-perfectly
     * when displayed with fitXY + match_parent. Used for load preview overlay.
     *
     * @param glRetroView The GLRetroView instance to capture
     * @param callback Called with the captured Bitmap or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureFullScreen(glRetroView: GLRetroView, callback: (Bitmap?) -> Unit) {
        try {
            val width = glRetroView.width
            val height = glRetroView.height

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions for full capture: ${width}x$height")
                callback(null)
                return
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val surfaceView = glRetroView as SurfaceView

            if (!surfaceView.holder.surface.isValid) {
                Log.e(TAG, "Surface is not valid for full capture")
                bitmap.recycle()
                callback(null)
                return
            }

            // Capture entire surface — no source rect, no crop
            PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            Log.d(TAG, "Full screenshot captured: ${width}x$height")
                            callback(bitmap)
                        } else {
                            Log.e(TAG, "Full PixelCopy failed with result: $copyResult")
                            bitmap.recycle()
                            callback(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
            )
            // Same rationale as captureGameScreen(): PixelCopy.request()'s only documented
            // failure not already ruled out by the checks above is IllegalArgumentException.
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to capture full screenshot", e)
            callback(null)
        }
    }

    /**
     * Capture and cache both cropped and full screenshots when menu opens.
     * Cropped screenshot (no black bars) is used for slot thumbnails.
     * Full screenshot (with black bars) is used for load preview overlay.
     *
     * @param glRetroView The GLRetroView to capture from
     * @param onCaptured Optional callback when capture completes
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(
            glRetroView: GLRetroView,
            onCaptured: ((Boolean) -> Unit)? = null
    ) {
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

    /** Get the cached cropped screenshot for saving. Returns null if no screenshot was cached. */
    fun getCachedScreenshot(): Bitmap? {
        return cachedScreenshot
    }

    /** Manually inject bitmaps. Useful for PiP. */
    fun setManualScreenshots(screenshot: Bitmap?, fullScreenshot: Bitmap?) {
        synchronized(this) {
            cachedScreenshot?.recycle()
            cachedScreenshot = screenshot
            cachedFullScreenshot?.recycle()
            cachedFullScreenshot = fullScreenshot
        }
    }

    /** Get the cached full-screen screenshot (with black bars) for preview overlay. */
    fun getCachedFullScreenshot(): Bitmap? {
        return cachedFullScreenshot
    }

    /** Check if a cached screenshot exists. */
    fun hasCachedScreenshot(): Boolean {
        return cachedScreenshot != null
    }

    /** Clear all cached screenshots. Call when menu closes without saving to free memory. */
    fun clearCachedScreenshot() {
        synchronized(this) {
            cachedScreenshot?.recycle()
            cachedScreenshot = null
            cachedFullScreenshot?.recycle()
            cachedFullScreenshot = null
        }
        Log.d(TAG, "All cached screenshots cleared")
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
            // view.draw() dispatches into an arbitrary caller-supplied View's onDraw() /
            // dispatchDraw() override, which isn't enumerable from this generic fallback utility;
            // kept broad via the escape hatch.
        } catch (expectedViewDrawFailure: Exception) {
            Log.e(TAG, "Fallback screenshot capture failed", expectedViewDrawFailure)
            null
        }
    }
}
