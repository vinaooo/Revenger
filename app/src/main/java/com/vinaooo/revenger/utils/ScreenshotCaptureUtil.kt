package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import com.swordfish.libretrodroid.GLRetroView

// An object's own body members aren't visible from within its own supertype constructor
// arguments (there is no `this` yet at that point), so these stores are instantiated at file
// scope -- with placeholder no-op collaborators -- and wired up to the real ones from an `init`
// block inside [ScreenshotCaptureUtil] below, once that object has finished constructing.
private val pipFrameStore = PipFrameStore()
private val croppedScreenshotStore = CroppedScreenshotStore()

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
 * - Takes the game aspect ratio from the configured platform's PiP profile
 *
 * IMPORTANT: GLRetroView is a GLSurfaceView, so we must use PixelCopy.request(SurfaceView, ...)
 * instead of PixelCopy.request(Window, ...) to capture the actual GL content.
 *
 * The black-border/aspect-ratio math lives in [ScreenshotGeometry], the Picture-in-Picture frame
 * cache lives in [PipFrameStore], and the menu screenshot cache lives in [CroppedScreenshotStore]
 * -- all split out purely to keep this object under the project's function-count threshold; see
 * those classes for why.
 */
object ScreenshotCaptureUtil :
        PipFrameCache by pipFrameStore, CroppedScreenshotCache by croppedScreenshotStore {

    init {
        pipFrameStore.configure(
                captureFullScreen = { glRetroView, callback -> captureFullScreen(glRetroView, callback) },
                getCachedFullScreenshot = { getCachedFullScreenshot() }
        )
        croppedScreenshotStore.configure(
                captureGameScreen = { glRetroView, callback -> captureGameScreen(glRetroView, callback) },
                captureFullScreen = { glRetroView, callback -> captureFullScreen(glRetroView, callback) }
        )
    }

    private const val TAG = "ScreenshotCaptureUtil"

    /**
     * Whether [setContext] has run. The aspect ratio itself comes from
     * `RevengerApplication.appConfig`, so only this flag is kept, not the Context: this object
     * lives for the whole process (lint StaticFieldLeak).
     */
    private var contextConfigured = false

    /**
     * Enables config-based aspect-ratio lookup for captures.
     * Should be called from GameActivity.onCreate().
     */
    @Suppress("UNUSED_PARAMETER")
    fun setContext(context: Context) {
        contextConfigured = true
    }

    /** Aspect ratio used until the platform's is known (no [setContext] yet, or no config). */
    private const val DEFAULT_ASPECT_RATIO = 4f / 3f

    /** Issues the PixelCopy requests; replaced by a fake in tests. */
    internal var pixelCopier: PixelCopier = SystemPixelCopier

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
    fun captureGameScreen(glRetroView: GLRetroView, callback: (Bitmap?) -> Unit) {
        try {
            val width = glRetroView.width
            val height = glRetroView.height
            val gameRect = resolveCaptureRect(width, height, callback) ?: return
            val target = prepareCaptureTarget(glRetroView, gameRect, callback) ?: return

            // Copy only the game content area (the source rect)
            pixelCopier.copy(target.surfaceView, target.rect, target.bitmap) { copyResult ->
                onGameScreenCopyResult(copyResult, target, width, height, callback)
            }
            // PixelCopy.request() documents throwing IllegalArgumentException when the source
            // surface isn't laid out or attached to a window; the dimension/validity checks above
            // rule out the other documented causes (invalid rect, immutable/hardware bitmap).
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to capture screenshot", e)
            callback(null)
        }
    }

    /** The bitmap + surface + source rect [captureGameScreen] needs to issue its PixelCopy. */
    private data class CaptureTarget(val bitmap: Bitmap, val surfaceView: SurfaceView, val rect: Rect)

    /**
     * Resolves the game's aspect ratio from the platform config (matching the PiP ratio), or
     * [DEFAULT_ASPECT_RATIO] when no context is set yet or `RevengerApplication.appConfig` isn't
     * initialized.
     */
    private fun resolveGameAspectRatio(): Float {
        if (!contextConfigured) {
            Log.w(TAG, "Context not set, using default aspect ratio")
            return DEFAULT_ASPECT_RATIO
        }
        return try {
            val platformId = com.vinaooo.revenger.RevengerApplication.appConfig.getPlatformId()
            val pipProfile = com.vinaooo.revenger.repositories.PipConfigRepository.getProfile(platformId)
            val aspectRatio = pipProfile.ratioW.toFloat() / pipProfile.ratioH.toFloat()
            Log.d(TAG, "Platform: $platformId, Aspect ratio: $aspectRatio")
            aspectRatio
            // RevengerApplication.appConfig is a lateinit var; accessing it before
            // Application.onCreate() completes throws UninitializedPropertyAccessException.
        } catch (e: UninitializedPropertyAccessException) {
            Log.w(TAG, "Could not determine aspect ratio", e)
            DEFAULT_ASPECT_RATIO
        }
    }

    /** Logs [message] (as a warning, or an error with [bitmapToRecycle] recycled) and notifies [callback] of failure. */
    private fun rejectCapture(
            message: String,
            callback: (Bitmap?) -> Unit,
            isError: Boolean = false,
            bitmapToRecycle: Bitmap? = null
    ): Nothing? {
        if (isError) Log.e(TAG, message) else Log.w(TAG, message)
        bitmapToRecycle?.recycle()
        callback(null)
        return null
    }

    /** Computes the game content rect (excluding black bars), or null -- rejecting -- if invalid. */
    private fun resolveCaptureRect(width: Int, height: Int, callback: (Bitmap?) -> Unit): Rect? {
        if (width <= 0 || height <= 0) {
            return rejectCapture("Invalid view dimensions: ${width}x$height", callback)
        }

        val gameAspectRatio = resolveGameAspectRatio()
        val gameRect = ScreenshotGeometry.calculateGameContentRect(width, height, gameAspectRatio)
        Log.d(TAG, "View: ${width}x$height, Game aspect: $gameAspectRatio, Content rect: $gameRect")

        return if (gameRect.width() <= 0 || gameRect.height() <= 0) {
            rejectCapture("Invalid cropped dimensions: ${gameRect.width()}x${gameRect.height()}", callback)
        } else {
            gameRect
        }
    }

    /** Allocates the capture bitmap and validates the source surface, or null -- rejecting -- if invalid. */
    private fun prepareCaptureTarget(
            glRetroView: GLRetroView,
            gameRect: Rect,
            callback: (Bitmap?) -> Unit
    ): CaptureTarget? {
        val bitmap = Bitmap.createBitmap(gameRect.width(), gameRect.height(), Bitmap.Config.ARGB_8888)
        // GLRetroView extends GLSurfaceView which extends SurfaceView
        val surfaceView = glRetroView as SurfaceView

        if (!surfaceView.holder.surface.isValid) {
            return rejectCapture(
                    "Surface is not valid for capture",
                    callback,
                    isError = true,
                    bitmapToRecycle = bitmap
            )
        }
        return CaptureTarget(bitmap, surfaceView, gameRect)
    }

    /** Handles the async [PixelCopy.request] result for [captureGameScreen]. */
    private fun onGameScreenCopyResult(
            copyResult: Int,
            target: CaptureTarget,
            viewWidth: Int,
            viewHeight: Int,
            callback: (Bitmap?) -> Unit
    ) {
        if (copyResult != PixelCopy.SUCCESS) {
            Log.e(TAG, "PixelCopy failed with result: $copyResult")
            target.bitmap.recycle()
            callback(null)
            return
        }
        Log.d(
                TAG,
                "Screenshot captured: ${target.rect.width()}x${target.rect.height()} " +
                        "(cropped from ${viewWidth}x$viewHeight)"
        )
        // Auto-crop any remaining black borders from the core output
        val autoCropped = ScreenshotGeometry.autoCropBlackBorders(target.bitmap)
        if (autoCropped !== target.bitmap) {
            target.bitmap.recycle()
        }
        callback(autoCropped)
    }

    /**
     * Capture a full-screen screenshot of the GLRetroView WITHOUT cropping.
     * Includes black bars (letterbox/pillarbox) so the image matches the screen pixel-perfectly
     * when displayed with fitXY + match_parent. Used for load preview overlay.
     *
     * @param glRetroView The GLRetroView instance to capture
     * @param callback Called with the captured Bitmap or null on failure
     */
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
            pixelCopier.copy(surfaceView, null, bitmap) { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    Log.d(TAG, "Full screenshot captured: ${width}x$height")
                    callback(bitmap)
                } else {
                    Log.e(TAG, "Full PixelCopy failed with result: $copyResult")
                    bitmap.recycle()
                    callback(null)
                }
            }
            // Same rationale as captureGameScreen(): PixelCopy.request()'s only documented
            // failure not already ruled out by the checks above is IllegalArgumentException.
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to capture full screenshot", e)
            callback(null)
        }
    }

}

/** The PixelCopy request [ScreenshotCaptureUtil] makes, behind an interface so tests can fake it. */
internal fun interface PixelCopier {
    /**
     * Copies [source] (only [srcRect] of it, or the whole surface when null) into [dest], then
     * reports the [PixelCopy] result code to [onResult] on the main thread.
     */
    fun copy(source: SurfaceView, srcRect: Rect?, dest: Bitmap, onResult: (Int) -> Unit)
}

/** [PixelCopier] over the real [PixelCopy] API, delivering results on the main looper. */
internal object SystemPixelCopier : PixelCopier {
    override fun copy(source: SurfaceView, srcRect: Rect?, dest: Bitmap, onResult: (Int) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        if (srcRect == null) {
            PixelCopy.request(source, dest, { onResult(it) }, handler)
        } else {
            PixelCopy.request(source, srcRect, dest, { onResult(it) }, handler)
        }
    }
}
