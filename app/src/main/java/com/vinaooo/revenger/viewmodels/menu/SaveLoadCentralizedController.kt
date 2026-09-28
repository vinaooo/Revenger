package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils

/** Centralized save/load/reset orchestration plus save-state availability. */
interface SaveLoadCentralizedFacade {
    fun loadStateCentralized(onComplete: (() -> Unit)? = null)
    fun saveStateCentralized(onComplete: (() -> Unit)? = null, keepPaused: Boolean = false)
    fun resetGameCentralized(onComplete: (() -> Unit)? = null)
    fun hasSaveState(): Boolean
}

/**
 * Implementation of [SaveLoadCentralizedFacade]. [retroView] and [retroViewUtils] are read lazily
 * via providers, not captured at construction time, since both are mutated on the owning
 * ViewModel after construction.
 */
class SaveLoadCentralizedController(
        private val saveLoadOrchestrator: SaveLoadOrchestrator,
        private val retroView: () -> RetroView?,
        private val retroViewUtils: () -> RetroViewUtils?
) : SaveLoadCentralizedFacade {

    override fun loadStateCentralized(onComplete: (() -> Unit)?) {
        saveLoadOrchestrator.loadState(retroView(), retroViewUtils(), onComplete)
    }

    override fun saveStateCentralized(onComplete: (() -> Unit)?, keepPaused: Boolean) {
        saveLoadOrchestrator.saveState(retroView(), retroViewUtils(), keepPaused, onComplete)
    }

    override fun resetGameCentralized(onComplete: (() -> Unit)?) {
        saveLoadOrchestrator.resetGame(retroView(), onComplete)
    }

    override fun hasSaveState(): Boolean = retroViewUtils()?.hasSaveState() ?: false
}
