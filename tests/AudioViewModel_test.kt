package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.AudioController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AudioViewModel_test {

    private lateinit var viewModel: AudioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("revenger_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        viewModel = AudioViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial de audio comeca habilitado quando nao ha preferencia salva`() {
        assertTrue(viewModel.getAudioState())
    }

    @Test
    fun `toggleAudio emite evento com o retroView e retorna o estado atual (sem altera-lo)`() {
        val retroView = mockk<GLRetroView>()

        val stateBeforeToggle = viewModel.toggleAudio(retroView)

        assertEquals(viewModel.getAudioState(), stateBeforeToggle)
        assertTrue(viewModel.eventFlow.value is AudioViewModel.AudioEvent.ToggleAudio)
    }

    @Test
    fun `setAudioEnabled com GLRetroView aplica no controller e atualiza o estado`() {
        val retroView = mockk<GLRetroView>(relaxed = true)
        val controller = mockk<AudioController>(relaxed = true)
        viewModel.setAudioController(controller)

        viewModel.setAudioEnabled(retroView, false)

        assertFalse(viewModel.getAudioState())
        verify { controller.setAudioEnabled(retroView, false) }
    }

    @Test
    fun `setAudioEnabled com objeto que nao e GLRetroView atualiza o estado sem tocar no controller`() {
        val controller = mockk<AudioController>(relaxed = true)
        viewModel.setAudioController(controller)

        viewModel.setAudioEnabled(null, false)

        assertFalse(viewModel.getAudioState())
        verify(exactly = 0) { controller.setAudioEnabled(any(), any()) }
    }

    @Test
    fun `getAudioController retorna o controller configurado via setAudioController`() {
        val controller = mockk<AudioController>(relaxed = true)

        viewModel.setAudioController(controller)

        assertEquals(controller, viewModel.getAudioController())
    }
}
