package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipSnapshotSelector_test {

    private fun validBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    @Test
    fun `all suppliers return null - result is null`() {
        val result = PipSnapshotSelector.select({ null }, { null }, { null })

        assertNull(result)
    }

    @Test
    fun `first supplier hits - later suppliers are never called`() {
        val expected = validBitmap()
        var secondCalled = false
        var thirdCalled = false

        val result = PipSnapshotSelector.select(
            { expected },
            {
                secondCalled = true
                validBitmap()
            },
            {
                thirdCalled = true
                throw IllegalStateException("should never be invoked")
            }
        )

        assertEquals(expected, result)
        assertFalse(secondCalled)
        assertFalse(thirdCalled)
    }

    @Test
    fun `first supplier misses with null - second supplier wins`() {
        val expected = validBitmap()

        val result = PipSnapshotSelector.select({ null }, { expected })

        assertEquals(expected, result)
    }

    @Test
    fun `first supplier returns a recycled bitmap - falls through to next supplier`() {
        val recycled = validBitmap().apply { recycle() }
        val expected = validBitmap()

        val result = PipSnapshotSelector.select({ recycled }, { expected })

        assertTrue(recycled.isRecycled)
        assertEquals(expected, result)
    }

    @Test
    fun `all suppliers return recycled or null bitmaps - result is null`() {
        val recycled = validBitmap().apply { recycle() }

        val result = PipSnapshotSelector.select({ null }, { recycled }, { null })

        assertNull(result)
    }
}
