package com.vinaooo.revenger.viewmodels.menu

import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun renderedRetroView(frameSpeed: Int, rendered: Boolean = true): Pair<RetroView, GLRetroView> {
        val (retroView, glRetroView) = mockRetroView(frameSpeed)
        every { retroView.frameRendered } returns MutableLiveData(rendered)
        return retroView to glRetroView
    }

    @Test
    fun `loadState carrega em velocidade 1 e devolve a velocidade anterior`() {
        val (retroView, glRetroView) = renderedRetroView(frameSpeed = 0)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        every { utils.hasSaveState() } returns true
        var speedDuringLoad = -1
        every { utils.loadState(retroView) } answers { speedDuringLoad = glRetroView.frameSpeed }
        var completed = false

        val loaded = SaveLoadOrchestrator().loadState(retroView, utils) { completed = true }

        assertTrue(loaded)
        assertEquals(1, speedDuringLoad)
        assertEquals(0, glRetroView.frameSpeed)
        assertTrue(completed)
    }

    @Test
    fun `loadState sem callback tambem carrega`() {
        val (retroView, _) = renderedRetroView(frameSpeed = 1)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        every { utils.hasSaveState() } returns true

        assertTrue(SaveLoadOrchestrator().loadState(retroView, utils))
        verify(exactly = 1) { utils.loadState(retroView) }
    }

    @Test
    fun `loadState sem save state nao carrega e avisa`() {
        val (retroView, _) = renderedRetroView(frameSpeed = 1)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        every { utils.hasSaveState() } returns false
        var completed = false

        assertFalse(SaveLoadOrchestrator().loadState(retroView, utils) { completed = true })
        assertFalse(SaveLoadOrchestrator().loadState(retroView, utils))

        assertTrue(completed)
        verify(exactly = 0) { utils.loadState(any()) }
    }

    @Test
    fun `loadState antes do primeiro frame ou sem utils nao carrega`() {
        val (notRendered, _) = renderedRetroView(frameSpeed = 1, rendered = false)
        val (rendered, _) = renderedRetroView(frameSpeed = 1)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        var completions = 0
        val orchestrator = SaveLoadOrchestrator()

        assertFalse(orchestrator.loadState(notRendered, utils) { completions++ })
        assertFalse(orchestrator.loadState(rendered, null) { completions++ })
        assertFalse(orchestrator.loadState(null, utils))

        assertEquals(2, completions)
        verify(exactly = 0) { utils.loadState(any()) }
    }

    @Test
    fun `saveState sem RetroView ou utils so avisa`() {
        val (retroView, _) = mockRetroView(initialFrameSpeed = 1)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        var completions = 0
        val orchestrator = SaveLoadOrchestrator()

        orchestrator.saveState(null, utils) { completions++ }
        orchestrator.saveState(retroView, null) { completions++ }
        orchestrator.saveState(null, null)

        assertEquals(2, completions)
        verify(exactly = 0) { utils.saveState(any()) }
    }

    @Test
    fun `saveState em velocidade normal salva na hora e avisa`() {
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 2)
        val utils = mockk<RetroViewUtils>(relaxed = true)
        var completed = false

        SaveLoadOrchestrator().saveState(retroView, utils) { completed = true }

        verify(exactly = 1) { utils.saveState(retroView) }
        assertEquals(2, glRetroView.frameSpeed)
        assertTrue(completed)
    }

    @Test
    fun `saveState pausado avisa depois do atraso`() {
        val (retroView, _) = mockRetroView(initialFrameSpeed = 0)
        var completed = false

        SaveLoadOrchestrator().saveState(retroView, mockk(relaxed = true)) { completed = true }
        assertFalse(completed)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        assertTrue(completed)
    }

    @Test
    fun `resetGame reinicia o core e avisa`() {
        val (retroView, glRetroView) = mockRetroView(initialFrameSpeed = 1)
        var completions = 0
        val orchestrator = SaveLoadOrchestrator()

        orchestrator.resetGame(retroView) { completions++ }
        orchestrator.resetGame(null) { completions++ }
        orchestrator.resetGame(retroView)

        verify(exactly = 2) { glRetroView.reset() }
        assertEquals(2, completions)
    }
}
