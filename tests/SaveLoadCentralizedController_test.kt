package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SaveLoadCentralizedController]'s own logic is thin delegation to [SaveLoadOrchestrator]
 * (already covered by its own test suite) -- these tests pin only what's specific to this class:
 * which arguments each method forwards, and the `markSkipNextTempStateLoad` callback wiring that
 * `GameActivityViewModel`'s characterization tests already cover end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveLoadCentralizedController_test {

    private lateinit var saveLoadOrchestrator: SaveLoadOrchestrator
    private var currentRetroView: RetroView? = null
    private var currentRetroViewUtils: RetroViewUtils? = null
    private var skipNextTempStateLoadMarked = false
    private lateinit var controller: SaveLoadCentralizedController

    @Before
    fun setUp() {
        saveLoadOrchestrator = mockk(relaxed = true)
        currentRetroView = null
        currentRetroViewUtils = null
        skipNextTempStateLoadMarked = false
        controller =
                SaveLoadCentralizedController(
                        saveLoadOrchestrator = saveLoadOrchestrator,
                        retroView = { currentRetroView },
                        retroViewUtils = { currentRetroViewUtils },
                        markSkipNextTempStateLoad = { skipNextTempStateLoadMarked = true }
                )
    }

    @Test
    fun `loadStateCentralized marca skipNextTempStateLoad quando o load realmente aconteceu`() {
        currentRetroView = mockk(relaxed = true)
        currentRetroViewUtils = mockk(relaxed = true)
        every { saveLoadOrchestrator.loadState(currentRetroView, currentRetroViewUtils, any()) } returns
                true

        controller.loadStateCentralized()

        assertTrue(skipNextTempStateLoadMarked)
    }

    @Test
    fun `loadStateCentralized nao marca skipNextTempStateLoad quando nao houve load`() {
        every { saveLoadOrchestrator.loadState(any(), any(), any()) } returns false

        controller.loadStateCentralized()

        assertFalse(skipNextTempStateLoadMarked)
    }

    @Test
    fun `saveStateCentralized repassa retroView, retroViewUtils, keepPaused e onComplete`() {
        currentRetroView = mockk(relaxed = true)
        currentRetroViewUtils = mockk(relaxed = true)
        val onComplete = {}

        controller.saveStateCentralized(onComplete, keepPaused = true)

        verify(exactly = 1) {
            saveLoadOrchestrator.saveState(currentRetroView, currentRetroViewUtils, true, onComplete)
        }
    }

    @Test
    fun `resetGameCentralized repassa retroView e onComplete`() {
        currentRetroView = mockk(relaxed = true)
        val onComplete = {}

        controller.resetGameCentralized(onComplete)

        verify(exactly = 1) { saveLoadOrchestrator.resetGame(currentRetroView, onComplete) }
    }

    @Test
    fun `hasSaveState delega para retroViewUtils`() {
        val utils = mockk<RetroViewUtils>(relaxed = true)
        every { utils.hasSaveState() } returns true
        currentRetroViewUtils = utils

        assertTrue(controller.hasSaveState())
    }

    @Test
    fun `hasSaveState retorna false quando retroViewUtils e nulo`() {
        currentRetroViewUtils = null

        assertFalse(controller.hasSaveState())
    }
}
