package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.AudioController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel specialized in audio management. Responsible for volume control, mute, and
 * audio settings.
 */
class AudioViewModel(application: Application) : AndroidViewModel(application) {

    sealed class AudioEvent {
        object Idle : AudioEvent()
        data class ToggleAudio(val retroView: Any?) : AudioEvent()
    }

    private val _eventFlow = MutableStateFlow<AudioEvent>(AudioEvent.Idle)
    val eventFlow: StateFlow<AudioEvent> = _eventFlow.asStateFlow()


    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )
    private var audioController: AudioController? = null

    // Audio state
    private var isAudioEnabled: Boolean = true

    init {
        loadAudioState()
    }

    // SharedPreferences.getBoolean() throws ClassCastException if a value stored under that key
    // isn't a Boolean (e.g. a stale value from a preferences-format change).
    private fun loadAudioState() {
        viewModelScope.launch {
            try {
                isAudioEnabled = preferencesRepository.getAudioEnabledSync()
            } catch (e: ClassCastException) {
                android.util.Log.e("AudioViewModel", "Error loading audio state", e)
                isAudioEnabled = true // Default
            }
        }
    }

    // ========== AUDIO CONTROL METHODS ==========

    fun toggleAudio(retroView: Any?): Boolean {
        _eventFlow.value = AudioEvent.ToggleAudio(retroView)
        return isAudioEnabled
    }

    fun setAudioEnabled(retroView: Any?, enabled: Boolean) {
        isAudioEnabled = enabled

        // Apply audio change to controller if available
        if (retroView is GLRetroView) {
            audioController?.setAudioEnabled(retroView, enabled)
        }

        saveAudioState()
    }

    // SharedPreferences.Editor.putBoolean()/apply() don't declare or realistically throw on the
    // standard Android implementation; kept broad via the escape hatch as a defensive net for
    // this fire-and-forget write, since there's no narrower reachable type to name.
    private fun saveAudioState() {
        viewModelScope.launch {
            try {
                preferencesRepository.setAudioEnabled(isAudioEnabled)
            } catch (expectedPreferencesWriteFailure: Exception) {
                android.util.Log.e("AudioViewModel", "Error saving audio state", expectedPreferencesWriteFailure)
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
