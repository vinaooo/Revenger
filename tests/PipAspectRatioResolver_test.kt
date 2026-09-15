package com.vinaooo.revenger.utils

import android.util.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipAspectRatioResolver_test {

    @Test
    fun `platform ratio within allowed range wins regardless of container dimensions`() {
        val result = PipAspectRatioResolver.resolve(
            ratioW = 16,
            ratioH = 9,
            containerWidth = 0,
            containerHeight = 0
        )

        assertEquals(Rational(16, 9), result)
    }

    @Test
    fun `platform ratio outside range falls back to in-range container ratio`() {
        val result = PipAspectRatioResolver.resolve(
            ratioW = 10,
            ratioH = 1,
            containerWidth = 1920,
            containerHeight = 1080
        )

        assertEquals(Rational(1920, 1080), result)
    }

    @Test
    fun `platform ratio and container ratio both outside range return null`() {
        val result = PipAspectRatioResolver.resolve(
            ratioW = 10,
            ratioH = 1,
            containerWidth = 10,
            containerHeight = 1
        )

        assertNull(result)
    }

    @Test
    fun `platform ratio outside range with zero-size container returns null`() {
        val result = PipAspectRatioResolver.resolve(
            ratioW = 10,
            ratioH = 1,
            containerWidth = 0,
            containerHeight = 0
        )

        assertNull(result)
    }

    @Test
    fun `platform ratio outside range with negative-size container returns null`() {
        val result = PipAspectRatioResolver.resolve(
            ratioW = 10,
            ratioH = 1,
            containerWidth = -1,
            containerHeight = -1
        )

        assertNull(result)
    }

    @Test
    fun `platform ratio exactly at the minimum boundary is included`() {
        // 209/500 == 0.418f exactly - pin the float value itself so this test actually
        // exercises the boundary rather than merely landing near it.
        assertEquals(0.418f, Rational(209, 500).toFloat(), 0f)

        val result = PipAspectRatioResolver.resolve(
            ratioW = 209,
            ratioH = 500,
            containerWidth = 0,
            containerHeight = 0
        )

        assertEquals(Rational(209, 500), result)
    }

    @Test
    fun `platform ratio exactly at the maximum boundary is included`() {
        // 239/100 == 2.39f exactly - pin the float value itself so this test actually
        // exercises the boundary rather than merely landing near it.
        assertEquals(2.39f, Rational(239, 100).toFloat(), 0f)

        val result = PipAspectRatioResolver.resolve(
            ratioW = 239,
            ratioH = 100,
            containerWidth = 0,
            containerHeight = 0
        )

        assertEquals(Rational(239, 100), result)
    }

    @Test
    fun `platform ratio just below the minimum boundary is excluded`() {
        // 417/1000 == 0.417f, just under the 0.418f minimum.
        val result = PipAspectRatioResolver.resolve(
            ratioW = 417,
            ratioH = 1000,
            containerWidth = 0,
            containerHeight = 0
        )

        assertNull(result)
    }

    @Test
    fun `platform ratio just above the maximum boundary is excluded`() {
        // 2391/1000 == 2.391f, just over the 2.39f maximum.
        val result = PipAspectRatioResolver.resolve(
            ratioW = 2391,
            ratioH = 1000,
            containerWidth = 0,
            containerHeight = 0
        )

        assertNull(result)
    }
}
