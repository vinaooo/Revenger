package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import com.swordfish.libretrodroid.GLRetroView
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * [PipFrameStore] was split out of [ScreenshotCaptureUtil] purely to keep that object under the
 * project's function-count threshold, taking over the Picture-in-Picture full-screen frame cache
 * (throttled capture, manual injection, clearing).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipFrameStore_test {

    @Before
    fun setUp() {
        // Robolectric's fake elapsedRealtime() starts at 0, same as the store's "never captured"
        // sentinel -- on a real device elapsedRealtime() is always large (time since boot), so
        // establish a realistic non-zero baseline here instead of leaving throttle math to compare
        // 0 against 0 on a store's very first call.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(60))
    }

    private fun newStore(
        captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback -> callback(null) },
        getCachedFullScreenshot: () -> Bitmap? = { null }
    ) = PipFrameStore(captureFullScreen, getCachedFullScreenshot)

    @Test
    fun `getPipFrame comeca nulo`() {
        val store = newStore()

        assertNull(store.getPipFrame())
    }

    @Test
    fun `updatePipFrame armazena o bitmap informado`() {
        val store = newStore()
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        store.updatePipFrame(bitmap)

        assertSame(bitmap, store.getPipFrame())
    }

    @Test
    fun `capturePipFrame usa o bitmap capturado quando a captura tem sucesso`() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val store = newStore(captureFullScreen = { _, callback -> callback(bitmap) })

        store.capturePipFrame(mockk(relaxed = true), force = true)

        assertSame(bitmap, store.getPipFrame())
    }

    @Test
    fun `capturePipFrame mantem o frame anterior quando a captura falha`() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val store = newStore(captureFullScreen = { _, callback -> callback(bitmap) })
        store.capturePipFrame(mockk(relaxed = true), force = true)

        val failingStore =
            PipFrameStore(
                captureFullScreen = { _, callback -> callback(null) },
                getCachedFullScreenshot = { null }
            )
        failingStore.updatePipFrame(bitmap)
        failingStore.capturePipFrame(mockk(relaxed = true), force = true)

        assertSame(bitmap, failingStore.getPipFrame())
    }

    @Test
    fun `capturePipFrame respeita o throttle quando force e falso`() {
        var captureCount = 0
        val store =
            newStore(
                captureFullScreen = { _, callback ->
                    captureCount++
                    callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            )

        store.capturePipFrame(mockk(relaxed = true), force = false)
        store.capturePipFrame(mockk(relaxed = true), force = false)

        assertEquals(1, captureCount)
    }

    @Test
    fun `capturePipFrame com force ignora o throttle`() {
        var captureCount = 0
        val store =
            newStore(
                captureFullScreen = { _, callback ->
                    captureCount++
                    callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            )

        store.capturePipFrame(mockk(relaxed = true), force = true)
        store.capturePipFrame(mockk(relaxed = true), force = true)

        assertEquals(2, captureCount)
    }

    @Test
    fun `capturePipFrame apos o intervalo minimo captura novamente mesmo sem force`() {
        var captureCount = 0
        val store =
            newStore(
                captureFullScreen = { _, callback ->
                    captureCount++
                    callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            )

        store.capturePipFrame(mockk(relaxed = true), force = false)
        ShadowSystemClock.advanceBy(Duration.ofMillis(2600))
        store.capturePipFrame(mockk(relaxed = true), force = false)

        assertEquals(2, captureCount)
    }

    @Test
    fun `promoteCachedFullToPipFrame copia o cache cheio para o frame de pip`() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val store = newStore(getCachedFullScreenshot = { source })

        store.promoteCachedFullToPipFrame()

        val promoted = store.getPipFrame()
        assertTrue(promoted !== null && promoted !== source)
    }

    @Test
    fun `promoteCachedFullToPipFrame nao faz nada quando nao ha cache cheio`() {
        val store = newStore(getCachedFullScreenshot = { null })

        store.promoteCachedFullToPipFrame()

        assertNull(store.getPipFrame())
    }

    @Test
    fun `promoteCachedFullToPipFrame nao faz nada quando o bitmap fonte esta reciclado`() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        source.recycle()
        val store = newStore(getCachedFullScreenshot = { source })

        store.promoteCachedFullToPipFrame()

        assertNull(store.getPipFrame())
    }

    @Test
    fun `clearPipFrame limpa o frame armazenado`() {
        val store = newStore()
        store.updatePipFrame(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888))

        store.clearPipFrame()

        assertNull(store.getPipFrame())
    }

    @Test
    fun `clearPipFrame permite uma nova captura imediata mesmo sem force`() {
        var captureCount = 0
        val store =
            newStore(
                captureFullScreen = { _, callback ->
                    captureCount++
                    callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            )
        store.capturePipFrame(mockk(relaxed = true), force = true)

        store.clearPipFrame()
        store.capturePipFrame(mockk(relaxed = true), force = false)

        assertEquals(2, captureCount)
    }
}
