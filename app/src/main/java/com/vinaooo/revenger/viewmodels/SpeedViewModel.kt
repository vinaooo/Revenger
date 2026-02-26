package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel specialized in game speed management. Responsible for fast-forward control
 * and emulation speed.
 */
class SpeedViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )
    private var speedController: SpeedController? = null

    // Speed state
    private var currentSpeed: Int = 1
    private var isFastForwardEnabled: Boolean = false

    init {
        loadSpeedState()
        loadFastForwardState()
    }

    private fun loadSpeedState() {
        viewModelScope.launch {
            try {
                currentSpeed = preferencesRepository.getGameSpeedSync()
            } catch (e: Exception) {
                android.util.Log.e("SpeedViewModel", "Error loading speed state", e)
                currentSpeed = 1 // Default
            }
        }
    }

    private fun loadFastForwardState() {
        viewModelScope.launch {
            try {
                isFastForwardEnabled = preferencesRepository.getFastForwardEnabledSync()
            } catch (e: Exception) {
                android.util.Log.e("SpeedViewModel", "Error loading fast-forward state", e)
                isFastForwardEnabled = false // Default
            }
        }
    }

    // ========== SPEED CONTROL METHODS ==========

    @Suppress("UNUSED_PARAMETER")
    fun toggleFastForward(retroView: Any? = null): Boolean {
        // TODO: Implement fast-forward toggle
        isFastForwardEnabled = !isFastForwardEnabled
        return isFastForwardEnabled
    }

    @Suppress("UNUSED_PARAMETER")
    fun setGameSpeed(speed: Int) {
        // Validar range (normalmente 1-2)
        val validSpeed = speed.coerceIn(1, 2)

        // TODO: Implement speed configuration in controller
        currentSpeed = validSpeed
        saveSpeedState()
    }

    private fun saveSpeedState() {
        viewModelScope.launch {
            try {
                preferencesRepository.setGameSpeed(currentSpeed)
            } catch (e: Exception) {
                android.util.Log.e("SpeedViewModel", "Error saving speed state", e)
            }
        }
    }

    private fun saveFastForwardState() {
        viewModelScope.launch {
            try {
                preferencesRepository.setFastForwardEnabled(isFastForwardEnabled)
            } catch (e: Exception) {
                android.util.Log.e("SpeedViewModel", "Error saving fast-forward state", e)
            }
        }
    }

    // ========== GETTERS ==========

    fun getFastForwardState(): Boolean = isFastForwardEnabled

    fun getGameSpeed(): Int = currentSpeed

    fun getSpeedController(): SpeedController? = speedController

    // ========== SETTERS ==========

    @Suppress("UNUSED_PARAMETER")
    fun enableFastForward(retroView: Any? = null) {
        isFastForwardEnabled = true

        // Apply fast-forward to controller if available
        if (retroView is GLRetroView) {
            speedController?.enableFastForward(retroView)
        }

        // Save state
        saveFastForwardState()
    }

    @Suppress("UNUSED_PARAMETER")
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
        // TODO: Apply current speed to controller
        // controller.setGameSpeed(currentSpeed)
    }
}
