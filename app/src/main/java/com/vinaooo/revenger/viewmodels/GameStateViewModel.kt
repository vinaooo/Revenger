package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
                // TODO: Implement game reset logic
                // - Save temporary state if needed
                // - Reset the emulator
                // - Restore settings

                onComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("GameStateViewModel", "Error resetting game", e)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveState(slot: Int = 0) {
        // TODO: Implement save state
    }

    @Suppress("UNUSED_PARAMETER")
    fun loadState(slot: Int = 0) {
        // TODO: Implement load state
    }

    @Suppress("UNUSED_PARAMETER")
    fun hasSaveState(slot: Int = 0): Boolean {
        // TODO: Check if save state exists
        return false
    }

    // ========== SPEED CONTROL METHODS ==========

    fun restoreGameSpeedFromPreferences() {
        viewModelScope.launch {
            try {
                val speed = preferencesRepository.getGameSpeedSync()
                setGameSpeed(speed)
            } catch (e: Exception) {
                android.util.Log.e("GameStateViewModel", "Error restoring game speed", e)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setGameSpeed(speed: Int) {
        // TODO: Implement game speed configuration
        // Validate range (normally 1-2)
        // Apply to emulator
    }

    // ========== UTILITY METHODS ==========

    fun setSkipNextTempStateLoad(skip: Boolean) {
        skipNextTempStateLoad = skip
    }

    fun shouldSkipNextTempStateLoad(): Boolean = skipNextTempStateLoad
}
