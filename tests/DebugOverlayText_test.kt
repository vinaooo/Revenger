package com.vinaooo.revenger.performance

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** The performance overlay's text ([DebugOverlayText]). */
class DebugOverlayText_test {

    private val measured = AdvancedPerformanceProfiler.FrameStats(averageFps = 59.94, averageFrameTimeMs = 16.683, droppedFrames = 3)

    @Test
    fun `com quadros medidos mostra FPS, tempo de quadro e perdidos`() {
        assertEquals(
                "FPS: 59.9\nFrame Time: 16.68ms\nDropped: 3\nMemory: 42MB\nCPU: 12.3%",
                DebugOverlayText.format(measured, memoryUsedMb = 42, cpuUsagePercent = 12.34, locale = Locale.US)
        )
    }

    @Test
    fun `sem quadros medidos mostra marcadores no lugar das metricas de quadro`() {
        val none = AdvancedPerformanceProfiler.FrameStats(averageFps = 0.0, averageFrameTimeMs = 0.0, droppedFrames = 0)

        assertEquals(
                "FPS: Collecting data...\nFrame Time: --\nDropped: --\nMemory: 7MB\nCPU: 0.0%",
                DebugOverlayText.format(none, memoryUsedMb = 7, cpuUsagePercent = 0.0, locale = Locale.US)
        )
    }

    @Test
    fun `numeros seguem o locale`() {
        val text = DebugOverlayText.format(measured, 1, 12.34, Locale.GERMANY)

        assertEquals("FPS: 59,9", text.lines().first())
        assertEquals("CPU: 12,3%", text.lines().last())
    }

    @Test
    fun `le memoria e CPU do mapa de dados e trata ausentes ou de outro tipo como zero`() {
        val data =
                mapOf<String, Any>(
                        DebugOverlayText.MEMORY_USED_MB_KEY to 42L,
                        DebugOverlayText.CPU_USAGE_PERCENT_KEY to 12.34,
                )

        assertEquals(DebugOverlayText.format(measured, 42, 12.34, Locale.US), DebugOverlayText.format(measured, data, Locale.US))
        assertEquals(
                DebugOverlayText.format(measured, 0, 0.0, Locale.US),
                DebugOverlayText.format(measured, mapOf(DebugOverlayText.MEMORY_USED_MB_KEY to "42"), Locale.US)
        )
    }
}
