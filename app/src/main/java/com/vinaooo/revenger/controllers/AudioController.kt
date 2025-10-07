package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Modular controller to manage emulator audio functionalities. Allows centralized sound control
 * that can be reused across different parts of the system
 */
class AudioController(
        private val context: Context,
        private val sharedPreferences: SharedPreferences
) {
    /**
     * Toggles the audio state (on/off)
     * @param retroView RetroView where to apply the change
     * @return new audio state (true = on, false = off)
     */
    fun toggleAudio(retroView: GLRetroView): Boolean {
        val newState = !retroView.audioEnabled
        retroView.audioEnabled = newState

        // Salvar o novo estado imediatamente
        saveAudioState(newState)

        return newState
    }

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
        return sharedPreferences.getBoolean(context.getString(R.string.pref_audio_enabled), true)
    }

    /**
     * Gets the current audio state directly from RetroView
     * @param retroView RetroView to check the state
     * @return true if audio is enabled, false otherwise
     */
    fun getAudioState(retroView: GLRetroView): Boolean {
        return retroView.audioEnabled
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
        with(sharedPreferences.edit()) {
            putBoolean(context.getString(R.string.pref_audio_enabled), enabled)
            commit() // Use commit() instead of apply() to ensure synchronization
        }
    }

    /**
     * Gets textual description of the current audio state
     * @return String with current state ("Audio ON" or "Audio OFF")
     */
    fun getAudioStateDescription(): String {
        return if (getAudioState()) {
            context.getString(R.string.audio_on)
        } else {
            context.getString(R.string.audio_off)
        }
    }

    /**
     * Gets appropriate icon ID for the current audio state
     * @return Resource ID of the icon
     */
    fun getAudioIconResource(): Int {
        return if (getAudioState()) {
            R.drawable.ic_volume_up_24
        } else {
            R.drawable.ic_volume_off_24
        }
    }
}
