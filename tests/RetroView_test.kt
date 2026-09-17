package com.vinaooo.revenger.retroview

import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [RetroView]'s ROM-loading failure path.
 *
 * Regression test for the narrowed catch around `context.assets.open("rom/$romName")`: it must
 * rethrow as [IllegalArgumentException] with the original [java.io.FileNotFoundException]
 * attached as [Throwable.cause], instead of losing it (the bug the SwallowedException finding
 * flagged).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroView_test {

    @Test
    fun `construir RetroView com rom ausente nos assets lanca IllegalArgumentException com a causa original`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appConfig = mockk<AppConfig>()
        every { appConfig.getRomName() } returns "does_not_exist.rom"

        val exception =
                assertThrowsIllegalArgumentException {
                    RetroView(context, CoroutineScope(Dispatchers.Unconfined), appConfig)
                }

        assertTrue(exception.message?.contains("does_not_exist.rom") == true)
        assertTrue(exception.cause is java.io.FileNotFoundException)
    }

    private fun assertThrowsIllegalArgumentException(block: () -> Unit): IllegalArgumentException {
        try {
            block()
        } catch (e: IllegalArgumentException) {
            return e
        }
        throw AssertionError("Expected IllegalArgumentException was not thrown")
    }
}
