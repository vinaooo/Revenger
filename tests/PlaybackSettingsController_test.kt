package com.vinaooo.revenger.viewmodels.menu

import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.SpeedViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PlaybackSettingsController]'s [RetroView]/[SpeedController]/[SharedPreferences]/
 * [AudioViewModel]/[SpeedViewModel] dependencies are read through provider lambdas backed by
 * local vars here, standing in for `GameActivityViewModel`'s own fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PlaybackSettingsController_test {

    private var currentRetroView: RetroView? = null
    private var currentSpeedController: SpeedController? = null
    private var currentSharedPreferences: SharedPreferences? = null
    private lateinit var audioViewModel: AudioViewModel
    private lateinit var speedViewModel: SpeedViewModel
    private lateinit var controller: PlaybackSettingsController

    @Before
    fun setUp() {
        currentRetroView = null
        currentSpeedController = null
        currentSharedPreferences = null
        audioViewModel = mockk(relaxed = true)
        speedViewModel = mockk(relaxed = true)
        controller =
                PlaybackSettingsController(
                        retroView = { currentRetroView },
                        speedController = { currentSpeedController },
                        sharedPreferences = { currentSharedPreferences },
                        audioViewModel = { audioViewModel },
                        speedViewModel = { speedViewModel }
                )
    }

    // --- setAudioEnabled ---

    @Test
    fun `setAudioEnabled sem retroView delega ao audioViewModel com view nula`() {
        controller.setAudioEnabled(true)

        verify(exactly = 1) { audioViewModel.setAudioEnabled(null, true) }
    }

    @Test
    fun `setAudioEnabled com retroView delega ao audioViewModel com a view`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView

        controller.setAudioEnabled(false)

        verify(exactly = 1) { audioViewModel.setAudioEnabled(glRetroView, false) }
    }

    // --- setGameSpeed ---

    @Test
    fun `setGameSpeed sem retroView nao chama speedController`() {
        val speedController = mockk<SpeedController>(relaxed = true)
        currentSpeedController = speedController

        controller.setGameSpeed(2)

        verify(exactly = 0) { speedController.setSpeed(any(), any()) }
    }

    @Test
    fun `setGameSpeed com retroView mas sem speedController nao lanca`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView
        currentSpeedController = null

        controller.setGameSpeed(2)
    }

    @Test
    fun `setGameSpeed com retroView e speedController chama setSpeed`() {
        val speedController = mockk<SpeedController>(relaxed = true)
        currentSpeedController = speedController
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView

        controller.setGameSpeed(3)

        verify(exactly = 1) { speedController.setSpeed(glRetroView, 3) }
    }

    // --- setFastForwardEnabled ---

    @Test
    fun `setFastForwardEnabled true chama enableFastForward e grava 2 nas preferencias`() {
        val prefs =
                ApplicationProvider.getApplicationContext<android.app.Application>()
                        .getSharedPreferences(
                                "playback_settings_controller_test",
                                android.content.Context.MODE_PRIVATE
                        )
        currentSharedPreferences = prefs

        controller.setFastForwardEnabled(true)

        verify(exactly = 1) { speedViewModel.enableFastForward(null) }
        assertEquals(2, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
    }

    @Test
    fun `setFastForwardEnabled false chama disableFastForward e grava 1 nas preferencias`() {
        val prefs =
                ApplicationProvider.getApplicationContext<android.app.Application>()
                        .getSharedPreferences(
                                "playback_settings_controller_test",
                                android.content.Context.MODE_PRIVATE
                        )
        currentSharedPreferences = prefs

        controller.setFastForwardEnabled(false)

        verify(exactly = 1) { speedViewModel.disableFastForward(null) }
        assertEquals(1, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
    }

    @Test
    fun `setFastForwardEnabled sem sharedPreferences nao lanca`() {
        currentSharedPreferences = null

        controller.setFastForwardEnabled(true)

        verify(exactly = 1) { speedViewModel.enableFastForward(null) }
    }
}
