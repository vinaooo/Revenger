package com.vinaooo.revenger.managers

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.models.SaveSlotPayload
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for SaveStateManager.
 * Uses Robolectric to provide Android Context for file operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveStateManager_test {

    private lateinit var context: Context
    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        
        context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
        
        manager = SaveStateManager.getInstance(context)
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

    // ========== BASIC OPERATIONS ==========

    @Test
    fun `getAllSlots retorna 9 slots`() {
        val slots = manager.getAllSlots()
        assertEquals(9, slots.size)
    }

    @Test
    fun `getAllSlots retorna slots numerados de 1 a 9`() {
        val slots = manager.getAllSlots()
        slots.forEachIndexed { index, slot -> 
            assertEquals(index + 1, slot.slotNumber)
        }
    }

    @Test
    fun `slot vazio tem isEmpty true`() {
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 5", slot.name)
    }

    @Test
    fun `getSlot com numero invalido lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) { 
            manager.getSlot(0) 
        }
        assertThrows(IllegalArgumentException::class.java) { 
            manager.getSlot(10) 
        }
    }

    @Test
    fun `getInstance retorna mesma instancia`() {
        val manager1 = SaveStateManager.getInstance(context)
        val manager2 = SaveStateManager.getInstance(context)
        assertSame(manager1, manager2)
    }

    // ========== SAVE OPERATIONS ==========

    @Test
    fun `saveToSlot cria arquivo de estado`() {
        val testData = "test state data".toByteArray()
        val result = manager.saveToSlot(1, SaveSlotPayload(testData, null, name = "Test Save"))

        assertTrue(result)

        val slot = manager.getSlot(1)
        assertFalse(slot.isEmpty)
        assertEquals("Test Save", slot.name)
        assertNotNull(slot.stateFile)
        assertTrue(slot.stateFile!!.exists())
    }

    @Test
    fun `saveToSlot com nome padrao usa formato Slot X`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(3, SaveSlotPayload(testData, null))

        val slot = manager.getSlot(3)
        assertEquals("Slot 3", slot.name)
    }

    @Test
    fun `saveToSlot sobrescreve slot existente`() {
        val data1 = "first save".toByteArray()
        val data2 = "second save".toByteArray()
        
        manager.saveToSlot(2, SaveSlotPayload(data1, null, name = "First"))
        manager.saveToSlot(2, SaveSlotPayload(data2, null, name = "Second"))
        
        val slot = manager.getSlot(2)
        assertEquals("Second", slot.name)
        assertEquals(data2.size.toLong(), slot.stateFile!!.length())
    }

    @Test
    fun `saveToSlot com screenshot salva imagem`() {
        val stateData = "state".toByteArray()
        val screenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
        
        manager.saveToSlot(4, SaveSlotPayload(stateData, screenshot, name = "With Screenshot"))
        
        val slot = manager.getSlot(4)
        assertNotNull(slot.screenshotFile)
        assertTrue(slot.screenshotFile!!.exists())
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    fun `deleteSlot remove arquivos e marca como vazio`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(5, SaveSlotPayload(testData, null, name = "To Delete"))
        
        assertTrue(manager.deleteSlot(5))
        
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
        assertNull(slot.stateFile)
    }

    @Test
    fun `deleteSlot remove screenshot tambem`() {
        val stateData = "state".toByteArray()
        val screenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
        
        manager.saveToSlot(7, SaveSlotPayload(stateData, screenshot, name = "With Image"))
        manager.deleteSlot(7)
        
        val slot = manager.getSlot(7)
        assertNull(slot.screenshotFile)
    }

    // ========== RENAME OPERATIONS ==========

    @Test
    fun `copySlot duplica dados para outro slot`() {
        val testData = "copy test".toByteArray()
        manager.saveToSlot(1, SaveSlotPayload(testData, null, name = "Original"))
        
        assertTrue(manager.copySlot(1, 2))
        
        val source = manager.getSlot(1)
        val dest = manager.getSlot(2)
        
        assertFalse(source.isEmpty)
        assertFalse(dest.isEmpty)
        assertEquals(source.name, dest.name)
    }

    @Test
    fun `moveSlot transfere dados e limpa origem`() {
        val testData = "move test".toByteArray()
        manager.saveToSlot(3, SaveSlotPayload(testData, null, name = "To Move"))
        
        assertTrue(manager.moveSlot(3, 4))
        
        val source = manager.getSlot(3)
        val dest = manager.getSlot(4)
        
        assertTrue(source.isEmpty)
        assertFalse(dest.isEmpty)
        assertEquals("To Move", dest.name)
    }

    @Test
    fun `copySlot de slot vazio retorna false`() {
        assertFalse(manager.copySlot(5, 6))
    }

    // ========== RENAME OPERATIONS ==========

    @Test
    fun `renameSlot altera nome do slot`() {
        val testData = "rename test".toByteArray()
        manager.saveToSlot(7, SaveSlotPayload(testData, null, name = "Old Name"))
        
        assertTrue(manager.renameSlot(7, "New Name"))
        
        val slot = manager.getSlot(7)
        assertEquals("New Name", slot.name)
    }

    @Test
    fun `renameSlot de slot vazio retorna false`() {
        assertFalse(manager.renameSlot(8, "Cannot Rename"))
    }

    // ========== SCREENSHOT UPDATE ==========

    @Test
    fun `updateScreenshot em slot existente sobrescreve a imagem e retorna true`() {
        val initialScreenshot = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        manager.saveToSlot(
                2,
                SaveSlotPayload("state".toByteArray(), initialScreenshot, name = "With Slot")
        )
        val initialLength = manager.getSlot(2).screenshotFile!!.length()

        val newScreenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
        val result = manager.updateScreenshot(2, newScreenshot)

        assertTrue(result)
        val slot = manager.getSlot(2)
        assertNotNull(slot.screenshotFile)
        assertTrue(slot.screenshotFile!!.exists())
        assertTrue(
                "screenshot file must reflect the new (larger) image, not the original",
                slot.screenshotFile!!.length() > initialLength
        )
    }

    @Test
    fun `updateScreenshot em slot vazio retorna false`() {
        val screenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)

        val result = manager.updateScreenshot(4, screenshot)

        assertFalse(result)
    }

    // ========== ERROR PATHS ==========

    // Regression test for the narrowed IOException catch in saveToSlot: forces a real I/O
    // failure (the slot directory can't be created because a plain file already occupies that
    // path) rather than a mock, so a mis-narrowed catch type would let the exception propagate
    // and fail this test instead of silently passing.
    @Test
    fun `saveToSlot com diretorio de slot bloqueado por arquivo retorna false`() {
        savesDir.mkdirs()
        val blockedSlotDir = File(savesDir, "slot_6")
        blockedSlotDir.createNewFile()

        val result = manager.saveToSlot(6, SaveSlotPayload("data".toByteArray(), null))

        assertFalse(result)
    }

    // Regression test for the narrowed JSONException catch in readMetadata: a state file exists
    // (so getSlot() doesn't short-circuit to empty) but metadata.json is not valid JSON.
    @Test
    fun `getSlot com metadata json corrompido nao lanca excecao e usa nome padrao`() {
        val slotDir = File(savesDir, "slot_1")
        slotDir.mkdirs()
        File(slotDir, "state.bin").writeBytes("state".toByteArray())
        File(slotDir, "metadata.json").writeText("{ not valid json ]")

        val slot = manager.getSlot(1)

        assertFalse(slot.isEmpty)
        assertEquals("Slot 1", slot.name)
    }

    // Regression test for the narrowed DateTimeParseException catch in parseTimestamp: valid
    // metadata JSON, but the timestamp field isn't a valid ISO-8601 instant.
    @Test
    fun `getSlot com timestamp invalido no metadata retorna slot sem timestamp`() {
        val slotDir = File(savesDir, "slot_1")
        slotDir.mkdirs()
        File(slotDir, "state.bin").writeBytes("state".toByteArray())
        File(slotDir, "metadata.json").writeText(
                org.json.JSONObject()
                        .put("name", "Broken Timestamp")
                        .put("timestamp", "not-a-real-timestamp")
                        .toString()
        )

        val slot = manager.getSlot(1)

        assertFalse(slot.isEmpty)
        assertEquals("Broken Timestamp", slot.name)
        assertNull(slot.timestamp)
    }

    // ========== CONCURRENCY ==========

    // Regression test for the PiP quick-save flow (a background Thread) racing a main-thread
    // save to the same slot. Each thread writes a state.bin filled entirely with its own byte
    // value; without @Synchronized, concurrent unsynchronized writeBytes() calls to the same
    // file can interleave, leaving a state.bin that mixes bytes from two different threads.
    // With @Synchronized, every write is fully serialized, so the slot's final content must
    // always be a single thread's complete, uniform array.
    @Test
    fun `saveToSlot concorrente no mesmo slot nunca produz um state-bin corrompido`() {
        val threadCount = 20
        val stateSize = 2000
        val ready = java.util.concurrent.CountDownLatch(threadCount)
        val go = java.util.concurrent.CountDownLatch(1)
        val done = java.util.concurrent.CountDownLatch(threadCount)

        val threads = (0 until threadCount).map { i ->
            Thread {
                ready.countDown()
                go.await()
                manager.saveToSlot(3, SaveSlotPayload(ByteArray(stateSize) { i.toByte() }, null))
                done.countDown()
            }
        }
        threads.forEach { it.start() }
        ready.await()
        go.countDown() // release all threads at once to maximize contention
        done.await()

        val result = manager.loadFromSlot(3)
        assertNotNull(result)
        assertEquals(stateSize, result!!.size)
        val distinctValues = result.toSet()
        assertEquals(
                "state.bin must contain exactly one thread's byte value, not a mix of several",
                1,
                distinctValues.size
        )
    }

    // Regression test for the shared lock between SaveStateManager and its delegated
    // SlotQueryStore: getSlot() is implemented by a separate collaborator instance than
    // saveToSlot(), so if they didn't synchronize on the same monitor, getSlot() could observe a
    // slot mid-write (e.g. state.bin already written but metadata.json not yet, so a non-empty
    // slot momentarily reports the pre-write default name instead of the one being saved).
    @Test
    fun `getSlot concorrente com saveToSlot nunca observa slot parcialmente escrito`() {
        val iterations = 200
        val payload = SaveSlotPayload("consistent state".toByteArray(), null, name = "Consistent")
        val done = java.util.concurrent.CountDownLatch(1)
        val readerFailure = java.util.concurrent.atomic.AtomicReference<String?>(null)

        val writer = Thread {
            repeat(iterations) { manager.saveToSlot(9, payload) }
            done.countDown()
        }
        val reader = Thread {
            while (done.count > 0) {
                val slot = manager.getSlot(9)
                if (!slot.isEmpty &&
                                (slot.stateFile?.exists() != true || slot.name != "Consistent")
                ) {
                    readerFailure.compareAndSet(
                            null,
                            "non-empty slot with stateFile.exists()=${slot.stateFile?.exists()} name=${slot.name}"
                    )
                }
            }
        }

        writer.start()
        reader.start()
        writer.join()
        reader.join()

        assertNull(
                "getSlot must never observe a torn/partial write: ${readerFailure.get()}",
                readerFailure.get()
        )
    }
}
