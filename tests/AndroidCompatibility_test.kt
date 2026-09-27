package com.vinaooo.revenger.utils

import android.os.Build
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Tests for [AndroidCompatibility]'s SDK level thresholds, so a future accidental change to one of
 * the boundary values (API 31, and the named ANDROID_16_API_LEVEL = 36) fails a test instead of
 * passing silently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AndroidCompatibility_test {

    private fun setSdkInt(level: Int) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", level)
    }

    @After
    fun tearDown() {
        // Restore the Robolectric-configured SDK level so other tests in the same process are
        // unaffected by this file's overrides.
        setSdkInt(30)
    }

    @Test
    fun `isAndroid12Plus e falso abaixo da api 31`() {
        setSdkInt(30)
        assertFalse(AndroidCompatibility.isAndroid12Plus())
    }

    @Test
    fun `isAndroid12Plus e verdadeiro exatamente na api 31`() {
        setSdkInt(31)
        assertTrue(AndroidCompatibility.isAndroid12Plus())
    }

    @Test
    fun `isAndroid16Plus e falso abaixo da api 36`() {
        setSdkInt(35)
        assertFalse(AndroidCompatibility.isAndroid16Plus())
    }

    @Test
    fun `isAndroid16Plus e verdadeiro exatamente na api 36`() {
        setSdkInt(36)
        assertTrue(AndroidCompatibility.isAndroid16Plus())
    }
}
