package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [ScreenshotCaptureUtil.autoCropBlackBorders] (private), which detects and removes thin
 * black borders from a captured screenshot. Reached via reflection since the method is an
 * implementation detail of the public capture flow.
 *
 * These tests lock the named constants extracted from former magic numbers:
 * MAX_BORDER_CROP_RATIO (limits detection/cropping to 5% of each dimension, avoiding false
 * positives on legitimately dark content) and BLACK_BORDER_BRIGHTNESS_THRESHOLD (what counts as
 * "black" vs. real content). The RGB channel shift/mask constants are exercised indirectly: a
 * wrong shift or mask would corrupt the brightness sum and change which borders get detected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ScreenshotCaptureUtil_test {

    private fun autoCropBlackBorders(bitmap: Bitmap): Bitmap {
        val method =
            ScreenshotCaptureUtil::class.java.getDeclaredMethod(
                "autoCropBlackBorders",
                Bitmap::class.java
            )
        method.isAccessible = true
        return method.invoke(ScreenshotCaptureUtil, bitmap) as Bitmap
    }

    @Test
    fun `Robolectric bitmap suporta setPixel e getPixel (pre-condicao para os testes abaixo)`() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.WHITE)

        assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
    }

    @Test
    fun `bitmap sem bordas pretas e retornado sem cortes (mesma instancia)`() {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (x in 0 until 40) {
            for (y in 0 until 40) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        val result = autoCropBlackBorders(bitmap)

        assertSame(bitmap, result)
    }

    @Test
    fun `bordas pretas dentro do limite de 5 por cento sao detectadas e cortadas`() {
        // 200x200 bitmap: a 3px black frame around a white interior.
        // MAX_BORDER_CROP_RATIO (5%) of 200 is 10px, comfortably larger than the 3px border,
        // so the scan window can see past the border into the real content.
        val size = 200
        val borderThickness = 3
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val isBorder =
                    x < borderThickness ||
                        x >= size - borderThickness ||
                        y < borderThickness ||
                        y >= size - borderThickness
                bitmap.setPixel(x, y, if (isBorder) Color.BLACK else Color.WHITE)
            }
        }

        val result = autoCropBlackBorders(bitmap)

        val expectedSize = size - 2 * borderThickness
        assertTrue("Esperava um bitmap recortado (nova instancia)", result !== bitmap)
        assertEquals(expectedSize, result.width)
        assertEquals(expectedSize, result.height)
    }

    @Test
    fun `bordas mais grossas que o limite de 5 por cento nao sao cortadas (evita falso positivo)`() {
        // Border thicker than MAX_BORDER_CROP_RATIO's scan window: the whole scanned region is
        // black, so no content boundary is found and the bitmap must come back untouched.
        val size = 200
        val borderThickness = 30 // > 5% of 200 (10px)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val isBorder =
                    x < borderThickness ||
                        x >= size - borderThickness ||
                        y < borderThickness ||
                        y >= size - borderThickness
                bitmap.setPixel(x, y, if (isBorder) Color.BLACK else Color.WHITE)
            }
        }

        val result = autoCropBlackBorders(bitmap)

        assertSame(bitmap, result)
    }
}
