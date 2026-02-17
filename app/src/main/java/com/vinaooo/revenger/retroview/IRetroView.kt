package com.vinaooo.revenger.retroview

import android.graphics.Bitmap
import android.view.Surface
import android.view.View

/**
 * Abstração mínima para uma superfície de renderização do emulador.
 * POC: contrato reduzido para suportar GL (via adapter) e futuros backends (Vulkan stub).
 */
interface IRetroView {
    val view: View
    fun onCreate()
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun setFrameSpeed(speed: Int)
    fun setFrameRenderedListener(listener: (() -> Unit)?)
    fun captureScreenshot(callback: (Bitmap?) -> Unit)
    fun getSurface(): Surface?
}
