package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.PixelCopy
import android.view.SurfaceView
import com.swordfish.libretrodroid.GLRetroView
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ScreenshotCaptureUtil]'s capture paths with a fake [PixelCopier]: the checks before the copy,
 * which rect is copied, what reaches the callback for each copy result, and which bitmaps get
 * recycled. The crop math itself is `ScreenshotGeometry_test`'s. Without `setContext` the default
 * 4:3 ratio applies, so an 800x480 view has a 640x480 game area at x=80.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScreenshotCaptureUtilCapture_test {

    /** What the fake copier was asked, and what it does to the destination bitmap. */
    private var copiedRect: Rect? = null
    private var copiedInto: Bitmap? = null
    private var copyCalls = 0
    private var result = PixelCopy.SUCCESS
    private var paint: (Bitmap) -> Unit = { it.eraseColor(Color.RED) }

    private var delivered: Bitmap? = null
    private var callbacks = 0

    @Before
    fun setUp() {
        ScreenshotCaptureUtil.pixelCopier = PixelCopier { _, srcRect, dest, onResult ->
            copyCalls++
            copiedRect = srcRect
            copiedInto = dest
            paint(dest)
            onResult(result)
        }
    }

    @After
    fun tearDown() {
        ScreenshotCaptureUtil.pixelCopier = SystemPixelCopier
    }

    private fun retroView(width: Int = 800, height: Int = 480, surfaceValid: Boolean = true): GLRetroView =
            mockk(relaxed = true) {
                every { this@mockk.width } returns width
                every { this@mockk.height } returns height
                every { holder.surface.isValid } returns surfaceValid
            }

    private val callback: (Bitmap?) -> Unit = {
        callbacks++
        delivered = it
    }

    // --- captureGameScreen ---

    @Test
    fun `captura do jogo copia so a area do jogo e entrega o bitmap`() {
        ScreenshotCaptureUtil.captureGameScreen(retroView(), callback)

        assertEquals(Rect(80, 0, 720, 480), copiedRect)
        assertEquals(1, callbacks)
        assertSame(copiedInto, delivered)
        assertEquals(640, delivered?.width)
        assertEquals(480, delivered?.height)
    }

    @Test
    fun `captura do jogo corta bordas pretas e recicla o bitmap original`() {
        // A 10px black frame around red content. setPixels, not Canvas: Robolectric's default
        // graphics mode doesn't rasterize Canvas draws into the bitmap.
        paint = { bitmap ->
            bitmap.eraseColor(Color.BLACK)
            val width = bitmap.width - 20
            val height = bitmap.height - 20
            bitmap.setPixels(IntArray(width * height) { Color.RED }, 0, width, 10, 10, width, height)
        }

        ScreenshotCaptureUtil.captureGameScreen(retroView(), callback)

        val cropped = requireNotNull(delivered)
        assertTrue(cropped.width < 640 && cropped.height < 480)
        assertTrue(requireNotNull(copiedInto).isRecycled)
        assertFalse(cropped.isRecycled)
    }

    @Test
    fun `captura do jogo com view sem tamanho falha sem copiar`() {
        ScreenshotCaptureUtil.captureGameScreen(retroView(width = 0), callback)

        assertEquals(0, copyCalls)
        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura do jogo com superficie invalida falha sem copiar`() {
        ScreenshotCaptureUtil.captureGameScreen(retroView(surfaceValid = false), callback)

        assertEquals(0, copyCalls)
        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura do jogo com PixelCopy falho entrega null e recicla o bitmap`() {
        result = PixelCopy.ERROR_SOURCE_NO_DATA

        ScreenshotCaptureUtil.captureGameScreen(retroView(), callback)

        assertEquals(1, callbacks)
        assertNull(delivered)
        assertTrue(requireNotNull(copiedInto).isRecycled)
    }

    @Test
    fun `captura do jogo com IllegalArgumentException do PixelCopy entrega null`() {
        ScreenshotCaptureUtil.pixelCopier = PixelCopier { _, _, _, _ -> throw IllegalArgumentException("not attached") }

        ScreenshotCaptureUtil.captureGameScreen(retroView(), callback)

        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    // --- captureFullScreen ---

    @Test
    fun `captura da tela inteira copia a superficie toda sem cortar`() {
        paint = { it.eraseColor(Color.BLACK) }

        ScreenshotCaptureUtil.captureFullScreen(retroView(), callback)

        assertNull(copiedRect)
        assertEquals(1, copyCalls)
        assertSame(copiedInto, delivered)
        assertEquals(800, delivered?.width)
        assertEquals(480, delivered?.height)
    }

    @Test
    fun `captura da tela inteira com view sem tamanho ou superficie invalida falha sem copiar`() {
        ScreenshotCaptureUtil.captureFullScreen(retroView(height = 0), callback)
        ScreenshotCaptureUtil.captureFullScreen(retroView(surfaceValid = false), callback)

        assertEquals(0, copyCalls)
        assertEquals(2, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura da tela inteira com PixelCopy falho entrega null e recicla o bitmap`() {
        result = PixelCopy.ERROR_TIMEOUT

        ScreenshotCaptureUtil.captureFullScreen(retroView(), callback)

        assertEquals(1, callbacks)
        assertNull(delivered)
        assertTrue(requireNotNull(copiedInto).isRecycled)
    }

    @Test
    fun `captura da tela inteira com IllegalArgumentException entrega null`() {
        ScreenshotCaptureUtil.pixelCopier = PixelCopier { _, _, _, _ -> throw IllegalArgumentException("not attached") }

        ScreenshotCaptureUtil.captureFullScreen(retroView(), callback)

        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura do jogo com altura zero falha sem copiar`() {
        ScreenshotCaptureUtil.captureGameScreen(retroView(height = 0), callback)

        assertEquals(0, copyCalls)
        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura do jogo numa view estreita demais para a area do jogo falha sem copiar`() {
        // 1px de largura em 4:3: a area do jogo teria 0px de altura.
        ScreenshotCaptureUtil.captureGameScreen(retroView(width = 1), callback)

        assertEquals(0, copyCalls)
        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    @Test
    fun `captura da tela inteira com largura zero falha sem copiar`() {
        ScreenshotCaptureUtil.captureFullScreen(retroView(width = 0), callback)

        assertEquals(0, copyCalls)
        assertEquals(1, callbacks)
        assertNull(delivered)
    }

    // --- stores ligados ao ScreenshotCaptureUtil ---

    @Test
    fun `capturePipFrame guarda a tela inteira como frame do PiP`() {
        try {
            ScreenshotCaptureUtil.capturePipFrame(retroView(), force = true)

            assertEquals(1, copyCalls)
            assertNull(copiedRect)
            assertEquals(800, ScreenshotCaptureUtil.getPipFrame()?.width)
        } finally {
            ScreenshotCaptureUtil.updatePipFrame(null)
        }
    }

    @Test
    fun `captureAndCacheScreenshot guarda o recorte do jogo e a tela inteira`() {
        var captured: Boolean? = null
        try {
            ScreenshotCaptureUtil.captureAndCacheScreenshot(retroView()) { captured = it }

            assertEquals(2, copyCalls)
            assertEquals(640, ScreenshotCaptureUtil.getCachedScreenshot()?.width)
            assertEquals(800, ScreenshotCaptureUtil.getCachedFullScreenshot()?.width)
            assertEquals(true, captured)
        } finally {
            ScreenshotCaptureUtil.clearCachedScreenshot()
        }
    }

    // --- bitmap alocado e descartado quando a superficie e invalida ---

    /** Runs [block] recording every bitmap `Bitmap.createBitmap(w, h, config)` allocates. */
    private fun recordingAllocations(block: () -> Unit): List<Bitmap> {
        val allocated = mutableListOf<Bitmap>()
        mockkStatic(Bitmap::class)
        try {
            every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } answers {
                (callOriginal() as Bitmap).also { allocated += it }
            }
            block()
        } finally {
            unmockkStatic(Bitmap::class)
        }
        return allocated
    }

    @Test
    fun `captura do jogo com superficie invalida recicla o bitmap que alocou`() {
        val allocated = recordingAllocations {
            ScreenshotCaptureUtil.captureGameScreen(retroView(surfaceValid = false), callback)
        }

        assertEquals(1, allocated.size)
        assertTrue(allocated.single().isRecycled)
    }

    @Test
    fun `captura da tela inteira com superficie invalida recicla o bitmap que alocou`() {
        val allocated = recordingAllocations {
            ScreenshotCaptureUtil.captureFullScreen(retroView(surfaceValid = false), callback)
        }

        assertEquals(1, allocated.size)
        assertTrue(allocated.single().isRecycled)
    }

    // --- SystemPixelCopier ---

    @Test
    fun `SystemPixelCopier com retangulo copia so aquele retangulo`() {
        val source = mockk<SurfaceView>()
        val dest = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val rect = Rect(1, 2, 3, 4)
        mockkStatic(PixelCopy::class)
        try {
            every {
                PixelCopy.request(any<SurfaceView>(), any<Rect>(), any(), any(), any())
            } just Runs
            every { PixelCopy.request(any<SurfaceView>(), any<Bitmap>(), any(), any()) } just Runs

            SystemPixelCopier.copy(source, rect, dest) {}

            verify(exactly = 1) { PixelCopy.request(source, rect, dest, any(), any()) }
            verify(exactly = 0) { PixelCopy.request(any<SurfaceView>(), any<Bitmap>(), any(), any()) }
        } finally {
            unmockkStatic(PixelCopy::class)
        }
    }

    @Test
    fun `SystemPixelCopier sem retangulo copia a superficie inteira`() {
        val source = mockk<SurfaceView>()
        val dest = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        mockkStatic(PixelCopy::class)
        try {
            every {
                PixelCopy.request(any<SurfaceView>(), any<Rect>(), any(), any(), any())
            } just Runs
            every { PixelCopy.request(any<SurfaceView>(), any<Bitmap>(), any(), any()) } just Runs

            SystemPixelCopier.copy(source, null, dest) {}

            verify(exactly = 1) { PixelCopy.request(source, dest, any(), any()) }
            verify(exactly = 0) {
                PixelCopy.request(any<SurfaceView>(), any<Rect>(), any(), any(), any())
            }
        } finally {
            unmockkStatic(PixelCopy::class)
        }
    }
}
