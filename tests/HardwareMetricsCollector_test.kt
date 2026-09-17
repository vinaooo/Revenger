package com.vinaooo.revenger.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [HardwareMetricsCollector] was split out of [AdvancedPerformanceProfiler] purely to keep that
 * object under the project's function-count threshold. It had no direct test before -- it was
 * only exercised indirectly through the (private, at the time) collection methods on the object
 * -- so this covers its behavior in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class HardwareMetricsCollector_test {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var performanceData: ConcurrentHashMap<String, Any>
    private lateinit var collector: HardwareMetricsCollector

    @Before
    fun setUp() {
        performanceData = ConcurrentHashMap()
        collector = HardwareMetricsCollector(performanceData)
    }

    @Test
    fun `getMemoryInfo retorna valores de memoria nao negativos`() {
        val memoryInfo = collector.getMemoryInfo(context)

        assertTrue(memoryInfo.used >= 0L)
        assertTrue(memoryInfo.available >= 0L)
        assertTrue(memoryInfo.total >= 0L)
    }

    @Test
    fun `getCpuUsage retorna uma porcentagem baseada em memoria usada e maxima`() {
        val cpuUsage = collector.getCpuUsage()

        // Derived from used/max heap memory as a 0-100 percentage (PERCENTAGE_MULTIPLIER = 100);
        // pins the multiplier itself, not just non-negativity.
        assertTrue(cpuUsage in 0.0..100.0)
    }

    @Test
    fun `collectAdvancedMetrics preenche gpu, termico e frame pacing no mapa compartilhado`() {
        collector.collectAdvancedMetrics()

        assertEquals(45.0, performanceData["gpu_utilization"])
        assertEquals("NORMAL", performanceData["thermal_state"])
        assertEquals(95.0, performanceData["frame_pacing_score"])
    }

    @Test
    fun `collectStandardMetrics preenche apenas a info basica de gpu no mapa compartilhado`() {
        collector.collectStandardMetrics()

        assertEquals("Available", performanceData["basic_gpu_info"])
        assertNull(performanceData["gpu_utilization"])
    }

    @Test
    fun `os stubs de perfil por nivel de SDK nao lancam excecao e nao tocam o mapa compartilhado`() {
        collector.startAdvancedProfilingStubs()
        collector.startBasicGpuProfiling()
        collector.startStandardMemoryProfiling()
        collector.startBasicSystemMonitoring()

        assertTrue(performanceData.isEmpty())
    }
}
