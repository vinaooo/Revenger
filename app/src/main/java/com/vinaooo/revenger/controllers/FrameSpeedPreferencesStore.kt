package com.vinaooo.revenger.controllers

import android.content.SharedPreferences
import com.vinaooo.revenger.utils.PreferencesConstants

/**
 * Reads/writes the persisted frame-speed preference used by [SpeedController]. Split out purely
 * to keep that class under the project's function-count threshold; [getCurrentSpeed] and
 * [getFastForwardState] are exposed back on [SpeedController] via Kotlin interface delegation
 * (`by`) since they are called from outside that file (tests call them directly); [saveSpeedState]
 * is only ever called from within [SpeedController] and stays a plain method on this class.
 */
interface FrameSpeedPreferences {
    /**
     * Gets the current speed from preferences.
     * Never returns 0 (paused) - treated as normal speed.
     * @return current speed (1 = normal, > 1 = fast forward)
     */
    fun getCurrentSpeed(): Int

    /**
     * Gets the fast forward state from preferences.
     * @return true if fast forward is active, false otherwise
     */
    fun getFastForwardState(): Boolean
}

class FrameSpeedPreferencesStore(private val sharedPreferences: SharedPreferences) :
        FrameSpeedPreferences {

    override fun getCurrentSpeed(): Int {
        val savedSpeed = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
        // CRITICAL: If it's 0 (paused), return 1 (normal)
        return if (savedSpeed == 0) 1 else savedSpeed
    }

    override fun getFastForwardState(): Boolean {
        val savedSpeed = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
        return savedSpeed > 1
    }

    /**
     * Saves the current speed state to preferences.
     * @param speed speed to be saved
     */
    fun saveSpeedState(speed: Int) {
        with(sharedPreferences.edit()) {
            putInt(PreferencesConstants.PREF_FRAME_SPEED, speed)
            apply()
        }
    }
}
