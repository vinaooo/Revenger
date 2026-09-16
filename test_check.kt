import com.swordfish.libretrodroid.GLRetroView

fun testAccessibility(view: GLRetroView) {
    view.preserveEGLContextOnPause = true
}

import android.opengl.GLSurfaceView
fun testRenderMode(view: GLRetroView) { view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY }
