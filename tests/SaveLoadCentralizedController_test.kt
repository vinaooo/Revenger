package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SaveLoadCentralizedController]'s own logic is thin delegation to [SaveLoadOrchestrator]
 * (already covered by its own test suite) -- these tests pin only what's specific to this class:
 * which arguments each method forwards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveLoadCentralizedController_test {

    private lateinit var saveLoadOrchestrator: SaveLoadOrchestrator
    private var currentRetroView: RetroView? = null
    private var currentRetroViewUtils: RetroViewUtils? = null
    private lateinit var controller: SaveLoadCentralizedController

    @Before
    fun setUp() {
        saveLoadOrchestrator = mockk(relaxed = true)
        currentRetroView = null
        currentRetroViewUtils = null
        controller =
                SaveLoadCentralizedController(
                        saveLoadOrchestrator = saveLoadOrchestrator,
                        retroView = { currentRetroView },
                        retroViewUtils = { currentRetroViewUtils }
                )
    }

    @Test
    fun `loadStateCentralized repassa retroView, retroViewUtils e onComplete`() {
        currentRetroView = mockk(relaxed = true)
        currentRetroViewUtils = mockk(relaxed = true)
        val onComplete = {}

        controller.loadStateCentralized(onComplete)

        verify(exactly = 1) { saveLoadOrchestrator.loadState(currentRetroView, currentRetroViewUtils, onComplete) }
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
}
