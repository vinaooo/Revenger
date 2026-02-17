package com.vinaooo.revenger.retroview

import android.graphics.Bitmap
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class GLRetroViewAdapter_test {

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setFrameSpeed sets underlying gl frameSpeed`() {
        val gl = mockk<GLRetroView>(relaxed = true)
        val adapter = GLRetroViewAdapter(gl)

        adapter.setFrameSpeed(2)

        verify { gl.frameSpeed = 2 }
    }

    @Test
    fun `captureScreenshot delegates to ScreenshotCaptureUtil`() {
        val gl = mockk<GLRetroView>(relaxed = true)
        val adapter = GLRetroViewAdapter(gl)

        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.captureGameScreen(gl, any()) } answers {
            val cb = secondArg<(Bitmap?) -> Unit>()
            cb(null)
        }

        adapter.captureScreenshot { /* callback */ }

        verify { ScreenshotCaptureUtil.captureGameScreen(gl, any()) }
        unmockkObject(ScreenshotCaptureUtil)
    }
}
