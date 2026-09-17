package com.vinaooo.revenger.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [StateFileStore] was split out of [RetroViewUtils] purely to keep that class under the
 * project's function-count threshold, taking over the raw save-state/temp-state/SRAM file I/O
 * (previously private/internal-only implementation details of [RetroViewUtils]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class StateFileStore_test {

    private lateinit var storage: Storage
    private lateinit var store: StateFileStore
    private lateinit var glRetroView: GLRetroView
    private lateinit var retroView: RetroView

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storage = Storage(context)
        // Clean slate: Storage is a process-wide singleton pattern but this test constructs its
        // own instance directly, so start from files that don't exist yet.
        storage.state.delete()
        storage.tempState.delete()
        storage.sram.delete()

        store = StateFileStore(storage)

        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
    }

    @Test
    fun `getSaveStatePath retorna o caminho absoluto do arquivo de state`() {
        assertNotNull(store.getSaveStatePath())
    }

    @Test
    fun `loadState nao faz nada quando o arquivo de state nao existe`() {
        store.loadState(retroView)

        verify(exactly = 0) { glRetroView.unserializeState(any()) }
    }

    @Test
    fun `loadState desserializa o conteudo salvo quando o arquivo existe`() {
        val bytes = byteArrayOf(1, 2, 3)
        storage.state.writeBytes(bytes)

        store.loadState(retroView)

        verify { glRetroView.unserializeState(bytes) }
    }

    @Test
    fun `loadTempState desserializa o conteudo salvo do tempstate`() {
        val bytes = byteArrayOf(9, 9, 9)
        storage.tempState.writeBytes(bytes)

        store.loadTempState(retroView)

        verify { glRetroView.unserializeState(bytes) }
    }

    @Test
    fun `loadTempState nao faz nada quando o arquivo nao existe`() {
        store.loadTempState(retroView)

        verify(exactly = 0) { glRetroView.unserializeState(any()) }
    }

    @Test
    fun `saveState escreve o resultado de serializeState no arquivo de state`() {
        val bytes = byteArrayOf(5, 6, 7)
        every { glRetroView.serializeState() } returns bytes

        store.saveState(retroView)

        assertNotNull(storage.state.readBytes())
        org.junit.Assert.assertArrayEquals(bytes, storage.state.readBytes())
    }

    @Test
    fun `saveTempState escreve o resultado de serializeState no arquivo de tempstate`() {
        val bytes = byteArrayOf(4, 3, 2)
        every { glRetroView.serializeState() } returns bytes

        store.saveTempState(retroView)

        org.junit.Assert.assertArrayEquals(bytes, storage.tempState.readBytes())
    }

    @Test
    fun `saveSRAM escreve o resultado de serializeSRAM no arquivo de sram`() {
        val bytes = byteArrayOf(7, 8, 9)
        every { glRetroView.serializeSRAM() } returns bytes

        store.saveSRAM(retroView)

        org.junit.Assert.assertArrayEquals(bytes, storage.sram.readBytes())
    }
}
