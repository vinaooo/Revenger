package com.vinaooo.revenger.viewmodels.menu

import android.os.Handler
import android.os.Looper
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils

/**
 * Centralizes save/load/reset orchestration for `GameActivityViewModel` around the emulator's
 * frame-speed state. Takes [RetroView]/[RetroViewUtils] by value on each call rather than owning
 * them, so it carries no ViewModel-lifecycle state of its own.
 */
class SaveLoadOrchestrator {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingSaveRestore: Runnable? = null

    /**
     * Loads the current save state, temporarily unpausing the core if needed so the load can
     * proceed, then restores the prior frame speed. Returns true if a load actually happened
     * (the caller uses this to decide whether to set its own "skip next temp-state load" flag) --
     * false if there was nothing to load or the emulator wasn't ready.
     */
    fun loadState(
            retroView: RetroView?,
            utils: RetroViewUtils?,
            onComplete: (() -> Unit)? = null
    ): Boolean {
        if (retroView?.frameRendered?.value != true || utils == null) {
            onComplete?.invoke()
            return false
        }

        if (utils.hasSaveState() != true) {
            onComplete?.invoke()
            return false
        }

        val savedFrameSpeed = retroView.view.frameSpeed
        retroView.view.frameSpeed = 1
        utils.loadState(retroView)
        retroView.view.frameSpeed = savedFrameSpeed
        onComplete?.invoke()
        return true
    }

    /**
     * Saves the current state. If the emulator is currently paused (frame speed 0) and the
     * caller isn't explicitly keeping it paused, temporarily unpauses, saves after a 200ms
     * delay, then restores the pause -- otherwise saves immediately at the current frame speed.
     */
    fun saveState(
            retroView: RetroView?,
            utils: RetroViewUtils?,
            keepPaused: Boolean = false,
            onComplete: (() -> Unit)? = null
    ) {
        if (retroView == null || utils == null) {
            onComplete?.invoke()
            return
        }

        val savedFrameSpeed = retroView.view.frameSpeed
        if (savedFrameSpeed == 0 && !keepPaused) {
            // Only temporarily unpause if not explicitly keeping paused (menu context).
            // Cancel any still-pending restore from an earlier overlapping call first: without
            // this, two overlapping saveState() calls each capture their own savedFrameSpeed
            // and race to restore it 200ms later, so the frame speed left behind after both
            // callbacks fire depends on scheduling order instead of on the most recent call.
            pendingSaveRestore?.let { handler.removeCallbacks(it) }
            retroView.view.frameSpeed = 1
            val restore = Runnable {
                utils.saveState(retroView)
                retroView.view.frameSpeed = savedFrameSpeed
                pendingSaveRestore = null
                onComplete?.invoke()
            }
            pendingSaveRestore = restore
            handler.postDelayed(restore, 200)
        } else {
            // Keep current frameSpeed (including 0 for paused state in menu)
            utils.saveState(retroView)
            onComplete?.invoke()
        }
    }

    /**
     * Cancels a pending delayed save-restore scheduled by [saveState], if any. Callers should
     * invoke this from their own teardown (e.g. ViewModel.onCleared()) so the restore never
     * fires 200ms later against a [RetroView] that has already been torn down.
     */
    fun cancelPendingSave() {
        pendingSaveRestore?.let { handler.removeCallbacks(it) }
        pendingSaveRestore = null
    }

    /** Resets the emulator core to the beginning of the game, if a [RetroView] is attached. */
    fun resetGame(retroView: RetroView?, onComplete: (() -> Unit)? = null) {
        retroView?.view?.reset()
        onComplete?.invoke()
    }
}
