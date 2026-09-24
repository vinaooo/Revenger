package com.vinaooo.revenger.viewmodels.menu

import android.graphics.Bitmap
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins [ScreenshotPreviewController]'s behavior directly, independent of
 * `GameActivityViewModel`: the load-preview callback plumbing, the `ScreenshotCaptureUtil`
 * delegation, and every real branch in `captureScreenshotForSaveState` (the suppression flag, and
 * the null-`retroView` short-circuit).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ScreenshotPreviewController_test {

    private var callback: ((Bitmap?) -> Unit)? = null
    private var currentRetroView: RetroView? = null
    private var suppressed = false
    private lateinit var controller: ScreenshotPreviewController

    @Before
    fun setUp() {
        callback = null
        currentRetroView = null
        suppressed = false
        controller =
                ScreenshotPreviewController(
                        retroView = { currentRetroView },
                        loadPreviewCallback = { callback },
                        isScreenshotCaptureSuppressed = { suppressed },
                        clearScreenshotCaptureSuppression = { suppressed = false }
                )
    }

    @After
    fun tearDown() {
        unmockkObject(ScreenshotCaptureUtil)
    }

    @Test
    fun `showLoadPreview invoca o callback com o bitmap`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        var received: Bitmap? = null
        callback = { received = it }

        controller.showLoadPreview(bitmap)

        assertSame(bitmap, received)
    }

    @Test
    fun `showLoadPreview nao lanca quando nao ha callback registrado`() {
        controller.showLoadPreview(mockk(relaxed = true))
    }

    @Test
    fun `hideLoadPreview invoca o callback com null`() {
        var received: Bitmap? = mockk(relaxed = true)
        callback = { received = it }

        controller.hideLoadPreview()

        assertNull(received)
    }

    @Test
    fun `getCachedFullScreenshot delega para ScreenshotCaptureUtil`() {
        mockkObject(ScreenshotCaptureUtil)
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { ScreenshotCaptureUtil.getCachedFullScreenshot() } returns bitmap

        assertSame(bitmap, controller.getCachedFullScreenshot())
    }

    @Test
    fun `getCachedScreenshot delega para ScreenshotCaptureUtil`() {
        mockkObject(ScreenshotCaptureUtil)
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { ScreenshotCaptureUtil.getCachedScreenshot() } returns bitmap

        assertSame(bitmap, controller.getCachedScreenshot())
    }

    @Test
    fun `clearCachedScreenshot delega para ScreenshotCaptureUtil`() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.clearCachedScreenshot() } just Runs

        controller.clearCachedScreenshot()

        verify(exactly = 1) { ScreenshotCaptureUtil.clearCachedScreenshot() }
    }

    /**
     * When capture is suppressed, it must report success without touching
     * `ScreenshotCaptureUtil`, and clear the suppression flag so the next capture goes through
     * normally.
     */
    @Test
    fun `captureScreenshotForSaveState suprimido reporta sucesso e limpa a supressao`() {
        mockkObject(ScreenshotCaptureUtil)
        suppressed = true
        var result: Boolean? = null

        controller.captureScreenshotForSaveState { result = it }

        assertTrue(result == true)
        assertFalse(suppressed)
        verify { ScreenshotCaptureUtil wasNot Called }
    }

    @Test
    fun `captureScreenshotForSaveState sem retroView reporta falha`() {
        mockkObject(ScreenshotCaptureUtil)
        currentRetroView = null
        var result: Boolean? = null

        controller.captureScreenshotForSaveState { result = it }

        assertTrue(result == false)
        verify { ScreenshotCaptureUtil wasNot Called }
    }

    @Test
    fun `captureScreenshotForSaveState com retroView captura e atualiza o frame do PiP`() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any()) } just Runs
        every { ScreenshotCaptureUtil.capturePipFrame(any(), any()) } just Runs
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView

        controller.captureScreenshotForSaveState()

        verify(exactly = 1) { ScreenshotCaptureUtil.captureAndCacheScreenshot(glRetroView, any()) }
        verify(exactly = 1) { ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = true) }
    }
}
