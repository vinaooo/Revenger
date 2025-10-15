package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView

class RetroViewUtils(private val activity: Activity) {
    /** Retorna o caminho do arquivo de save state utilizado */
    fun getSaveStatePath(retroView: com.vinaooo.revenger.retroview.RetroView?): String? {
        return try {
            storage.state.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    private val fastForwardSpeed =
            activity.resources.getInteger(R.integer.config_fast_forward_multiplier)

    fun restoreEmulatorState(
            retroView: RetroView,
            skipTempStateLoad: Boolean = false,
            autoRestoreManualState: Boolean = false
    ) {
        // FIX: Restore frameSpeed, but ensure it never is 0 (paused)
        // If saved frameSpeed is 0, it means app was closed with menu open
        // In this case, restore to 1 (normal speed) to avoid black screen
        val savedFrameSpeed = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
        retroView.view.frameSpeed = if (savedFrameSpeed == 0) 1 else savedFrameSpeed
        retroView.view.audioEnabled =
                sharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)

        // CRITICAL FIX: Do not load tempState if we just did manual Load State
        // This prevents tempState from overwriting the save state that user just loaded
        if (!skipTempStateLoad) {
            val hasSave = hasSaveState()
            val tempExists = storage.tempState.exists()

            when {
                autoRestoreManualState && hasSave -> loadState(retroView)
                tempExists -> loadTempState(retroView)
            }
        }
    }

    fun preserveEmulatorState(retroView: RetroView) {
        saveSRAM(retroView)

        saveTempState(retroView)

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

    fun saveSRAM(retroView: RetroView) {
        storage.sram.outputStream().use { it.write(retroView.view.serializeSRAM()) }
    }

    fun loadState(retroView: RetroView) {
        if (!storage.state.exists()) {
            return
        }

        val stateBytes = storage.state.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    fun loadTempState(retroView: RetroView) {
        if (!storage.tempState.exists()) {
            return
        }

        val stateBytes = storage.tempState.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    fun saveState(retroView: RetroView) {
        try {
            val stateBytes = retroView.view.serializeState()

            storage.state.outputStream().use { it.write(stateBytes) }
        } catch (e: Exception) {
            // Save errors are ignored to maintain compatibility with previous behavior
        }
    }

    fun saveTempState(retroView: RetroView) {
        val stateBytes = retroView.view.serializeState()

        storage.tempState.outputStream().use { it.write(stateBytes) }
    }

    fun fastForward(retroView: RetroView) {
        retroView.view.frameSpeed = if (retroView.view.frameSpeed == 1) fastForwardSpeed else 1
    }

    /** Check if fast forward is currently active Required for Material You menu state tracking */
    fun isFastForwardActive(): Boolean {
        return sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1) > 1
    }

    /**
     * Check if a save state exists Required for Material You menu to show/hide load state option
     */
    fun hasSaveState(): Boolean {
        val exists = storage.state.exists()
        val length = if (exists) storage.state.length() else 0
        val result = exists && length > 0

        return result
    }

    /** Get the current audio state from preferences */
    fun getAudioState(): Boolean {
        return sharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)
    }

    /** Get the current fast forward state from preferences */
    fun getFastForwardState(): Boolean {
        return sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1) > 1
    }
}
