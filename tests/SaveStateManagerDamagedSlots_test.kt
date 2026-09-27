package com.vinaooo.revenger.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.models.SaveSlotPayload
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Manage Saves on slots that are damaged (corrupt or missing `metadata.json`) or can't be written.
 * The menu shows a damaged save as "Slot N", so rename, copy and move must work on it (repairing
 * the file) instead of crashing or half-finishing; and an operation that fails must say so and
 * never leave half a save behind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveStateManagerDamagedSlots_test {

    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
        manager = SaveStateManager.getInstance(context)
    }

    @After
    fun tearDown() {
        savesDir.walkTopDown().forEach { it.setReadable(true); it.setWritable(true) }
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

    private fun slotDir(number: Int) = File(savesDir, "slot_$number")

    private fun save(number: Int, bytes: ByteArray, name: String) {
        assertTrue(manager.saveToSlot(number, SaveSlotPayload(bytes, name = name)))
    }

    private fun corruptMetadata(number: Int) = File(slotDir(number), "metadata.json").writeText("{ not json")

    private fun metadataJson(number: Int) = JSONObject(File(slotDir(number), "metadata.json").readText())

    // Regression: updateName parsed the file itself, so a corrupt one threw an uncaught
    // JSONException that crashed the app from Manage Saves.
    @Test
    fun `renomear um save com metadata corrompida funciona e conserta o arquivo`() {
        save(2, byteArrayOf(1), "Before")
        corruptMetadata(2)

        assertTrue(manager.renameSlot(2, "After"))

        val json = metadataJson(2)
        assertEquals("After", json.getString("name"))
        assertEquals(2, json.getInt("slotNumber"))
        assertEquals("After", manager.getSlot(2).name)
    }

    @Test
    fun `renomear um save sem metadata cria o arquivo com o novo nome`() {
        save(3, byteArrayOf(1), "Before")
        File(slotDir(3), "metadata.json").delete()

        assertTrue(manager.renameSlot(3, "Named"))

        assertEquals("Named", manager.getSlot(3).name)
        assertEquals(3, metadataJson(3).getInt("slotNumber"))
    }

    @Test
    fun `renomear quando a metadata nao pode ser gravada retorna false`() {
        save(4, byteArrayOf(1), "Before")
        File(slotDir(4), "metadata.json").apply { delete(); mkdirs() }

        assertFalse(manager.renameSlot(4, "After"))
    }

    // Regression: the corrupt metadata made copySlot fail after the target was already
    // overwritten, so a move left the save in both slots and reported an error.
    @Test
    fun `mover um save com metadata corrompida move o save e conserta o arquivo`() {
        save(1, byteArrayOf(7, 7), "Source")
        save(2, byteArrayOf(2), "Target")
        corruptMetadata(1)

        assertTrue(manager.moveSlot(1, 2))

        assertTrue(manager.getSlot(1).isEmpty)
        val target = manager.getSlot(2)
        assertArrayEquals(byteArrayOf(7, 7), target.stateFile?.readBytes())
        assertEquals("Slot 2", target.name)
        assertEquals(2, metadataJson(2).getInt("slotNumber"))
    }

    @Test
    fun `copiar um save sem metadata copia so os arquivos que existem`() {
        save(5, byteArrayOf(5), "No metadata")
        File(slotDir(5), "metadata.json").delete()

        assertTrue(manager.copySlot(5, 6))

        assertFalse(File(slotDir(6), "metadata.json").exists())
        assertEquals("Slot 6", manager.getSlot(6).name)
    }

    @Test
    fun `uma copia que falha nao deixa meio save no destino`() {
        save(1, byteArrayOf(1), "Source")
        save(2, byteArrayOf(2), "Target")
        File(slotDir(1), "state.bin").setReadable(false)

        assertFalse(manager.copySlot(1, 2))

        assertFalse(slotDir(2).exists())
        assertTrue(manager.getSlot(2).isEmpty)
    }

    // Regression: deleteSlot ignored deleteRecursively's result and always reported success.
    @Test
    fun `apagar um slot cujos arquivos nao podem ser apagados retorna false`() {
        save(3, byteArrayOf(3), "Stuck")
        slotDir(3).setWritable(false)

        assertFalse(manager.deleteSlot(3))

        assertFalse(manager.getSlot(3).isEmpty)
    }

    @Test
    fun `apagar um slot vazio retorna true`() {
        assertTrue(manager.deleteSlot(8))
    }

    @Test
    fun `mover quando a origem nao pode ser apagada retorna false e o save fica nos dois slots`() {
        save(6, byteArrayOf(6), "Stuck")
        slotDir(6).setWritable(false)

        assertFalse(manager.moveSlot(6, 7))

        assertArrayEquals(byteArrayOf(6), manager.getSlot(6).stateFile?.readBytes())
        assertArrayEquals(byteArrayOf(6), manager.getSlot(7).stateFile?.readBytes())
    }
}
