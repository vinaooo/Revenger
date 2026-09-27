package com.vinaooo.revenger.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Instant
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SaveStateManager.backfillMissingTimestamps] gives undated slots a save date so PiP Quick Save
 * can find the oldest save. The slots here are written the way older app versions (or damaged
 * files) leave them: no `metadata.json`, a corrupt one, one without a `timestamp`, one with an
 * unreadable date, and the single save migrated from the pre-slot format. After the backfill the
 * files must still use the keys and ISO-8601 date format every version reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveStateManagerTimestamps_test {

    private val fileDate = Instant.parse("2026-01-10T08:30:00Z")

    private lateinit var context: Context
    private lateinit var savesDir: File

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
        File(context.filesDir, "state").delete()
    }

    @After
    fun tearDown() {
        savesDir.deleteRecursively()
        File(context.filesDir, "state").delete()
        SaveStateManager.clearInstance()
    }

    private fun manager() = SaveStateManager.getInstance(context)

    private fun slotDir(number: Int) = File(savesDir, "slot_$number").apply { mkdirs() }

    private fun metadataFile(number: Int) = File(slotDir(number), "metadata.json")

    // A slot's state file, dated like a save written at [fileDate].
    private fun writeState(number: Int) {
        File(slotDir(number), "state.bin").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(fileDate.toEpochMilli())
        }
    }

    private fun readJson(number: Int) = JSONObject(metadataFile(number).readText())

    @Test
    fun `slot sem metadata ganha um arquivo com o nome padrao e a data do state`() {
        writeState(3)

        assertEquals(1, manager().backfillMissingTimestamps())

        val json = readJson(3)
        assertEquals("Slot 3", json.getString("name"))
        assertEquals(3, json.getInt("slotNumber"))
        assertEquals(fileDate, Instant.parse(json.getString("timestamp")))
        assertEquals(fileDate, manager().getSlot(3).timestamp)
    }

    @Test
    fun `metadata sem timestamp ganha a data e mantem os outros campos`() {
        writeState(2)
        metadataFile(2).writeText(
                JSONObject()
                        .put("name", "Before the boss")
                        .put("slotNumber", 2)
                        .put("romName", "Some Game")
                        .put("playTime", 42)
                        .toString()
        )

        manager().backfillMissingTimestamps()

        val slot = manager().getSlot(2)
        assertEquals("Before the boss", slot.name)
        assertEquals("Some Game", slot.romName)
        assertEquals(42L, slot.playTime)
        assertEquals(fileDate, slot.timestamp)
    }

    @Test
    fun `metadata corrompida e trocada por uma valida com o nome que o menu ja mostrava`() {
        writeState(4)
        metadataFile(4).writeText("{ not json")
        assertEquals("Slot 4", manager().getSlot(4).name)

        manager().backfillMissingTimestamps()

        val slot = manager().getSlot(4)
        assertEquals("Slot 4", slot.name)
        assertEquals(fileDate, slot.timestamp)
    }

    @Test
    fun `data ilegivel e trocada pela data do state`() {
        writeState(5)
        metadataFile(5).writeText(JSONObject().put("name", "Slot 5").put("timestamp", "yesterday").toString())

        manager().backfillMissingTimestamps()

        assertEquals(fileDate, manager().getSlot(5).timestamp)
    }

    @Test
    fun `slot que ja tem data nao e tocado`() {
        writeState(6)
        val original = JSONObject()
                .put("name", "Slot 6")
                .put("timestamp", "2026-03-01T00:00:00Z")
                .put("slotNumber", 6)
                .toString()
        metadataFile(6).writeText(original)

        assertEquals(0, manager().backfillMissingTimestamps())

        assertEquals(original, metadataFile(6).readText())
    }

    @Test
    fun `slots vazios nao ganham arquivos`() {
        assertEquals(0, manager().backfillMissingTimestamps())

        assertFalse(File(savesDir, "slot_1/metadata.json").exists())
        assertFalse(File(savesDir, "slot_9/metadata.json").exists())
    }

    @Test
    fun `o save unico migrado do formato antigo ja vem com data e nao e tocado`() {
        File(context.filesDir, "state").writeBytes(byteArrayOf(9, 9))

        val migrated = manager().getSlot(1)

        assertFalse(migrated.isEmpty)
        assertNotNull(migrated.timestamp)
        assertEquals(0, manager().backfillMissingTimestamps())
    }

    @Test
    fun `depois do backfill uma nova instancia le as mesmas datas`() {
        writeState(7)
        writeState(8)
        manager().backfillMissingTimestamps()

        SaveStateManager.clearInstance()

        val slots = manager().getAllSlots()
        assertEquals(fileDate, slots[6].timestamp)
        assertEquals(fileDate, slots[7].timestamp)
        assertEquals(0, manager().backfillMissingTimestamps())
    }
}
