package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import com.vinaooo.revenger.viewmodels.speed.SpeedStatePersistence

/**
 * ViewModel specialized in game speed management. Responsible for fast-forward control
 * and emulation speed.
 */
class SpeedViewModel(application: Application) : AndroidViewModel(application) {

    sealed class SpeedEvent {
        object Idle : SpeedEvent()
        data class ToggleFastForward(val retroView: Any?) : SpeedEvent()
        data class SetGameSpeed(val speed: Int) : SpeedEvent()
        data class ApplySpeedToController(val controller: SpeedController) : SpeedEvent()
    }

    private val _eventFlow = MutableStateFlow<SpeedEvent>(SpeedEvent.Idle)
    val eventFlow: StateFlow<SpeedEvent> = _eventFlow.asStateFlow()


    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )
    private var speedController: SpeedController? = null
    private val statePersistence = SpeedStatePersistence(preferencesRepository, viewModelScope)

    // Speed state
    private var currentSpeed: Int = 1
    private var isFastForwardEnabled: Boolean = false

    init {
        statePersistence.loadSpeedState(
                onLoaded = { currentSpeed = it },
                onFailure = {
                    android.util.Log.e("SpeedViewModel", "Error loading speed state", it)
                    currentSpeed = 1 // Default
                }
        )
        statePersistence.loadFastForwardState(
                onLoaded = { isFastForwardEnabled = it },
                onFailure = {
                    android.util.Log.e("SpeedViewModel", "Error loading fast-forward state", it)
                    isFastForwardEnabled = false // Default
                }
        )
    }

    // ========== SPEED CONTROL METHODS ==========

    fun toggleFastForward(retroView: Any? = null): Boolean {
        _eventFlow.value = SpeedEvent.ToggleFastForward(retroView)
        isFastForwardEnabled = !isFastForwardEnabled
        return isFastForwardEnabled
    }

    fun setGameSpeed(speed: Int) {
        // Validar range (normalmente 1-2)
        val validSpeed = speed.coerceIn(1, 2)

        _eventFlow.value = SpeedEvent.SetGameSpeed(validSpeed)
        currentSpeed = validSpeed
        statePersistence.saveSpeedState(currentSpeed) {
            android.util.Log.e("SpeedViewModel", "Error saving speed state", it)
        }
    }

    private fun saveFastForwardState() {
        statePersistence.saveFastForwardState(isFastForwardEnabled) {
            android.util.Log.e("SpeedViewModel", "Error saving fast-forward state", it)
        }
    }

    // ========== GETTERS ==========

    fun getFastForwardState(): Boolean = isFastForwardEnabled

    fun getGameSpeed(): Int = currentSpeed

    fun getSpeedController(): SpeedController? = speedController

    // ========== SETTERS ==========

    fun enableFastForward(retroView: Any? = null) {
        isFastForwardEnabled = true

        // Apply fast-forward to controller if available
        if (retroView is GLRetroView) {
            speedController?.enableFastForward(retroView)
        }

        // Save state
        saveFastForwardState()
    }

    fun disableFastForward(retroView: Any? = null) {
        isFastForwardEnabled = false

        // Apply normal speed to controller if available
        if (retroView is GLRetroView) {
            speedController?.setSpeed(retroView, 1)
        }

        // Save state
        saveFastForwardState()
    }

    fun setSpeedController(controller: SpeedController) {
        speedController = controller
        _eventFlow.value = SpeedEvent.ApplySpeedToController(controller)
    }
}
