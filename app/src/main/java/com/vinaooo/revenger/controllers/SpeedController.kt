package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig

/**
 * Modular controller to manage emulator speed functionalities (fast forward) Allows centralized
 * speed control that can be reused in different parts of the system
 */
class SpeedController(
        private val context: Context,
        private val sharedPreferences: SharedPreferences,
        private val appConfig: AppConfig,
        private val framePreferences: FrameSpeedPreferencesStore =
                FrameSpeedPreferencesStore(sharedPreferences)
) : FrameSpeedPreferences by framePreferences,
        SpeedInfo by SpeedInfoProvider(
                context,
                appConfig.getFastForwardMultiplier(),
                isFastForwardActive = { framePreferences.getFastForwardState() }
        ) {
    // Fast forward speed configured in config.xml
    private val fastForwardSpeed = appConfig.getFastForwardMultiplier()

    /**
     * Toggles between normal speed (1x) and fast forward (configurable)
     * @param retroView RetroView where to apply the change
     * @return new fast forward state (true = active, false = inactive)
     */
    fun toggleFastForward(retroView: GLRetroView): Boolean {
        val newSpeed = if (retroView.frameSpeed == 1) fastForwardSpeed else 1
        retroView.frameSpeed = newSpeed

        // Save the new state immediately
        framePreferences.saveSpeedState(newSpeed)

        return newSpeed > 1
    }

    /**
     * Sets the specific speed
     * @param retroView RetroView where to apply the change
     * @param speed desired speed (1 = normal, > 1 = fast forward)
     */
    fun setSpeed(retroView: GLRetroView, speed: Int) {
        retroView.frameSpeed = speed
        framePreferences.saveSpeedState(speed)
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
     * Restores the speed from preferences to the RetroView Used after game reset to ensure
     * framespeed is properly restored
     * @param retroView RetroView to configure
     */
    fun restoreSpeedFromPreferences(retroView: GLRetroView?) {
        retroView?.let {
            val savedSpeed = getCurrentSpeed()
            // CRITICAL: Ensure it's never 0 (paused) - treat as normal speed
            val safeSpeed = if (savedSpeed == 0) 1 else savedSpeed
            it.frameSpeed = safeSpeed
        }
    }
}
