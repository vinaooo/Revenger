package com.vinaooo.revenger.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.utils.PreferencesConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AudioController_test {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var retroView: GLRetroView
    private lateinit var controller: AudioController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        retroView = mockk(relaxed = true)
        controller = AudioController(context, prefs)
    }

    @Test
    fun `toggleAudio inverte o estado atual do retroView e persiste`() {
        every { retroView.audioEnabled } returns true

        val newState = controller.toggleAudio(retroView)

        assertFalse(newState)
        verify { retroView.audioEnabled = false }
        assertFalse(prefs.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true))
    }

    @Test
    fun `setAudioEnabled aplica o valor no retroView e persiste`() {
        controller.setAudioEnabled(retroView, false)

        verify { retroView.audioEnabled = false }
        assertFalse(prefs.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true))
    }

    @Test
    fun `getAudioState sem preferencia salva retorna true por padrao`() {
        assertTrue(controller.getAudioState())
    }

    @Test
    fun `getAudioState le a preferencia salva`() {
        prefs.edit().putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false).commit()

        assertFalse(controller.getAudioState())
    }

    @Test
    fun `getAudioState com retroView le diretamente da view`() {
        every { retroView.audioEnabled } returns false

        assertFalse(controller.getAudioState(retroView))
    }

    @Test
    fun `initializeAudioState aplica o valor salvo nas preferencias ao retroView`() {
        prefs.edit().putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false).commit()

        controller.initializeAudioState(retroView)

        verify { retroView.audioEnabled = false }
    }

    @Test
    fun `getAudioStateDescription reflete o estado salvo`() {
        prefs.edit().putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false).commit()
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals(
            context.getString(com.vinaooo.revenger.R.string.audio_off),
            controller.getAudioStateDescription(),
        )
    }
}
