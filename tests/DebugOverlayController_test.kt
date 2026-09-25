package com.vinaooo.revenger.performance

import android.os.Handler
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [DebugOverlayController]'s update-runnable lifecycle. `startDebugOverlayUpdates` is
 * private and only reached through `showDebugOverlay` after a resource/config check, so it is
 * invoked by reflection; the [Handler] is a mock, so the posted runnable never actually runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DebugOverlayController_test {

    private lateinit var handler: Handler
    private lateinit var controller: DebugOverlayController

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
        controller =
                DebugOverlayController(
                        handler,
                        ConcurrentHashMap(),
                        mockk(relaxed = true),
                        isProfilingActive = { false }
                )
    }

    private fun startDebugOverlayUpdates() {
        val method = controller.javaClass.getDeclaredMethod("startDebugOverlayUpdates")
        method.isAccessible = true
        method.invoke(controller)
    }

    private fun storedRunnable(): Runnable? {
        val field = controller.javaClass.getDeclaredField("debugOverlayUpdateRunnable")
        field.isAccessible = true
        return field.get(controller) as Runnable?
    }

    @Test
    fun `startDebugOverlayUpdates posta o mesmo runnable que guarda`() {
        val posted = slot<Runnable>()

        startDebugOverlayUpdates()

        verify(exactly = 1) { handler.post(capture(posted)) }
        assertSame(posted.captured, storedRunnable())
    }

    @Test
    fun `hideDebugOverlay remove o runnable postado e limpa a referencia`() {
        val posted = slot<Runnable>()
        startDebugOverlayUpdates()
        verify { handler.post(capture(posted)) }

        controller.hideDebugOverlay()

        verify(exactly = 1) { handler.removeCallbacks(posted.captured) }
        assertNull(storedRunnable())
    }

    @Test
    fun `hideDebugOverlay sem updates iniciados nao remove callbacks`() {
        controller.hideDebugOverlay()

        verify(exactly = 0) { handler.removeCallbacks(any<Runnable>()) }
    }
}
