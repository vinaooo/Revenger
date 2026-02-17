package com.vinaooo.revenger.utils

import com.vinaooo.revenger.retroview.IRetroView
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ScreenshotCaptureUtil_test {

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `captureGameScreen with IRetroView calls captureScreenshot fallback`() {
        val iRetroView = mockk<IRetroView>(relaxed = true)
        val fakeView = mockk<android.view.View>()
        every { iRetroView.view } returns fakeView

        every { iRetroView.captureScreenshot(any()) } answers {
            val cb = firstArg<(android.graphics.Bitmap?) -> Unit>()
            cb(null)
        }

        ScreenshotCaptureUtil.captureGameScreen(iRetroView) { /* no-op */ }

        verify { iRetroView.captureScreenshot(any()) }
    }
}
