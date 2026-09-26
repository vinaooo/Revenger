package com.vinaooo.revenger.performance

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.RevengerApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [DebugOverlayController]. The first group drives the private
 * `startDebugOverlayUpdates` by reflection with a mocked [Handler], so the posted runnable never
 * runs; the rest show, update and hide the overlay in a real activity with the main looper.
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

    // --- showing, updating and hiding the overlay, with a real activity ---

    private val frameStats = AdvancedPerformanceProfiler.FrameStats(averageFps = 59.9, averageFrameTimeMs = 16.7, droppedFrames = 2)
    private val performanceData = ConcurrentHashMap<String, Any>(mapOf(DebugOverlayText.MEMORY_USED_MB_KEY to 42L))
    private var profilingActive = true

    private fun realController(enabled: Boolean): DebugOverlayController =
            DebugOverlayController(
                    Handler(Looper.getMainLooper()),
                    performanceData,
                    mockk<FrameStatsProvider> { every { getFrameStats() } returns frameStats },
                    isOverlayEnabled = { enabled },
                    isProfilingActive = { profilingActive }
            )

    private val activity by lazy { Robolectric.buildActivity(Activity::class.java).setup().get() }

    private fun overlays(): List<TextView> {
        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        return (0 until content.childCount).map(content::getChildAt).filterIsInstance<TextView>()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `overlay desligado na configuracao nao adiciona nada`() {
        realController(enabled = false).showDebugOverlay(activity)
        idle()

        assertTrue(overlays().isEmpty())
    }

    @Test
    fun `contexto que nao e Activity nao adiciona nada`() {
        realController(enabled = true).showDebugOverlay(ApplicationProvider.getApplicationContext<Context>())
        idle()

        assertTrue(overlays().isEmpty())
    }

    @Test
    fun `overlay ligado mostra as metricas e atualiza a cada meio segundo`() {
        val overlay = realController(enabled = true)

        overlay.showDebugOverlay(activity)
        idle()

        val view = overlays().single()
        assertEquals(DebugOverlayText.format(frameStats, performanceData), view.text.toString())

        performanceData[DebugOverlayText.MEMORY_USED_MB_KEY] = 64L
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertTrue(view.text.toString(), view.text.contains("Memory: 64MB"))
    }

    @Test
    fun `mostrar de novo reaproveita a mesma view`() {
        val overlay = realController(enabled = true)

        overlay.showDebugOverlay(activity)
        overlay.showDebugOverlay(activity)
        idle()

        assertEquals(1, overlays().size)
    }

    @Test
    fun `sem profiling ativo o texto nao e atualizado`() {
        profilingActive = false
        realController(enabled = true).showDebugOverlay(activity)
        idle()

        assertEquals("Initializing FPS overlay...", overlays().single().text.toString())
    }

    @Test
    fun `hideDebugOverlay remove a view e para as atualizacoes`() {
        val overlay = realController(enabled = true)
        overlay.showDebugOverlay(activity)
        idle()
        val view = overlays().single()

        overlay.hideDebugOverlay()
        performanceData[DebugOverlayText.MEMORY_USED_MB_KEY] = 99L
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_000))

        assertTrue(overlays().isEmpty())
        assertFalse(view.text.contains("99MB"))
    }

    @Test
    fun `por padrao segue a chave performance_overlay do AppConfig`() {
        // Regression: the gate read a `performance_overlay` bool resource that doesn't exist (the
        // config is JSON now), so the overlay never showed whatever the config said.
        assertEquals(RevengerApplication.appConfig.getPerformanceOverlay(), isPerformanceOverlayConfigured())
    }
}
