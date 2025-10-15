package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.PreferencesConstants

/**
 * Modular controller to manage emulator speed functionalities (fast forward) Allows centralized
 * speed control that can be reused in different parts of the system
 */
class SpeedController(
        private val context: Context,
        private val sharedPreferences: SharedPreferences
) {
    // Fast forward speed configured in config.xml
    private val fastForwardSpeed =
            context.resources.getInteger(R.integer.config_fast_forward_multiplier)

    /**
     * Toggles between normal speed (1x) and fast forward (configurable)
     * @param retroView RetroView where to apply the change
     * @return new fast forward state (true = active, false = inactive)
     */
    fun toggleFastForward(retroView: GLRetroView): Boolean {
        val newSpeed = if (retroView.frameSpeed == 1) fastForwardSpeed else 1
        retroView.frameSpeed = newSpeed

        // Save the new state immediately
        saveSpeedState(newSpeed)

        return newSpeed > 1
    }

    /**
     * Sets the specific speed
     * @param retroView RetroView where to apply the change
     * @param speed desired speed (1 = normal, > 1 = fast forward)
     */
    fun setSpeed(retroView: GLRetroView, speed: Int) {
        retroView.frameSpeed = speed
        saveSpeedState(speed)
    }

    /**
     * Enables fast forward (configured speed)
     * @param retroView RetroView where to apply the change
     */
    fun enableFastForward(retroView: GLRetroView) {
        setSpeed(retroView, fastForwardSpeed)
    }

    /**
     * Pauses the game (frameSpeed = 0)
     * @param retroView RetroView where to apply the pause
     */
    fun pause(retroView: GLRetroView) {
        retroView.frameSpeed = 0
    }

    /**
     * Resumes the game (returns to normal speed)
     * @param retroView RetroView where to remove the pause
     */
    fun resume(retroView: GLRetroView) {
        retroView.frameSpeed = 1
    }

    /**
     * Checks if fast forward is active
     * @param retroView RetroView to check the state
     * @return true if fast forward is active, false otherwise
     */
    fun isFastForwardActive(retroView: GLRetroView): Boolean {
        return retroView.frameSpeed > 1
    }

    /**
     * Gets the fast forward state from preferences
     * @return true if fast forward is active, false otherwise
     */
    fun getFastForwardState(): Boolean {
        val savedSpeed = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
        return savedSpeed > 1
    }

    /**
     * Gets the current speed from preferences FIX: Never return 0 (paused) - treat as normal speed
     * @return current speed (1 = normal, > 1 = fast forward)
     */
    fun getCurrentSpeed(): Int {
        val savedSpeed = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
        // CRITICAL: If it's 0 (paused), return 1 (normal)
        return if (savedSpeed == 0) 1 else savedSpeed
    }

    /**
     * Gets the current speed from RetroView
     * @param retroView RetroView to check the speed
     * @return current speed
     */
    fun getCurrentSpeed(retroView: GLRetroView): Int {
        return retroView.frameSpeed
    }

    /**
     * Initializes the speed state in RetroView based on saved preferences FIX: Never apply
     * frameSpeed = 0 (paused) on initialization If savedSpeed == 0, it means the app was closed
     * with menu open In this case, restore to 1 (normal speed)
     * @param retroView RetroView to configure
     */
    fun initializeSpeedState(retroView: GLRetroView) {
        val savedSpeed = getCurrentSpeed()
        // CRITICAL: Ensure it's never 0 (paused)
        val safeSpeed = if (savedSpeed == 0) 1 else savedSpeed
        retroView.frameSpeed = safeSpeed
    }

    /**
     * Saves the current speed state to preferences
     * @param speed speed to be saved
     */
    private fun saveSpeedState(speed: Int) {
        with(sharedPreferences.edit()) {
            putInt(PreferencesConstants.PREF_FRAME_SPEED, speed)
            apply()
        }
    }

    /**
     * Gets textual description of the current speed state
     * @return String with current state ("Fast Forward Active" or "Normal Speed")
     */
    fun getSpeedStateDescription(): String {
        return if (getFastForwardState()) {
            context.getString(R.string.fast_forward_active)
        } else {
            context.getString(R.string.fast_forward_inactive)
        }
    }

    /**
     * Gets the appropriate icon ID for the current speed state
     * @return Resource ID of the icon
     */
    fun getSpeedIconResource(): Int {
        // For now uses the same icon, but can be differentiated in the future
        return R.drawable.ic_fast_forward_24
    }

    /**
     * Gets the configured fast forward speed
     * @return speed multiplier for fast forward
     */
    fun getFastForwardSpeed(): Int {
        return fastForwardSpeed
    }
}
