package com.vinaooo.revenger.viewmodels.menu

import android.os.Looper
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression tests for the 200ms delayed save-restore race described in the maturity audit:
 * two overlapping [SaveLoadOrchestrator.saveState] calls while paused used to each schedule an
 * independent restore of their own captured frame speed, so the frame speed left behind after
 * both callbacks fired depended on scheduling order rather than on the most recent call.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveLoadOrchestrator_test {

    private fun mockRetroView(initialFrameSpeed: Int): Pair<RetroView, GLRetroView> {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        var frameSpeed = initialFrameSpeed
        every { glRetroView.frameSpeed } answers { frameSpeed }
        every { glRetroView.frameSpeed = any() } answers { frameSpeed = firstArg() }
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        return retroView to glRetroView
    }

    @Test
    fun `saveState restaura o frameSpeed original apos o delay quando pausado`() {
        val orchestrator = SaveLoadOrchestrator()
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 0)
        val utils = mockk<RetroViewUtils>(relaxed = true)

        orchestrator.saveState(retroView, utils)

        assertEquals(1, glRetroView.frameSpeed) // temporarily unpaused
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        verify { utils.saveState(retroView) }
        assertEquals(0, glRetroView.frameSpeed) // restored to the paused state
    }

    @Test
    fun `uma segunda chamada sobreposta cancela o restore pendente da primeira`() {
        val orchestrator = SaveLoadOrchestrator()
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 0)
        val utils = mockk<RetroViewUtils>(relaxed = true)

        orchestrator.saveState(retroView, utils) // schedules restore-to-0 at t=200ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        glRetroView.frameSpeed = 0 // simulate the menu re-pausing between the two taps
        orchestrator.saveState(retroView, utils) // must cancel the first call's pending restore

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        // Only the second call's restore ever fires -- the stale first one was cancelled, so
        // saveState() on utils is called exactly once (not twice, racing) and the final frame
        // speed matches the second (most recent) call's captured value, deterministically.
        verify(exactly = 1) { utils.saveState(retroView) }
        assertEquals(0, glRetroView.frameSpeed)
    }

    @Test
    fun `cancelPendingSave impede o restore de disparar apos a destruicao do ViewModel`() {
        val orchestrator = SaveLoadOrchestrator()
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 0)
        val utils = mockk<RetroViewUtils>(relaxed = true)

        orchestrator.saveState(retroView, utils)
        orchestrator.cancelPendingSave() // e.g. ViewModel.onCleared() firing before the 200ms delay

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        verify(exactly = 0) { utils.saveState(retroView) }
        assertEquals(1, glRetroView.frameSpeed) // never restored -- the pending work was dropped
    }

    @Test
    fun `saveState imediato quando keepPaused e verdadeiro nao agenda nada`() {
        val orchestrator = SaveLoadOrchestrator()
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 0)
        val utils = mockk<RetroViewUtils>(relaxed = true)

        orchestrator.saveState(retroView, utils, keepPaused = true)

        verifyOrder { utils.saveState(retroView) }
        assertEquals(0, glRetroView.frameSpeed)
    }
}
