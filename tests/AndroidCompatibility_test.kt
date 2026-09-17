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
 * Tests for [AndroidCompatibility]'s SDK level thresholds. These lock the named constants
 * extracted from former magic numbers (ANDROID_14_API_LEVEL = 34, ANDROID_15_API_LEVEL = 35,
 * ANDROID_16_API_LEVEL = 36) so a future accidental change to one of those boundary values fails
 * a test instead of passing silently.
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
    fun `isAndroid14Plus e falso abaixo da api 34`() {
        setSdkInt(33)
        assertFalse(AndroidCompatibility.isAndroid14Plus())
    }

    @Test
    fun `isAndroid14Plus e verdadeiro exatamente na api 34`() {
        setSdkInt(34)
        assertTrue(AndroidCompatibility.isAndroid14Plus())
    }

    @Test
    fun `isAndroid15Plus e falso abaixo da api 35`() {
        setSdkInt(34)
        assertFalse(AndroidCompatibility.isAndroid15Plus())
    }

    @Test
    fun `isAndroid15Plus e verdadeiro exatamente na api 35`() {
        setSdkInt(35)
        assertTrue(AndroidCompatibility.isAndroid15Plus())
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
