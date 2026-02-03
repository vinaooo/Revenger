package com.vinaooo.revenger.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SaveStateManager. Uses Robolectric to provide Android Context for file operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SaveStateManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File
    private lateinit var filesDir: File

    @Before
    fun setup() {
        // Clear singleton before each test
        SaveStateManager.clearInstance()

        context = ApplicationProvider.getApplicationContext()
        filesDir = context.filesDir
        savesDir = File(filesDir, "saves")

        // Clean up any existing saves
        savesDir.deleteRecursively()

        // Create fresh manager instance
        manager = SaveStateManager.getInstance(context)
    }

    @After
    fun cleanup() {
        // Clean up test saves
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

    // ========== BASIC OPERATIONS ==========

    @Test
    fun `getAllSlots returns 9 slots`() {
        val slots = manager.getAllSlots()
        assertEquals(9, slots.size)
    }

    @Test
    fun `getAllSlots returns slots numbered 1 to 9`() {
        val slots = manager.getAllSlots()
        slots.forEachIndexed { index, slot -> assertEquals(index + 1, slot.slotNumber) }
    }

    @Test
    fun `empty slot has isEmpty true`() {
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 5", slot.name)
    }

    @Test
    fun `getSlot with invalid number throws exception`() {
        assertThrows(IllegalArgumentException::class.java) { manager.getSlot(0) }
        assertThrows(IllegalArgumentException::class.java) { manager.getSlot(10) }
    }

    // ========== SAVE OPERATIONS ==========

    @Test
    fun `saveToSlot creates state file`() {
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
    fun `saveToSlot with default name uses Slot X format`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(3, testData, null)

        val slot = manager.getSlot(3)
        assertEquals("Slot 3", slot.name)
    }

    @Test
    fun `saveToSlot creates timestamp`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(1, testData, null)

        val slot = manager.getSlot(1)
        assertNotNull(slot.timestamp)
    }

    @Test
    fun `saveToSlot stores romName`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(1, testData, null, "Save", "Zelda")

        val slot = manager.getSlot(1)
        assertEquals("Zelda", slot.romName)
    }

    @Test
    fun `saveToSlot with invalid slot throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.saveToSlot(0, "test".toByteArray(), null)
        }
    }

    // ========== LOAD OPERATIONS ==========

    @Test
    fun `loadFromSlot returns saved data`() {
        val testData = "test state data for loading".toByteArray()
        manager.saveToSlot(2, testData, null)

        val loadedData = manager.loadFromSlot(2)

        assertNotNull(loadedData)
        assertArrayEquals(testData, loadedData)
    }

    @Test
    fun `loadFromSlot returns null for empty slot`() {
        val loadedData = manager.loadFromSlot(5)
        assertNull(loadedData)
    }

    @Test
    fun `loadFromSlot with invalid slot throws exception`() {
        assertThrows(IllegalArgumentException::class.java) { manager.loadFromSlot(10) }
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    fun `deleteSlot removes save`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(3, testData, null)
        assertFalse(manager.getSlot(3).isEmpty)

        val deleteResult = manager.deleteSlot(3)

        assertTrue(deleteResult)
        assertTrue(manager.getSlot(3).isEmpty)
    }

    @Test
    fun `deleteSlot on empty slot returns true`() {
        assertTrue(manager.getSlot(5).isEmpty)
        val result = manager.deleteSlot(5)
        assertTrue(result)
    }

    // ========== COPY OPERATIONS ==========

    @Test
    fun `copySlot duplicates save`() {
        val testData = "copy test data".toByteArray()
        manager.saveToSlot(1, testData, null, "Original")

        val copyResult = manager.copySlot(1, 2)

        assertTrue(copyResult)
        assertFalse(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(2).isEmpty)
        assertArrayEquals(manager.loadFromSlot(1), manager.loadFromSlot(2))
    }

    @Test
    fun `copySlot updates target slot number in metadata`() {
        manager.saveToSlot(1, "test".toByteArray(), null)
        manager.copySlot(1, 5)

        val slot5 = manager.getSlot(5)
        assertEquals(5, slot5.slotNumber)
    }

    @Test
    fun `copySlot from empty slot returns false`() {
        val result = manager.copySlot(5, 6)
        assertFalse(result)
    }

    @Test
    fun `copySlot to same slot throws exception`() {
        manager.saveToSlot(1, "test".toByteArray(), null)
        assertThrows(IllegalArgumentException::class.java) { manager.copySlot(1, 1) }
    }

    @Test
    fun `copySlot overwrites existing target`() {
        manager.saveToSlot(1, "original".toByteArray(), null, "Source")
        manager.saveToSlot(2, "to be overwritten".toByteArray(), null, "Target")

        manager.copySlot(1, 2)

        assertArrayEquals("original".toByteArray(), manager.loadFromSlot(2))
    }

    // ========== MOVE OPERATIONS ==========

    @Test
    fun `moveSlot transfers save`() {
        val testData = "move test data".toByteArray()
        manager.saveToSlot(1, testData, null, "ToMove")

        val moveResult = manager.moveSlot(1, 3)

        assertTrue(moveResult)
        assertTrue(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(3).isEmpty)
        assertArrayEquals(testData, manager.loadFromSlot(3))
    }

    @Test
    fun `moveSlot from empty slot returns false`() {
        val result = manager.moveSlot(5, 6)
        assertFalse(result)
    }

    // ========== RENAME OPERATIONS ==========

    @Test
    fun `renameSlot updates metadata`() {
        val testData = "rename test".toByteArray()
        manager.saveToSlot(4, testData, null, "OldName")

        val renameResult = manager.renameSlot(4, "NewName")

        assertTrue(renameResult)
        assertEquals("NewName", manager.getSlot(4).name)
    }

    @Test
    fun `renameSlot on empty slot returns false`() {
        val result = manager.renameSlot(5, "NewName")
        assertFalse(result)
    }

    // ========== UTILITY METHODS ==========

    @Test
    fun `hasAnySave returns false when all empty`() {
        assertFalse(manager.hasAnySave())
    }

    @Test
    fun `hasAnySave returns true when one slot occupied`() {
        manager.saveToSlot(5, "test".toByteArray(), null)
        assertTrue(manager.hasAnySave())
    }

    @Test
    fun `getFirstEmptySlot returns 1 when all empty`() {
        assertEquals(1, manager.getFirstEmptySlot())
    }

    @Test
    fun `getFirstEmptySlot returns next empty slot`() {
        manager.saveToSlot(1, "test".toByteArray(), null)
        manager.saveToSlot(2, "test".toByteArray(), null)
        assertEquals(3, manager.getFirstEmptySlot())
    }

    @Test
    fun `getFirstEmptySlot returns null when all full`() {
        for (i in 1..9) {
            manager.saveToSlot(i, "test".toByteArray(), null)
        }
        assertNull(manager.getFirstEmptySlot())
    }

    @Test
    fun `getOccupiedSlotCount returns correct count`() {
        assertEquals(0, manager.getOccupiedSlotCount())

        manager.saveToSlot(1, "test".toByteArray(), null)
        assertEquals(1, manager.getOccupiedSlotCount())

        manager.saveToSlot(5, "test".toByteArray(), null)
        assertEquals(2, manager.getOccupiedSlotCount())
    }

    // ========== LEGACY MIGRATION ==========

    @Test
    fun `legacy save migrated to slot 1 on init`() {
        // Clean up and create legacy file before manager init
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()

        val legacyFile = File(filesDir, "state")
        legacyFile.writeBytes("legacy save data".toByteArray())
        assertTrue(legacyFile.exists())

        // Create new manager instance (triggers migration)
        val newManager = SaveStateManager.getInstance(context)

        // Verify migration
        val slot1 = newManager.getSlot(1)
        assertFalse(slot1.isEmpty)
        assertEquals("Slot 1 (Legacy)", slot1.name)
        assertArrayEquals("legacy save data".toByteArray(), newManager.loadFromSlot(1))

        // Legacy file should be deleted
        assertFalse(legacyFile.exists())
    }

    @Test
    fun `legacy migration does not overwrite existing slot 1`() {
        // First save to slot 1
        manager.saveToSlot(1, "existing save".toByteArray(), null, "Existing")
        SaveStateManager.clearInstance()

        // Create legacy file
        val legacyFile = File(filesDir, "state")
        legacyFile.writeBytes("legacy data".toByteArray())

        // Re-init manager
        val newManager = SaveStateManager.getInstance(context)

        // Slot 1 should keep existing save, not legacy
        assertEquals("Existing", newManager.getSlot(1).name)
        assertArrayEquals("existing save".toByteArray(), newManager.loadFromSlot(1))
    }

    // ========== SINGLETON ==========

    @Test
    fun `getInstance returns same instance`() {
        val instance1 = SaveStateManager.getInstance(context)
        val instance2 = SaveStateManager.getInstance(context)
        assertSame(instance1, instance2)
    }
}
