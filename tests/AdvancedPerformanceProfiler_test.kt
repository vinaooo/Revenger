package com.vinaooo.revenger.performance

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [AdvancedPerformanceProfiler]'s pure frame-timing math: [AdvancedPerformanceProfiler.getFrameStats]
 * and [AdvancedPerformanceProfiler.onFrameRendered]. These lock the named constants extracted from
 * former magic numbers (NANOS_PER_MILLISECOND, MILLISECONDS_PER_SECOND, TARGET_FPS,
 * FPS_SMOOTHING_FACTOR) so a future accidental change to one of those values fails a test instead
 * of passing silently.
 *
 * [AdvancedPerformanceProfiler] is a singleton `object` with private mutable state
 * (frameTimeData/lastFrameTime/frameCount/emulatorFps); state is reset via reflection between
 * tests, following the pattern already used for other stateful singletons in this codebase (see
 * definitions/Code.md).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AdvancedPerformanceProfiler_test {

    private fun setPrivateField(name: String, value: Any?) {
        val field = AdvancedPerformanceProfiler::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(AdvancedPerformanceProfiler, value)
    }

    private fun getPrivateField(name: String): Any? {
        val field = AdvancedPerformanceProfiler::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(AdvancedPerformanceProfiler)
    }

    private fun resetProfilerState() {
        setPrivateField("frameTimeData", mutableListOf<Long>())
        setPrivateField("lastFrameTime", 0L)
        setPrivateField("frameCount", 0)
        setPrivateField("emulatorFps", 0.0)
        setPrivateField("isProfilingActive", false)
    }

    @Before
    fun setUp() {
        resetProfilerState()
    }

    @After
    fun tearDown() {
        resetProfilerState()
    }

    // ========== getFrameStats() ==========

    @Test
    fun `getFrameStats converte nanossegundos para milissegundos e calcula fps a partir da media`() {
        // 10ms and 20ms frames (in nanoseconds). No emulatorFps recorded yet, so fps must come
        // from 1000 / averageFrameTimeMs (MILLISECONDS_PER_SECOND / averageFrameTimeMs).
        AdvancedPerformanceProfiler.recordFrameTime(10_000_000L)
        AdvancedPerformanceProfiler.recordFrameTime(20_000_000L)

        val stats = AdvancedPerformanceProfiler.getFrameStats()

        assertEquals(15.0, stats.averageFrameTimeMs, 0.001)
        assertEquals(1000.0 / 15.0, stats.averageFps, 0.001)
    }

    @Test
    fun `getFrameStats conta como dropped apenas frames mais lentos que o alvo de 60fps (16,67ms)`() {
        // 10ms is comfortably under the 60fps target (~16.67ms) and must NOT be dropped.
        // 20ms is over that target but under a 30fps target (~33.33ms) — this specifically
        // pins TARGET_FPS at 60, not some other frame-rate target.
        AdvancedPerformanceProfiler.recordFrameTime(10_000_000L)
        AdvancedPerformanceProfiler.recordFrameTime(20_000_000L)

        val stats = AdvancedPerformanceProfiler.getFrameStats()

        assertEquals(1, stats.droppedFrames)
    }

    @Test
    fun `getFrameStats prefere o emulatorFps ja calculado quando disponivel`() {
        setPrivateField("emulatorFps", 42.0)
        AdvancedPerformanceProfiler.recordFrameTime(10_000_000L)

        val stats = AdvancedPerformanceProfiler.getFrameStats()

        assertEquals(42.0, stats.averageFps, 0.0001)
    }

    // ========== onFrameRendered() smoothing factor ==========

    @Test
    fun `onFrameRendered suaviza o fps do emulador com FPS_SMOOTHING_FACTOR igual a 0,1`() {
        // Force a known starting point: emulatorFps = 1000.0, last frame ~100ms ago.
        // instantFps ~= 1000/100 = 10.0
        // expected = emulatorFps * (1 - 0.1) + instantFps * 0.1 = 900 + 1 = 901
        // A tolerance of 1.0 absorbs the small timing drift between setting lastFrameTime and
        // the actual System.nanoTime() read inside onFrameRendered(), while still failing loudly
        // if the smoothing factor were something else (e.g. 0.2 -> expected ~= 802).
        setPrivateField("emulatorFps", 1000.0)
        setPrivateField("lastFrameTime", System.nanoTime() - 100_000_000L)

        AdvancedPerformanceProfiler.onFrameRendered()

        val emulatorFps = getPrivateField("emulatorFps") as Double
        assertEquals(901.0, emulatorFps, 1.0)
    }

    @Test
    fun `onFrameRendered nao calcula fps na primeira chamada (sem lastFrameTime anterior)`() {
        // lastFrameTime starts at 0L (reset in setUp), so the first call only records the
        // timestamp and must not touch emulatorFps.
        AdvancedPerformanceProfiler.onFrameRendered()

        val emulatorFps = getPrivateField("emulatorFps") as Double
        assertEquals(0.0, emulatorFps, 0.0001)

        val lastFrameTime = getPrivateField("lastFrameTime") as Long
        assertEquals(true, lastFrameTime > 0L)
    }
}
