package com.vinaooo.revenger.viewmodels.menu

import android.graphics.Bitmap
import android.os.Build
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil

/** The load-preview-overlay and save-state-screenshot-capture surface. */
interface ScreenshotPreviewFacade {
    fun showLoadPreview(bitmap: Bitmap)
    fun hideLoadPreview()
    fun getCachedFullScreenshot(): Bitmap?
    fun captureScreenshotForSaveState(onCaptured: ((Boolean) -> Unit)? = null)
    fun getCachedScreenshot(): Bitmap?
    fun clearCachedScreenshot()
}

/**
 * Implementation of [ScreenshotPreviewFacade]. Every dependency is read lazily via a provider, not
 * captured at construction time, because `retroView`, `loadPreviewCallback` and
 * `suppressNextScreenshotCapture` are all mutated on the owning ViewModel after construction.
 */
class ScreenshotPreviewController(
        private val retroView: () -> RetroView?,
        private val loadPreviewCallback: () -> ((Bitmap?) -> Unit)?,
        private val isScreenshotCaptureSuppressed: () -> Boolean,
        private val clearScreenshotCaptureSuppression: () -> Unit
) : ScreenshotPreviewFacade {

    override fun showLoadPreview(bitmap: Bitmap) {
        loadPreviewCallback()?.invoke(bitmap)
    }

    override fun hideLoadPreview() {
        loadPreviewCallback()?.invoke(null)
    }

    override fun getCachedFullScreenshot(): Bitmap? = ScreenshotCaptureUtil.getCachedFullScreenshot()

    override fun captureScreenshotForSaveState(onCaptured: ((Boolean) -> Unit)?) {
        if (isScreenshotCaptureSuppressed()) {
            clearScreenshotCaptureSuppression()
            onCaptured?.invoke(true)
            return
        }

        retroView()?.view?.let { glRetroView ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ScreenshotCaptureUtil.captureAndCacheScreenshot(glRetroView, onCaptured)
                // Menu-open is a clean pause point with a valid surface -- refresh the PiP still too.
                ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = true)
            } else {
                onCaptured?.invoke(false)
            }
        }
                ?: onCaptured?.invoke(false)
    }

    override fun getCachedScreenshot(): Bitmap? = ScreenshotCaptureUtil.getCachedScreenshot()

    override fun clearCachedScreenshot() {
        ScreenshotCaptureUtil.clearCachedScreenshot()
    }
}
