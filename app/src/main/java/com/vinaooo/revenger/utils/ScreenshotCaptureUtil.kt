package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import com.vinaooo.revenger.retroview.RetroView

/** Utilitário para capturar screenshot do RetroView. */
object ScreenshotCaptureUtil {
    /**
     * Captura o screenshot atual do RetroView.
     * @param retroView Instância do RetroView
     * @return Bitmap do screenshot ou null se falhar
     */
    fun captureScreenshot(retroView: RetroView?): Bitmap? {
        return retroView?.getBitmapScreenshot()
    }
}
