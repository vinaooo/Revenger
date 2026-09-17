package com.vinaooo.revenger.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

    @Before
    fun setUp() {
        hardwareMetrics = mockk(relaxed = true)
        controller =
                ProfilingSessionController(
                        Handler(Looper.getMainLooper()),
                        ConcurrentHashMap(),
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
}
