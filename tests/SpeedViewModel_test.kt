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
    fun `estado inicial nao esta em fast forward`() {
        assertFalse(viewModel.getFastForwardState())
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

    // Regression test for the narrowed ClassCastException catch in loadFastForwardState(): a value
    // of the wrong type under the preference key (e.g. left over from a preferences-format change)
    // must fall back to the documented default instead of crashing construction.
    @Test
    fun `construcao com valor de tipo errado para fast_forward_enabled usa false como padrao`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("revenger_prefs", Application.MODE_PRIVATE)
                .edit()
                .putString("fast_forward_enabled", "not-a-boolean")
                .commit()

        val corrupted = SpeedViewModel(app)

        assertFalse(corrupted.getFastForwardState())
    }
}
