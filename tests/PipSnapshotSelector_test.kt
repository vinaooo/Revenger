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
                error("should never be invoked")
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
    fun `chosen non-null candidate is recycled - result is null, no fallthrough to next supplier`() {
        // Matches the original ?: chain: it short-circuits on the first non-null value, then
        // checks isRecycled ONCE on that single chosen candidate. A recycled candidate does not
        // send evaluation back to try another supplier - the overall result is simply null.
        val recycled = validBitmap().apply { recycle() }
        var nextSupplierCalled = false

        val result = PipSnapshotSelector.select(
            { recycled },
            {
                nextSupplierCalled = true
                validBitmap()
            }
        )

        assertTrue(recycled.isRecycled)
        assertNull(result)
        assertFalse(nextSupplierCalled)
    }

    @Test
    fun `a later non-null candidate is recycled - result is null, no fallthrough past it either`() {
        val recycled = validBitmap().apply { recycle() }
        var thirdSupplierCalled = false

        val result = PipSnapshotSelector.select(
            { null },
            { recycled },
            {
                thirdSupplierCalled = true
                validBitmap()
            }
        )

        assertNull(result)
        assertFalse(thirdSupplierCalled)
    }
}
