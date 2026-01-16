package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.AudioController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel especializado para gerenciamento de áudio. Responsável por controle de volume, mute e
 * configurações de áudio.
 */
class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )
    private var audioController: AudioController? = null

    // Estado do áudio
    private var isAudioEnabled: Boolean = true

    init {
        loadAudioState()
    }

    private fun loadAudioState() {
        viewModelScope.launch {
            try {
                isAudioEnabled = preferencesRepository.getAudioEnabledSync()
            } catch (e: Exception) {
                android.util.Log.e("AudioViewModel", "Error loading audio state", e)
                isAudioEnabled = true // Default
            }
        }
    }

    // ========== MÉTODOS DE CONTROLE DE ÁUDIO ==========

    @Suppress("UNUSED_PARAMETER")
    fun toggleAudio(retroView: Any?): Boolean {
        // TODO: Implementar toggle de áudio
        return isAudioEnabled
    }

    @Suppress("UNUSED_PARAMETER")
    fun setAudioEnabled(retroView: Any?, enabled: Boolean) {
        isAudioEnabled = enabled

        // Apply audio change to controller if available
        if (retroView is GLRetroView) {
            audioController?.setAudioEnabled(retroView, enabled)
        }

        saveAudioState()
    }

    private fun saveAudioState() {
        viewModelScope.launch {
            try {
                preferencesRepository.setAudioEnabled(isAudioEnabled)
            } catch (e: Exception) {
                android.util.Log.e("AudioViewModel", "Error saving audio state", e)
            }
        }
    }

    // ========== GETTERS ==========

    fun getAudioState(): Boolean = isAudioEnabled

    fun getAudioController(): AudioController? = audioController

    // ========== SETTERS ==========

    fun setAudioController(controller: AudioController) {
        audioController = controller
    }
}
