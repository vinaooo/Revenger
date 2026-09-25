package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.PreferencesConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.setAudioEnabled], [setGameSpeed] and
 * [setFastForwardEnabled] -- the playback-settings setters, written before extracting this
 * cluster into a dedicated class under `viewmodels/menu/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_playbackSettings_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // --- setAudioEnabled ---

    @Test
    fun `setAudioEnabled sem retroView delega ao audioViewModel com view nula`() {
        val audioViewModel = mockk<AudioViewModel>(relaxed = true)
        setPrivateField(viewModel, "audioViewModel", audioViewModel)
        viewModel.retroView = null

        viewModel.setAudioEnabled(true)

        verify(exactly = 1) { audioViewModel.setAudioEnabled(null, true) }
    }

    @Test
    fun `setAudioEnabled com retroView delega ao audioViewModel com a view`() {
        val audioViewModel = mockk<AudioViewModel>(relaxed = true)
        setPrivateField(viewModel, "audioViewModel", audioViewModel)
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel.retroView = retroView

        viewModel.setAudioEnabled(false)

        verify(exactly = 1) { audioViewModel.setAudioEnabled(glRetroView, false) }
    }

    // --- setGameSpeed ---

    @Test
    fun `setGameSpeed sem retroView nao chama speedController`() {
        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)
        viewModel.retroView = null

        viewModel.setGameSpeed(2)

        verify(exactly = 0) { speedController.setSpeed(any(), any()) }
    }

    @Test
    fun `setGameSpeed com retroView mas sem speedController nao lanca`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel.retroView = retroView
        setPrivateField(viewModel, "speedController", null)

        viewModel.setGameSpeed(2)
    }

    @Test
    fun `setGameSpeed com retroView e speedController chama setSpeed`() {
        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel.retroView = retroView

        viewModel.setGameSpeed(3)

        verify(exactly = 1) { speedController.setSpeed(glRetroView, 3) }
    }

    // --- setFastForwardEnabled ---

    @Test
    fun `setFastForwardEnabled true chama enableFastForward e grava 2 nas preferencias`() {
        val speedViewModel = mockk<SpeedViewModel>(relaxed = true)
        setPrivateField(viewModel, "speedViewModel", speedViewModel)
        val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                        .getSharedPreferences("playback_settings_test", android.content.Context.MODE_PRIVATE)
        setPrivateField(viewModel, "sharedPreferences", prefs)

        viewModel.setFastForwardEnabled(true)

        verify(exactly = 1) { speedViewModel.enableFastForward(null) }
        assertEquals(2, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
    }

    @Test
    fun `setFastForwardEnabled false chama disableFastForward e grava 1 nas preferencias`() {
        val speedViewModel = mockk<SpeedViewModel>(relaxed = true)
        setPrivateField(viewModel, "speedViewModel", speedViewModel)
        val prefs =
                ApplicationProvider.getApplicationContext<Application>()
                        .getSharedPreferences("playback_settings_test", android.content.Context.MODE_PRIVATE)
        setPrivateField(viewModel, "sharedPreferences", prefs)

        viewModel.setFastForwardEnabled(false)

        verify(exactly = 1) { speedViewModel.disableFastForward(null) }
        assertEquals(1, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
    }

    @Test
    fun `setFastForwardEnabled sem sharedPreferences nao lanca`() {
        val speedViewModel = mockk<SpeedViewModel>(relaxed = true)
        setPrivateField(viewModel, "speedViewModel", speedViewModel)
        setPrivateField<android.content.SharedPreferences?>(viewModel, "sharedPreferences", null)

        viewModel.setFastForwardEnabled(true)

        assertFalse(false) // no exception thrown is the assertion
    }
}
