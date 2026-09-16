package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
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
class SpeedViewModel_test {

    private lateinit var viewModel: SpeedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("revenger_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        viewModel = SpeedViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial nao esta em fast forward e a velocidade e 1`() {
        assertFalse(viewModel.getFastForwardState())
        assertEquals(1, viewModel.getGameSpeed())
    }

    @Test
    fun `toggleFastForward inverte o estado a cada chamada e emite o evento`() {
        val first = viewModel.toggleFastForward()
        assertTrue(first)
        assertTrue(viewModel.eventFlow.value is SpeedViewModel.SpeedEvent.ToggleFastForward)

        val second = viewModel.toggleFastForward()
        assertFalse(second)
    }

    @Test
    fun `setGameSpeed limita o valor ao intervalo 1 a 2`() {
        viewModel.setGameSpeed(10)
        assertEquals(2, viewModel.getGameSpeed())

        viewModel.setGameSpeed(-5)
        assertEquals(1, viewModel.getGameSpeed())
    }

    @Test
    fun `setGameSpeed emite SetGameSpeed com o valor ja limitado`() {
        viewModel.setGameSpeed(99)

        val event = viewModel.eventFlow.value
        assertTrue(event is SpeedViewModel.SpeedEvent.SetGameSpeed)
        assertEquals(2, (event as SpeedViewModel.SpeedEvent.SetGameSpeed).speed)
    }

    @Test
    fun `enableFastForward com GLRetroView aplica no controller`() {
        val retroView = mockk<GLRetroView>(relaxed = true)
        val controller = mockk<SpeedController>(relaxed = true)
        viewModel.setSpeedController(controller)

        viewModel.enableFastForward(retroView)

        assertTrue(viewModel.getFastForwardState())
        verify { controller.enableFastForward(retroView) }
    }

    @Test
    fun `disableFastForward com GLRetroView aplica velocidade normal no controller`() {
        val retroView = mockk<GLRetroView>(relaxed = true)
        val controller = mockk<SpeedController>(relaxed = true)
        viewModel.setSpeedController(controller)

        viewModel.disableFastForward(retroView)

        assertFalse(viewModel.getFastForwardState())
        verify { controller.setSpeed(retroView, 1) }
    }

    @Test
    fun `enableFastForward sem GLRetroView atualiza o estado sem tocar no controller`() {
        val controller = mockk<SpeedController>(relaxed = true)
        viewModel.setSpeedController(controller)

        viewModel.enableFastForward(null)

        assertTrue(viewModel.getFastForwardState())
        verify(exactly = 0) { controller.enableFastForward(any()) }
    }

    @Test
    fun `setSpeedController emite ApplySpeedToController com o controller informado`() {
        val controller = mockk<SpeedController>(relaxed = true)

        viewModel.setSpeedController(controller)

        assertEquals(controller, viewModel.getSpeedController())
        val event = viewModel.eventFlow.value
        assertTrue(event is SpeedViewModel.SpeedEvent.ApplySpeedToController)
        assertEquals(controller, (event as SpeedViewModel.SpeedEvent.ApplySpeedToController).controller)
    }
}
