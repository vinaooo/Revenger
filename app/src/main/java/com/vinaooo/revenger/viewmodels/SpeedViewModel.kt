package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import com.vinaooo.revenger.viewmodels.speed.SpeedStatePersistence

/** ViewModel for the fast-forward state: keeps it, persists it and applies it to the controller. */
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
    private val statePersistence = SpeedStatePersistence(preferencesRepository, viewModelScope)

    // Speed state
    private var isFastForwardEnabled: Boolean = false

    init {
        statePersistence.loadFastForwardState(
                onLoaded = { isFastForwardEnabled = it },
                onFailure = {
                    android.util.Log.e("SpeedViewModel", "Error loading fast-forward state", it)
                    isFastForwardEnabled = false // Default
                }
        )
    }

    // ========== SPEED CONTROL METHODS ==========

    private fun saveFastForwardState() {
        statePersistence.saveFastForwardState(isFastForwardEnabled) {
            android.util.Log.e("SpeedViewModel", "Error saving fast-forward state", it)
        }
    }

    // ========== GETTERS ==========

    fun getFastForwardState(): Boolean = isFastForwardEnabled

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
    }
}
