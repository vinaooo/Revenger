package com.vinaooo.revenger.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.utils.AndroidCompatibility
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [ProfilingSessionController] was split out of [AdvancedPerformanceProfiler] purely to keep that
 * object under the project's function-count threshold. It had no direct test before -- only
 * [AdvancedPerformanceProfiler.startProfiling]/[AdvancedPerformanceProfiler.stopProfiling] were
 * exercised, and only through reflection into the (then object-level) `isProfilingActive` field --
 * so this covers the session-active state machine in isolation.
 *
 * @Config(sdk = [30]) puts every case on the BASIC profiling path (below Android 12/S), which is
 * enough to exercise the active-flag transitions this class owns; the SDK-gated dispatch itself
 * lives in [HardwareMetricsCollector] and is not what's under test here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ProfilingSessionController_test {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var hardwareMetrics: HardwareMetricsCollector
    private lateinit var controller: ProfilingSessionController
    private val performanceData = ConcurrentHashMap<String, Any>()

    @Before
    fun setUp() {
        hardwareMetrics = mockk(relaxed = true)
        controller =
                ProfilingSessionController(
                        Handler(Looper.getMainLooper()),
                        performanceData,
                        hardwareMetrics
                )
    }

    @Test
    fun `isActive comeca falso antes de qualquer startProfiling`() {
        assertFalse(controller.isActive())
    }

    @Test
    fun `startProfiling marca a sessao como ativa`() {
        controller.startProfiling(context)

        assertTrue(controller.isActive())
    }

    @Test
    fun `startProfiling chamado duas vezes nao reinicia a coleta (idempotente)`() {
        controller.startProfiling(context)
        controller.startProfiling(context)

        assertTrue(controller.isActive())
        // The BASIC-level start hook must fire exactly once: a second startProfiling() call while
        // already active must return early instead of re-entering the dispatch.
        verify(exactly = 1) { hardwareMetrics.startBasicSystemMonitoring() }
    }

    @Test
    fun `stopProfiling desativa a sessao`() {
        controller.startProfiling(context)

        controller.stopProfiling()

        assertFalse(controller.isActive())
    }

    @Test
    fun `stopProfiling em uma sessao inativa nao lanca excecao`() {
        controller.stopProfiling()

        assertFalse(controller.isActive())
    }

    // --- profile level per Android version, and the monitoring loop ---

    @After
    fun tearDown() {
        unmockkObject(AndroidCompatibility)
    }

    private fun runFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `no Android 11 so o monitoramento basico roda`() {
        controller.startProfiling(context)
        runFor(0)

        verify { hardwareMetrics.startBasicSystemMonitoring() }
        verify(exactly = 0) { hardwareMetrics.collectStandardMetrics() }
        verify(exactly = 0) { hardwareMetrics.collectAdvancedMetrics() }
        assertTrue(performanceData.containsKey(DebugOverlayText.MEMORY_USED_MB_KEY))
        assertTrue(performanceData.containsKey(DebugOverlayText.CPU_USAGE_PERCENT_KEY))
        assertTrue(performanceData.containsKey("timestamp"))
    }

    @Test
    fun `no Android 12+ o perfil padrao coleta as metricas padrao`() {
        mockkObject(AndroidCompatibility)
        every { AndroidCompatibility.isAndroid12Plus() } returns true

        controller.startProfiling(context)
        runFor(0)

        verify { hardwareMetrics.startBasicGpuProfiling() }
        verify { hardwareMetrics.startStandardMemoryProfiling() }
        verify(exactly = 1) { hardwareMetrics.collectStandardMetrics() }
        verify(exactly = 0) { hardwareMetrics.startBasicSystemMonitoring() }
    }

    @Test
    fun `no Android 16+ o perfil avancado coleta as metricas avancadas`() {
        mockkObject(AndroidCompatibility)
        every { AndroidCompatibility.isAndroid16Plus() } returns true
        every { AndroidCompatibility.isAndroid12Plus() } returns true

        controller.startProfiling(context)
        runFor(0)

        verify { hardwareMetrics.startAdvancedProfilingStubs() }
        verify(exactly = 1) { hardwareMetrics.collectAdvancedMetrics() }
        verify(exactly = 0) { hardwareMetrics.collectStandardMetrics() }
    }

    @Test
    fun `a coleta se repete a cada segundo ate stopProfiling`() {
        controller.startProfiling(context)
        runFor(2_500)
        verify(exactly = 3) { hardwareMetrics.getCpuUsage() }

        controller.stopProfiling()
        runFor(5_000)

        verify(exactly = 3) { hardwareMetrics.getCpuUsage() }
    }

    @Test
    fun `parar e reiniciar logo em seguida nao deixa o ciclo antigo coletando junto`() {
        controller.startProfiling(context)
        runFor(500)
        controller.stopProfiling()
        controller.startProfiling(context)

        runFor(2_800)

        // t=0 (first session), then t=500, 1500, 2500 (second session) -- no leftover t=1000/2000/3000.
        verify(exactly = 4) { hardwareMetrics.getCpuUsage() }
    }
}
