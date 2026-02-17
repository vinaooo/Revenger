package com.vinaooo.revenger.retroview

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * Stub mínimo para simular um backend Vulkan (POC).
 * NÃO implementa Vulkan — apenas fornece um `View`/`Surface` para testes de swap.
 */
class StubVkRetroView(context: Context) : SurfaceView(context), IRetroView {
    init {
        // SurfaceView default holder callbacks
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    override val view: View
        get() = this

    override fun onCreate() {
        // Stub - nada para criar
    }

    override fun onResume() { }
    override fun onPause() { }
    override fun onDestroy() { }

    override fun setFrameSpeed(speed: Int) { /* noop for stub */ }
    override fun setFrameRenderedListener(listener: (() -> Unit)?) { /* noop */ }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        // Não suportado no stub -> retorna null
        callback(null)
    }

    override fun getSurface(): Surface? = holder.surface
}
