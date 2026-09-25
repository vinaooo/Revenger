package com.vinaooo.revenger.utils

import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.repositories.PipConfigRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the `setContext()` gate in [ScreenshotCaptureUtil]'s aspect-ratio lookup. The object
 * now keeps only a "configured" flag instead of the Context (lint StaticFieldLeak), so these pin
 * down that the gate still behaves the same: the default ratio before `setContext()`, the
 * configured platform's PiP ratio after. The flag is reset by reflection between tests, since the
 * object is process-wide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ScreenshotCaptureUtilAspectRatio_test {

    private val flagField =
            ScreenshotCaptureUtil::class.java.getDeclaredField("contextConfigured").apply {
                isAccessible = true
            }

    @Before
    fun setUp() {
        flagField.setBoolean(ScreenshotCaptureUtil, false)
    }

    @After
    fun tearDown() {
        flagField.setBoolean(ScreenshotCaptureUtil, false)
    }

    private fun resolveGameAspectRatio(): Float {
        val method = ScreenshotCaptureUtil::class.java.getDeclaredMethod("resolveGameAspectRatio")
        method.isAccessible = true
        return method.invoke(ScreenshotCaptureUtil) as Float
    }

    @Test
    fun `sem setContext usa a proporcao padrao 4 por 3`() {
        assertEquals(4f / 3f, resolveGameAspectRatio(), 0.0001f)
    }

    @Test
    fun `depois de setContext usa a proporcao PiP da plataforma configurada`() {
        ScreenshotCaptureUtil.setContext(ApplicationProvider.getApplicationContext())

        val profile = PipConfigRepository.getProfile(RevengerApplication.appConfig.getPlatformId())
        assertEquals(profile.ratioW.toFloat() / profile.ratioH.toFloat(), resolveGameAspectRatio(), 0.0001f)
    }
}
