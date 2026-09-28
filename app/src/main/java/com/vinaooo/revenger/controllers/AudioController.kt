package com.vinaooo.revenger.controllers

import android.content.SharedPreferences
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.utils.PreferencesConstants
import androidx.core.content.edit

/**
 * Modular controller to manage emulator audio functionalities. Allows centralized sound control
 * that can be reused across different parts of the system
 */
class AudioController(private val sharedPreferences: SharedPreferences) {
    /**
     * Sets the audio state
     * @param retroView RetroView where to apply the change
     * @param enabled true to turn on, false to turn off
     */
    fun setAudioEnabled(retroView: GLRetroView, enabled: Boolean) {
        retroView.audioEnabled = enabled
        saveAudioState(enabled)
    }

    /**
     * Gets the current audio state from preferences
     * @return true if audio is enabled, false otherwise
     */
    fun getAudioState(): Boolean {
        return sharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)
    }

    /**
     * Initializes the audio state in RetroView based on saved preferences
     * @param retroView RetroView to configure
     */
    fun initializeAudioState(retroView: GLRetroView) {
        val savedState = getAudioState()
        retroView.audioEnabled = savedState
    }

    /**
     * Saves the current audio state to preferences
     * @param enabled state to be saved
     */
    private fun saveAudioState(enabled: Boolean) {
      sharedPreferences.edit(commit = true) {
        putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, enabled)
        // Use commit() instead of apply() to ensure synchronization
        }
    }
}
