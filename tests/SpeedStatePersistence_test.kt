package com.vinaooo.revenger.viewmodels.speed

import com.vinaooo.revenger.repositories.PreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SpeedStatePersistence] was split out of `SpeedViewModel` purely to keep that ViewModel under
 * the project's function-count threshold, taking over its load/save preferences plumbing
 * (including the narrowed exception handling regression-tested there before the extraction).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedStatePersistence_test {

    private lateinit var repository: PreferencesRepository
    private lateinit var scope: TestScope
    private lateinit var persistence: SpeedStatePersistence

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scope = TestScope(UnconfinedTestDispatcher())
        persistence = SpeedStatePersistence(repository, scope)
    }

    @Test
    fun `loadSpeedState entrega o valor lido do repositorio`() {
        every { repository.getGameSpeedSync() } returns 2
        var loaded: Int? = null

        persistence.loadSpeedState(onLoaded = { loaded = it }, onFailure = { fail("nao deveria falhar") })

        assertEquals(2, loaded)
    }

    @Test
    fun `loadSpeedState com ClassCastException aciona onFailure em vez de propagar`() {
        every { repository.getGameSpeedSync() } throws ClassCastException("tipo errado")
        var failed = false

        persistence.loadSpeedState(onLoaded = { fail("nao deveria carregar") }, onFailure = { failed = true })

        assertTrue(failed)
    }

    @Test
    fun `loadFastForwardState entrega o valor lido do repositorio`() {
        every { repository.getFastForwardEnabledSync() } returns true
        var loaded: Boolean? = null

        persistence.loadFastForwardState(
                onLoaded = { loaded = it },
                onFailure = { fail("nao deveria falhar") }
        )

        assertTrue(loaded == true)
    }

    @Test
    fun `loadFastForwardState com ClassCastException aciona onFailure em vez de propagar`() {
        every { repository.getFastForwardEnabledSync() } throws ClassCastException("tipo errado")
        var failed = false

        persistence.loadFastForwardState(onLoaded = { fail("nao deveria carregar") }, onFailure = { failed = true })

        assertTrue(failed)
    }

    @Test
    fun `saveSpeedState grava no repositorio sem acionar onFailure`() {
        coEvery { repository.setGameSpeed(any()) } returns Unit
        var failed = false

        persistence.saveSpeedState(2) { failed = true }

        assertFalse(failed)
        coEvery { repository.setGameSpeed(2) }
    }

    @Test
    fun `saveSpeedState com falha na escrita aciona onFailure`() {
        coEvery { repository.setGameSpeed(any()) } throws RuntimeException("falha ao escrever")
        var failed = false

        persistence.saveSpeedState(2) { failed = true }

        assertTrue(failed)
    }

    @Test
    fun `saveFastForwardState grava no repositorio sem acionar onFailure`() {
        coEvery { repository.setFastForwardEnabled(any()) } returns Unit
        var failed = false

        persistence.saveFastForwardState(true) { failed = true }

        assertFalse(failed)
    }

    @Test
    fun `saveFastForwardState com falha na escrita aciona onFailure`() {
        coEvery { repository.setFastForwardEnabled(any()) } throws RuntimeException("falha ao escrever")
        var failed = false

        persistence.saveFastForwardState(true) { failed = true }

        assertTrue(failed)
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
