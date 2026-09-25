package com.vinaooo.revenger.viewmodels.menu

import android.content.SharedPreferences
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.SpeedViewModel

/** [GameActivityViewModel]'s audio/speed/fast-forward playback settings surface. */
interface PlaybackSettingsFacade {
    fun setAudioEnabled(enabled: Boolean)
    fun setGameSpeed(speed: Int)
    fun setFastForwardEnabled(enabled: Boolean)
}

/**
 * Implementation of [PlaybackSettingsFacade]. All five dependencies are read through provider
 * lambdas rather than captured, so the owning ViewModel's current field values are always used.
 */
class PlaybackSettingsController(
        private val retroView: () -> RetroView?,
        private val speedController: () -> SpeedController?,
        private val sharedPreferences: () -> SharedPreferences?,
        private val audioViewModel: () -> AudioViewModel,
        private val speedViewModel: () -> SpeedViewModel
) : PlaybackSettingsFacade {

    /**
     * Audio control using modular controller
     * @param enabled true to turn on, false to turn off
     */
    override fun setAudioEnabled(enabled: Boolean) {
        audioViewModel().setAudioEnabled(retroView()?.view, enabled)
    }

    /**
     * Controle de velocidade usando controller modular
     * @param speed velocidade desejada (1 = normal, > 1 = fast forward)
     */
    override fun setGameSpeed(speed: Int) {
        retroView()?.let { speedController()?.setSpeed(it.view, speed) }
    }

    /** Define fast forward enabled/disabled sem aplicar imediatamente (usado pelo menu Settings) */
    override fun setFastForwardEnabled(enabled: Boolean) {
        if (enabled) {
            speedViewModel().enableFastForward(null) // Pass null to avoid immediate application
            // Also save the speed value to preferences for menu closure restoration
            sharedPreferences()?.edit()?.putInt(PreferencesConstants.PREF_FRAME_SPEED, 2)?.apply()
        } else {
            speedViewModel().disableFastForward(null) // Pass null to avoid immediate application
            // Also save the speed value to preferences for menu closure restoration
            sharedPreferences()?.edit()?.putInt(PreferencesConstants.PREF_FRAME_SPEED, 1)?.apply()
        }
    }
}
