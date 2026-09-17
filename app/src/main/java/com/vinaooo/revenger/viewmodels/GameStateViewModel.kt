package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import com.vinaooo.revenger.retroview.RetroView
import kotlinx.coroutines.launch

/**
 * ViewModel specialized in managing game/emulation state. Responsible for reset,
 * save/load states, and general game control.
 */
class GameStateViewModel(application: Application) : AndroidViewModel(application) {

    sealed class GameStateEvent {
        object Idle : GameStateEvent()
        object ResetGame : GameStateEvent()
        data class SaveState(val slot: Int) : GameStateEvent()
        data class LoadState(val slot: Int) : GameStateEvent()
        data class CheckSaveState(val slot: Int) : GameStateEvent()
        data class SetGameSpeed(val speed: Int) : GameStateEvent()
    }

    private val _eventFlow = MutableStateFlow<GameStateEvent>(GameStateEvent.Idle)
    val eventFlow: StateFlow<GameStateEvent> = _eventFlow.asStateFlow()


    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )

    // References to RetroView
    private var retroView: RetroView? = null
    private var retroViewUtils: com.vinaooo.revenger.utils.RetroViewUtils? = null

    // Game state
    private var skipNextTempStateLoad = false

    fun setRetroView(retroView: RetroView?) {
        this.retroView = retroView
    }

    fun setRetroViewUtils(utils: com.vinaooo.revenger.utils.RetroViewUtils?) {
        retroViewUtils = utils
    }

    // ========== GAME CONTROL METHODS ==========

    fun resetGame(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _eventFlow.value = GameStateEvent.ResetGame

                // onComplete is an arbitrary caller-supplied callback (UI code reacting to the
                // reset); its reachable exceptions aren't enumerable from this ViewModel, so this
                // stays broad via the escape hatch to preserve never letting it crash the coroutine.
                onComplete?.invoke()
            } catch (expectedCallbackFailure: Exception) {
                android.util.Log.e("GameStateViewModel", "Error resetting game", expectedCallbackFailure)
            }
        }
    }

    fun saveState(slot: Int = 0) {
        _eventFlow.value = GameStateEvent.SaveState(slot)
    }

    fun loadState(slot: Int = 0) {
        _eventFlow.value = GameStateEvent.LoadState(slot)
    }

    fun hasSaveState(slot: Int = 0): Boolean {
        _eventFlow.value = GameStateEvent.CheckSaveState(slot)
        return false
    }

    // ========== SPEED CONTROL METHODS ==========

    // SharedPreferences.getInt() throws ClassCastException if a value stored under that key
    // isn't an Int (e.g. a stale value from a preferences-format change).
    fun restoreGameSpeedFromPreferences() {
        viewModelScope.launch {
            try {
                val speed = preferencesRepository.getGameSpeedSync()
                setGameSpeed(speed)
            } catch (e: ClassCastException) {
                android.util.Log.e("GameStateViewModel", "Error restoring game speed", e)
            }
        }
    }

    fun setGameSpeed(speed: Int) {
        _eventFlow.value = GameStateEvent.SetGameSpeed(speed)
    }

    // ========== UTILITY METHODS ==========

    fun setSkipNextTempStateLoad(skip: Boolean) {
        skipNextTempStateLoad = skip
    }

    fun shouldSkipNextTempStateLoad(): Boolean = skipNextTempStateLoad
}
