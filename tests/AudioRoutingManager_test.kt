package com.vinaooo.revenger.managers

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AudioRoutingManagerTest {

    private lateinit var audioManager: AudioManager
    private lateinit var shadowAudioManager: ShadowAudioManager
    private lateinit var manager: AudioRoutingManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        shadowAudioManager = shadowOf(audioManager)
        manager = AudioRoutingManager(audioManager)
    }

    @Test
    fun `requestFocus retorna true quando o foco e concedido`() {
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        assertTrue(manager.requestFocus())
    }

    @Test
    fun `requestFocus retorna false quando o foco e negado`() {
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

        assertFalse(manager.requestFocus())
    }

    @Test
    fun `requestFocus pede foco do tipo GAIN`() {
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        manager.requestFocus()

        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN,
            shadowAudioManager.lastAudioFocusRequest.durationHint,
        )
    }

    @Test
    fun `abandonFocus nao lanca excecao quando nenhum foco foi solicitado antes`() {
        manager.abandonFocus()

        assertNull(shadowAudioManager.lastAbandonedAudioFocusRequest)
    }

    @Test
    fun `abandonFocus libera o foco solicitado por requestFocus`() {
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        manager.requestFocus()

        manager.abandonFocus()

        assertNotNull(shadowAudioManager.lastAbandonedAudioFocusRequest)
    }
}
