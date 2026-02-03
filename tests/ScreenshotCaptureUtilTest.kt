package com.vinaooo.revenger.tests

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for ScreenshotCaptureUtil (basic bitmap operations) */
class ScreenshotCaptureUtilTest {

    @Test
    fun `bitmap creation with ARGB_8888 format`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)

        bitmap.recycle()
    }

    @Test
    fun `bitmap pixel manipulation`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        // Create a test pattern
        val pixels = IntArray(100)
        for (i in pixels.indices) {
            pixels[i] = 0xFF000000.toInt() // Black pixel
        }
        bitmap.setPixels(pixels, 0, 10, 0, 0, 10, 10)

        // Read back
        val readPixels = IntArray(100)
        bitmap.getPixels(readPixels, 0, 10, 0, 0, 10, 10)

        assertArrayEquals(pixels, readPixels)
        bitmap.recycle()
    }

    @Test
    fun `bitmap crop creation`() {
        val original = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val cropped = Bitmap.createBitmap(original, 50, 50, 100, 100)

        assertNotNull(cropped)
        assertEquals(100, cropped.width)
        assertEquals(100, cropped.height)

        original.recycle()
        cropped.recycle()
    }

    @Test
    fun `isNearBlack detection - test threshold logic`() {
        // Test pixel color values and threshold
        val blackPixel = 0xFF000000.toInt() // Pure black
        val nearBlackPixel = 0xFF050505.toInt() // Near black
        val whitePixel = 0xFFFFFFFF.toInt() // White

        // Extract color channels
        val blackR = (blackPixel shr 16) and 0xFF
        val blackG = (blackPixel shr 8) and 0xFF
        val blackB = blackPixel and 0xFF

        val nearBlackR = (nearBlackPixel shr 16) and 0xFF
        val nearBlackG = (nearBlackPixel shr 8) and 0xFF
        val nearBlackB = nearBlackPixel and 0xFF

        val whiteR = (whitePixel shr 16) and 0xFF
        val whiteG = (whitePixel shr 8) and 0xFF
        val whiteB = whitePixel and 0xFF

        val threshold = 10

        // Black should be below threshold
        assertTrue(blackR < threshold && blackG < threshold && blackB < threshold)

        // Near-black should be below threshold
        assertTrue(nearBlackR < threshold && nearBlackG < threshold && nearBlackB < threshold)

        // White should be above threshold
        assertFalse(whiteR < threshold && whiteG < threshold && whiteB < threshold)
    }

    @Test
    fun `bitmap compression - WebP format`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Bitmap.compress returns boolean indicating success
        val data = java.io.ByteArrayOutputStream()
        val success = bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, data)

        assertTrue(success)
        val compressedData = data.toByteArray()
        assertTrue(compressedData.isNotEmpty())

        bitmap.recycle()
    }
}
