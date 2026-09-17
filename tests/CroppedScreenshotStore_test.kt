package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import com.swordfish.libretrodroid.GLRetroView
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CroppedScreenshotStore] was split out of [ScreenshotCaptureUtil] purely to keep that object
 * under the project's function-count threshold, taking over the menu-open cropped/full screenshot
 * cache (slot thumbnails + load-preview overlay).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CroppedScreenshotStore_test {

    private fun newStore(
        captureGameScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback -> callback(null) },
        captureFullScreen: (GLRetroView, (Bitmap?) -> Unit) -> Unit = { _, callback -> callback(null) }
    ) = CroppedScreenshotStore(captureGameScreen, captureFullScreen)

    @Test
    fun `getCachedScreenshot e getCachedFullScreenshot comecam nulos`() {
        val store = newStore()

        assertNull(store.getCachedScreenshot())
        assertNull(store.getCachedFullScreenshot())
        assertFalse(store.hasCachedScreenshot())
    }

    @Test
    fun `captureAndCacheScreenshot armazena separadamente o recortado e o completo`() {
        val cropped = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val full = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val store =
            newStore(
                captureGameScreen = { _, callback -> callback(cropped) },
                captureFullScreen = { _, callback -> callback(full) }
            )

        store.captureAndCacheScreenshot(mockk(relaxed = true))

        assertSame(cropped, store.getCachedScreenshot())
        assertSame(full, store.getCachedFullScreenshot())
        assertTrue(store.hasCachedScreenshot())
    }

    @Test
    fun `captureAndCacheScreenshot invoca o callback com sucesso quando ao menos um bitmap e capturado`() {
        val cropped = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val store =
            newStore(
                captureGameScreen = { _, callback -> callback(cropped) },
                captureFullScreen = { _, callback -> callback(null) }
            )
        var result: Boolean? = null

        store.captureAndCacheScreenshot(mockk(relaxed = true)) { success -> result = success }

        assertEquals(true, result)
    }

    @Test
    fun `captureAndCacheScreenshot invoca o callback com falha quando nenhum bitmap e capturado`() {
        val store = newStore()
        var result: Boolean? = null

        store.captureAndCacheScreenshot(mockk(relaxed = true)) { success -> result = success }

        assertEquals(false, result)
    }

    @Test
    fun `setManualScreenshots sobrescreve os bitmaps em cache`() {
        val store = newStore()
        val cropped = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val full = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        store.setManualScreenshots(cropped, full)

        assertSame(cropped, store.getCachedScreenshot())
        assertSame(full, store.getCachedFullScreenshot())
    }

    @Test
    fun `clearCachedScreenshot limpa ambos os bitmaps em cache`() {
        val store = newStore()
        store.setManualScreenshots(
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        )

        store.clearCachedScreenshot()

        assertNull(store.getCachedScreenshot())
        assertNull(store.getCachedFullScreenshot())
        assertFalse(store.hasCachedScreenshot())
    }
}
