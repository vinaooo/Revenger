package com.vinaooo.revenger.performance

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Direct tests for [FrameStatsTracker]'s frame-time buffer. The FPS math is also covered through
 * [AdvancedPerformanceProfiler] in `AdvancedPerformanceProfiler_test`; these pin the tracker's own
 * empty-state and ring-buffer behavior without going through the profiler singleton.
 */
class FrameStatsTracker_test {

    private lateinit var tracker: FrameStatsTracker

    @Before
    fun setUp() {
        tracker = FrameStatsTracker()
    }

    @Test
    fun `getFrameStats sem frames registrados retorna tudo zerado`() {
        val stats = tracker.getFrameStats()

        assertEquals(0.0, stats.averageFps, 0.0)
        assertEquals(0.0, stats.averageFrameTimeMs, 0.0)
        assertEquals(0, stats.droppedFrames)
    }

    @Test
    fun `recordFrameTime acumula frames e calcula a media em milissegundos`() {
        tracker.recordFrameTime(10_000_000L)
        tracker.recordFrameTime(30_000_000L)

        val stats = tracker.getFrameStats()

        assertEquals(20.0, stats.averageFrameTimeMs, 0.0001)
        assertEquals(50.0, stats.averageFps, 0.0001)
        assertEquals(1, stats.droppedFrames)
    }

    @Test
    fun `buffer guarda apenas os 120 frames mais recentes`() {
        // 120 slow frames first, then 120 fast frames: the slow ones must all be evicted.
        repeat(120) { tracker.recordFrameTime(40_000_000L) }
        repeat(120) { tracker.recordFrameTime(10_000_000L) }

        val stats = tracker.getFrameStats()

        assertEquals(10.0, stats.averageFrameTimeMs, 0.0001)
        assertEquals(0, stats.droppedFrames)
    }
}
