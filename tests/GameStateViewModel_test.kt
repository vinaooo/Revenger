package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.Dispatchers
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
class GameStateViewModel_test {

    private lateinit var viewModel: GameStateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("revenger_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        viewModel = GameStateViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resetGame emite ResetGame e chama o callback de conclusao`() {
        var completed = false

        viewModel.resetGame { completed = true }

        assertTrue(completed)
        assertTrue(viewModel.eventFlow.value is GameStateViewModel.GameStateEvent.ResetGame)
    }

    @Test
    fun `resetGame sem callback nao lanca excecao`() {
        viewModel.resetGame()

        assertTrue(viewModel.eventFlow.value is GameStateViewModel.GameStateEvent.ResetGame)
    }

    @Test
    fun `saveState emite SaveState com o slot informado`() {
        viewModel.saveState(3)

        val event = viewModel.eventFlow.value
        assertTrue(event is GameStateViewModel.GameStateEvent.SaveState)
        assertEquals(3, (event as GameStateViewModel.GameStateEvent.SaveState).slot)
    }

    @Test
    fun `loadState emite LoadState com o slot informado`() {
        viewModel.loadState(5)

        val event = viewModel.eventFlow.value
        assertTrue(event is GameStateViewModel.GameStateEvent.LoadState)
        assertEquals(5, (event as GameStateViewModel.GameStateEvent.LoadState).slot)
    }

    @Test
    fun `hasSaveState emite CheckSaveState mas sempre retorna false (nao implementado)`() {
        // Documenta o comportamento atual: hasSaveState() nunca verifica de verdade o slot,
        // apenas emite o evento e retorna false incondicionalmente.
        val result = viewModel.hasSaveState(2)

        assertFalse(result)
        val event = viewModel.eventFlow.value
        assertTrue(event is GameStateViewModel.GameStateEvent.CheckSaveState)
        assertEquals(2, (event as GameStateViewModel.GameStateEvent.CheckSaveState).slot)
    }

    @Test
    fun `setGameSpeed emite SetGameSpeed com a velocidade informada`() {
        viewModel.setGameSpeed(2)

        val event = viewModel.eventFlow.value
        assertTrue(event is GameStateViewModel.GameStateEvent.SetGameSpeed)
        assertEquals(2, (event as GameStateViewModel.GameStateEvent.SetGameSpeed).speed)
    }

    @Test
    fun `setSkipNextTempStateLoad e shouldSkipNextTempStateLoad refletem o valor definido`() {
        assertFalse(viewModel.shouldSkipNextTempStateLoad())

        viewModel.setSkipNextTempStateLoad(true)

        assertTrue(viewModel.shouldSkipNextTempStateLoad())
    }
}
