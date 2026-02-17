package com.vinaooo.revenger.retroview

import android.graphics.Bitmap
import android.view.Surface
import android.view.View
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil

/** Adapter que expõe um GLRetroView através do contrato IRetroView (POC). */
class GLRetroViewAdapter(private val gl: GLRetroView) : IRetroView {
    override val view: View
        get() = gl

    override fun onCreate() {
        // GLRetroView lifecycle é gerenciado externamente; nada a fazer aqui para POC
    }

    override fun onResume() {
        gl.onResume()
    }

    override fun onPause() {
        gl.onPause()
    }

    override fun onDestroy() {
        // nenhum cleanup adicional no POC
    }

    override fun setFrameSpeed(speed: Int) {
        gl.frameSpeed = speed
    }

    override fun setFrameRenderedListener(listener: (() -> Unit)?) {
        // POC: no-op. RetroView continua a usar getGLRetroEvents() internamente.
        // Futuro: podemos coletar `gl.getGLRetroEvents()` aqui e invocar `listener`.
    }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        // Reutiliza utilitário existente que espera GLRetroView
        ScreenshotCaptureUtil.captureGameScreen(gl, callback)
    }

    override fun getSurface(): Surface? {
        return (gl as? View)?.let { v ->
            try {
                val holder = (v as? android.view.SurfaceView)?.holder
                holder?.surface
            } catch (_: Exception) {
                null
            }
        }
    }
}
