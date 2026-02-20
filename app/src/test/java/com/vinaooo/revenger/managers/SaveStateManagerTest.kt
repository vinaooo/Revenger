package com.vinaooo.revenger.managers

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
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
class SaveStateManagerTest {

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
        val result = manager.saveToSlot(1, testData, null, "Test Save")

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
        manager.saveToSlot(3, testData, null)

        val slot = manager.getSlot(3)
        assertEquals("Slot 3", slot.name)
    }

    @Test
    fun `saveToSlot sobrescreve slot existente`() {
        val data1 = "first save".toByteArray()
        val data2 = "second save".toByteArray()
        
        manager.saveToSlot(2, data1, null, "First")
        manager.saveToSlot(2, data2, null, "Second")
        
        val slot = manager.getSlot(2)
        assertEquals("Second", slot.name)
        assertEquals(data2.size.toLong(), slot.stateFile!!.length())
    }

    @Test
    fun `saveToSlot com screenshot salva imagem`() {
        val stateData = "state".toByteArray()
        val screenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
        
        manager.saveToSlot(4, stateData, screenshot, "With Screenshot")
        
        val slot = manager.getSlot(4)
        assertNotNull(slot.screenshotFile)
        assertTrue(slot.screenshotFile!!.exists())
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    fun `deleteSlot remove arquivos e marca como vazio`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(5, testData, null, "To Delete")
        
        assertTrue(manager.deleteSlot(5))
        
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
        assertNull(slot.stateFile)
    }

    @Test
    fun `deleteSlot remove screenshot tambem`() {
        val stateData = "state".toByteArray()
        val screenshot = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
        
        manager.saveToSlot(7, stateData, screenshot, "With Image")
        manager.deleteSlot(7)
        
        val slot = manager.getSlot(7)
        assertNull(slot.screenshotFile)
    }

    // ========== RENAME OPERATIONS ==========

    @Test
    fun `copySlot duplica dados para outro slot`() {
        val testData = "copy test".toByteArray()
        manager.saveToSlot(1, testData, null, "Original")
        
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
        manager.saveToSlot(3, testData, null, "To Move")
        
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
        manager.saveToSlot(7, testData, null, "Old Name")
        
        assertTrue(manager.renameSlot(7, "New Name"))
        
        val slot = manager.getSlot(7)
        assertEquals("New Name", slot.name)
    }

    @Test
    fun `renameSlot de slot vazio retorna false`() {
        assertFalse(manager.renameSlot(8, "Cannot Rename"))
    }
}
