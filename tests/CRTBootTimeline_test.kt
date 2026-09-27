package com.vinaooo.revenger.ui.splash

import com.vinaooo.revenger.ui.splash.CRTBootTimeline.MAX_ALPHA
import com.vinaooo.revenger.ui.splash.CRTBootTimeline.PHASE_1_END
import com.vinaooo.revenger.ui.splash.CRTBootTimeline.PHASE_2_END
import com.vinaooo.revenger.ui.splash.CRTBootTimeline.Phase
import com.vinaooo.revenger.ui.splash.CRTBootTimeline.SCANLINE_MAX_OPACITY
import org.junit.Assert.assertEquals
import org.junit.Test

class CRTBootTimeline_test {

    private val delta = 1e-4f

    @Test
    fun `phase boundaries`() {
        assertEquals(Phase.DOT, CRTBootTimeline.phaseAt(0f))
        assertEquals(Phase.DOT, CRTBootTimeline.phaseAt(PHASE_1_END - delta))
        assertEquals(Phase.LINE, CRTBootTimeline.phaseAt(PHASE_1_END))
        assertEquals(Phase.LINE, CRTBootTimeline.phaseAt(PHASE_2_END - delta))
        assertEquals(Phase.EXPANSION, CRTBootTimeline.phaseAt(PHASE_2_END))
        assertEquals(Phase.EXPANSION, CRTBootTimeline.phaseAt(1f))
    }

    @Test
    fun `boot runs forward and shutdown runs backward`() {
        assertEquals(0f to 1f, CRTBootTimeline.progressRange(reverse = false))
        assertEquals(1f to 0f, CRTBootTimeline.progressRange(reverse = true))
    }

    @Test
    fun `shutdown visits the phases in reverse order`() {
        val (from, to) = CRTBootTimeline.progressRange(reverse = true)
        val phases =
            (0..100).map { from + (to - from) * it / 100f }
                .map { CRTBootTimeline.phaseAt(it) }
                .distinct()

        assertEquals(listOf(Phase.EXPANSION, Phase.LINE, Phase.DOT), phases)
    }

    @Test
    fun `dot grows during the dot phase and stays full after it`() {
        assertEquals(0f, CRTBootTimeline.frameAt(0f, false).dotRadiusFraction, delta)
        assertEquals(0.5f, CRTBootTimeline.frameAt(PHASE_1_END / 2, false).dotRadiusFraction, delta)
        assertEquals(1f, CRTBootTimeline.frameAt(PHASE_1_END, false).dotRadiusFraction, delta)
        assertEquals(1f, CRTBootTimeline.frameAt(1f, false).dotRadiusFraction, delta)
    }

    @Test
    fun `line grows across the line phase only`() {
        val middle = (PHASE_1_END + PHASE_2_END) / 2

        assertEquals(0f, CRTBootTimeline.frameAt(PHASE_1_END / 2, false).lineWidthFraction, delta)
        assertEquals(0f, CRTBootTimeline.frameAt(PHASE_1_END, false).lineWidthFraction, delta)
        assertEquals(0.5f, CRTBootTimeline.frameAt(middle, false).lineWidthFraction, delta)
        assertEquals(0f, CRTBootTimeline.frameAt(1f, false).lineWidthFraction, delta)
    }

    @Test
    fun `expansion opens across the expansion phase`() {
        val middle = (PHASE_2_END + 1f) / 2

        assertEquals(0f, CRTBootTimeline.frameAt(PHASE_2_END, false).expansionFraction, delta)
        assertEquals(0.5f, CRTBootTimeline.frameAt(middle, false).expansionFraction, delta)
        assertEquals(1f, CRTBootTimeline.frameAt(1f, false).expansionFraction, delta)
    }

    @Test
    fun `boot fades the picture out as it opens`() {
        assertEquals(MAX_ALPHA, CRTBootTimeline.frameAt(PHASE_2_END, false).fillAlpha)
        assertEquals(0, CRTBootTimeline.frameAt(1f, false).fillAlpha)
    }

    @Test
    fun `shutdown fades the picture in as it opens`() {
        assertEquals(0, CRTBootTimeline.frameAt(PHASE_2_END, true).fillAlpha)
        assertEquals(MAX_ALPHA, CRTBootTimeline.frameAt(1f, true).fillAlpha)
    }

    @Test
    fun `no fill outside the expansion phase`() {
        assertEquals(0, CRTBootTimeline.frameAt(0.1f, false).fillAlpha)
        assertEquals(0, CRTBootTimeline.frameAt((PHASE_1_END + PHASE_2_END) / 2, true).fillAlpha)
    }

    @Test
    fun `scanlines appear with the expansion in both directions`() {
        assertEquals(0, CRTBootTimeline.frameAt(0.1f, false).scanlineAlpha)
        assertEquals(0, CRTBootTimeline.frameAt(PHASE_2_END, false).scanlineAlpha)
        assertEquals(SCANLINE_MAX_OPACITY, CRTBootTimeline.frameAt(1f, false).scanlineAlpha)
        assertEquals(SCANLINE_MAX_OPACITY, CRTBootTimeline.frameAt(1f, true).scanlineAlpha)
    }
}
