package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView

class RetroViewUtils(
        private val activity: Activity,
        private val stateFiles: StateFileOperations = StateFileStore(Storage.getInstance(activity))
) : StateFileOperations by stateFiles {

    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    /**
     * Keeps what must outlive the session when the menu opens or the app pauses: the game's own
     * save memory (SRAM) and the speed/audio settings. No emulator snapshot is taken: the game
     * starts fresh on launch and only a manual Load State restores one.
     */
    fun preserveEmulatorState(retroView: RetroView) {
        saveSRAM(retroView)

        sharedPreferences.edit {
            // CRITICAL: Never save frameSpeed = 0 (paused by menu)
            // If frameSpeed is 0, it means the menu is open
            // In this case, we keep the last valid saved value (don't overwrite)
            val currentFrameSpeed = retroView.view.frameSpeed
            if (currentFrameSpeed > 0) {
                putInt(PreferencesConstants.PREF_FRAME_SPEED, currentFrameSpeed)
            }
            putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, retroView.view.audioEnabled)
        }
    }

    /** Check if a non-empty save state exists (the load option depends on it) */
    fun hasSaveState(): Boolean {
        val exists = storage.state.exists()
        val length = if (exists) storage.state.length() else 0
        val result = exists && length > 0

        return result
    }
}
