package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.PixelCopy
import com.swordfish.libretrodroid.GLRetroView
import io.mockk.every
import io.mockk.mockk
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
}
