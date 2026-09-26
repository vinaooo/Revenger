package com.vinaooo.revenger.controllers

import android.util.Log
import com.vinaooo.revenger.managers.AudioRoutingManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * [GameSessionTeardown] is `GameActivity.onDestroy()`'s ordered cleanup. These tests pin the
 * order, that parts never created are skipped, and that one failing step no longer skips the rest.
 */
class GameSessionTeardown_test {

    private val watcher = mockk<InputDeviceWatcher>(relaxed = true)
    private val audio = mockk<AudioRoutingManager>(relaxed = true)
    private val parts = mockk<GameSessionParts>(relaxed = true)
    private val failures = mutableListOf<Pair<String, Throwable>>()
    private val teardown = GameSessionTeardown { step, error -> failures += step to error }

    private fun withAllParts() {
        every { parts.inputDeviceWatcher } returns watcher
        every { parts.audioRoutingManager } returns audio
    }

    @Test
    fun `steps run in the onDestroy order`() {
        withAllParts()

        teardown.run(parts)

        verifyOrder {
            parts.disposeRotation()
            watcher.dispose()
            parts.disposePip()
            parts.disposeFloatingButton()
            parts.stopProfiling()
            parts.hideDebugOverlay()
            parts.disposeViewModel()
            parts.detachRetroView()
            parts.clearPipFrame()
            audio.abandonFocus()
        }
    }

    @Test
    fun `step names follow the same order`() {
        withAllParts()

        val names = GameSessionTeardown.stepsFor(parts).map { it.name }

        assertEquals(
            listOf(
                "rotation", "input devices", "pip", "floating button", "profiler",
                "debug overlay", "view model", "retro view", "pip frame", "audio focus"
            ),
            names
        )
    }

    @Test
    fun `audio focus is abandoned when the manager was created`() {
        withAllParts()

        teardown.run(parts)

        verify(exactly = 1) { audio.abandonFocus() }
    }

    @Test
    fun `audio focus and input devices are skipped when never created`() {
        every { parts.inputDeviceWatcher } returns null
        every { parts.audioRoutingManager } returns null

        val failed = teardown.run(parts)

        assertTrue(failed.isEmpty())
        verify(exactly = 1) { parts.clearPipFrame() }
    }

    /** Regression: the first failure used to skip every later step and `super.onDestroy()`. */
    @Test
    fun `a failing step does not skip the ones after it`() {
        withAllParts()
        val error = UninitializedPropertyAccessException("never created")
        every { parts.disposeRotation() } throws error

        val failed = teardown.run(parts)

        assertEquals(listOf("rotation"), failed)
        verify(exactly = 1) { watcher.dispose() }
        verify(exactly = 1) { parts.clearPipFrame() }
        verify(exactly = 1) { audio.abandonFocus() }
    }

    @Test
    fun `each failure is logged with its step name and error`() {
        withAllParts()
        val pipError = IllegalStateException("pip")
        val viewModelError = IllegalArgumentException("view model")
        every { parts.disposePip() } throws pipError
        every { parts.disposeViewModel() } throws viewModelError

        val failed = teardown.run(parts)

        assertEquals(listOf("pip", "view model"), failed)
        assertEquals(listOf("pip", "view model"), failures.map { it.first })
        assertSame(pipError, failures[0].second)
        assertSame(viewModelError, failures[1].second)
    }

    @Test
    fun `run never throws even when every step fails`() {
        withAllParts()
        every { parts.disposeRotation() } throws IllegalStateException("failed")
        every { watcher.dispose() } throws IllegalStateException("failed")
        every { parts.clearPipFrame() } throws IllegalStateException("failed")
        every { audio.abandonFocus() } throws IllegalStateException("failed")

        val failed = teardown.run(parts)

        assertEquals(listOf("rotation", "input devices", "pip frame", "audio focus"), failed)
    }

    @Test
    fun `a null step is skipped and never reported`() {
        val calls = mutableListOf<String>()

        val failed =
            teardown.run(
                listOf(
                    GameSessionTeardown.Step("first") { calls += "first" },
                    GameSessionTeardown.Step("skipped", null),
                    GameSessionTeardown.Step("last") { calls += "last" }
                )
            )

        assertEquals(listOf("first", "last"), calls)
        assertTrue(failed.isEmpty())
    }
}

/** The default logger, which needs `android.util.Log` (hence Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameSessionTeardownLogging_test {

    @Test
    fun `the default logger logs the failed step at error level`() {
        ShadowLog.clear()

        val failed =
            GameSessionTeardown().run(listOf(GameSessionTeardown.Step("boom") { error("boom") }))

        assertEquals(listOf("boom"), failed)
        val entry = ShadowLog.getLogsForTag("GameSessionTeardown").single()
        assertEquals(Log.ERROR, entry.type)
        assertEquals("Teardown step 'boom' failed", entry.msg)
    }
}
