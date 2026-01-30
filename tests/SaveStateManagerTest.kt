package com.vinaooo.revenger.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vinaooo.revenger.managers.SaveStateManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for SaveStateManager class.
 *
 * These tests require Android context and run on device/emulator.
 * They verify:
 * - Slot creation and retrieval
 * - Save/Load operations
 * - Delete operations
 * - Copy/Move operations
 * - Rename operations
 * - Legacy migration
 */
@RunWith(AndroidJUnit4::class)
class SaveStateManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Clear singleton to ensure fresh instance
        SaveStateManager.clearInstance()
        
        // Clean up any existing saves before test
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
        
        // Also clean legacy file
        File(context.filesDir, "state").delete()
        
        manager = SaveStateManager.getInstance(context)
    }

    @After
    fun cleanup() {
        // Clean up test saves
        savesDir.deleteRecursively()
        File(context.filesDir, "state").delete()
        SaveStateManager.clearInstance()
    }

    @Test
    fun getAllSlots_returns9Slots() {
        val slots = manager.getAllSlots()
        assertEquals(9, slots.size)
    }

    @Test
    fun allEmptySlots_haveCorrectSlotNumbers() {
        val slots = manager.getAllSlots()
        slots.forEachIndexed { index, slot ->
            assertEquals(index + 1, slot.slotNumber)
            assertTrue(slot.isEmpty)
        }
    }

    @Test
    fun getSlot_emptySlot_hasIsEmptyTrue() {
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 5", slot.name)
    }

    @Test
    fun saveToSlot_createsStateFile() {
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
    fun loadFromSlot_returnsSavedData() {
        val testData = "test state data for load".toByteArray()
        manager.saveToSlot(2, testData, null)
        
        val loadedData = manager.loadFromSlot(2)
        
        assertNotNull(loadedData)
        assertArrayEquals(testData, loadedData)
    }

    @Test
    fun loadFromSlot_emptySlot_returnsNull() {
        val loadedData = manager.loadFromSlot(7)
        assertNull(loadedData)
    }

    @Test
    fun deleteSlot_removesSave() {
        val testData = "delete test".toByteArray()
        manager.saveToSlot(3, testData, null)
        
        // Verify save exists
        assertFalse(manager.getSlot(3).isEmpty)
        
        val deleteResult = manager.deleteSlot(3)
        assertTrue(deleteResult)
        
        val slot = manager.getSlot(3)
        assertTrue(slot.isEmpty)
    }

    @Test
    fun copySlot_duplicatesSave() {
        val testData = "copy test data".toByteArray()
        manager.saveToSlot(1, testData, null, "Original")
        
        val copyResult = manager.copySlot(1, 2)
        assertTrue(copyResult)
        
        // Both slots should have saves
        assertFalse(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(2).isEmpty)
        
        // Data should be equal
        assertArrayEquals(manager.loadFromSlot(1), manager.loadFromSlot(2))
        
        // Original name should be preserved
        assertEquals("Original", manager.getSlot(1).name)
    }

    @Test
    fun copySlot_emptySource_returnsFalse() {
        val copyResult = manager.copySlot(5, 6)
        assertFalse(copyResult)
    }

    @Test
    fun copySlot_updatesSlotNumberInMetadata() {
        val testData = "metadata test".toByteArray()
        manager.saveToSlot(1, testData, null)
        manager.copySlot(1, 3)
        
        val copiedSlot = manager.getSlot(3)
        assertEquals(3, copiedSlot.slotNumber)
    }

    @Test
    fun moveSlot_transfersSave() {
        val testData = "move test data".toByteArray()
        manager.saveToSlot(1, testData, null, "ToMove")
        
        val moveResult = manager.moveSlot(1, 3)
        assertTrue(moveResult)
        
        // Source should be empty
        assertTrue(manager.getSlot(1).isEmpty)
        
        // Target should have save
        assertFalse(manager.getSlot(3).isEmpty)
        
        // Data should be preserved
        assertArrayEquals(testData, manager.loadFromSlot(3))
    }

    @Test
    fun renameSlot_updatesMetadata() {
        val testData = "rename test".toByteArray()
        manager.saveToSlot(4, testData, null, "OldName")
        
        val renameResult = manager.renameSlot(4, "NewName")
        assertTrue(renameResult)
        
        val slot = manager.getSlot(4)
        assertEquals("NewName", slot.name)
    }

    @Test
    fun renameSlot_emptySlot_returnsFalse() {
        val renameResult = manager.renameSlot(9, "SomeName")
        assertFalse(renameResult)
    }

    @Test
    fun invalidSlotNumber_throwsException_zero() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSlot(0)
        }
    }

    @Test
    fun invalidSlotNumber_throwsException_ten() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSlot(10)
        }
    }

    @Test
    fun invalidSlotNumber_throwsException_negative() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSlot(-1)
        }
    }

    @Test
    fun copySlot_sameSourceAndTarget_throwsException() {
        manager.saveToSlot(1, "data".toByteArray(), null)
        assertThrows(IllegalArgumentException::class.java) {
            manager.copySlot(1, 1)
        }
    }

    @Test
    fun hasAnySave_noSaves_returnsFalse() {
        assertFalse(manager.hasAnySave())
    }

    @Test
    fun hasAnySave_withSave_returnsTrue() {
        manager.saveToSlot(5, "data".toByteArray(), null)
        assertTrue(manager.hasAnySave())
    }

    @Test
    fun getFirstEmptySlot_allEmpty_returns1() {
        assertEquals(1, manager.getFirstEmptySlot())
    }

    @Test
    fun getFirstEmptySlot_firstOccupied_returns2() {
        manager.saveToSlot(1, "data".toByteArray(), null)
        assertEquals(2, manager.getFirstEmptySlot())
    }

    @Test
    fun getFirstEmptySlot_allOccupied_returnsNull() {
        for (i in 1..9) {
            manager.saveToSlot(i, "data$i".toByteArray(), null)
        }
        assertNull(manager.getFirstEmptySlot())
    }

    @Test
    fun getOccupiedSlotCount_noSaves_returns0() {
        assertEquals(0, manager.getOccupiedSlotCount())
    }

    @Test
    fun getOccupiedSlotCount_someSaves_returnsCorrectCount() {
        manager.saveToSlot(1, "data".toByteArray(), null)
        manager.saveToSlot(3, "data".toByteArray(), null)
        manager.saveToSlot(7, "data".toByteArray(), null)
        assertEquals(3, manager.getOccupiedSlotCount())
    }

    @Test
    fun saveToSlot_withRomName_preservesRomName() {
        manager.saveToSlot(1, "data".toByteArray(), null, "My Save", "Super Mario World")
        
        val slot = manager.getSlot(1)
        assertEquals("Super Mario World", slot.romName)
    }

    @Test
    fun saveToSlot_defaultName_usesSlotNumber() {
        manager.saveToSlot(4, "data".toByteArray(), null)
        
        val slot = manager.getSlot(4)
        assertEquals("Slot 4", slot.name)
    }

    @Test
    fun saveToSlot_overwritesExistingSlot() {
        manager.saveToSlot(1, "old data".toByteArray(), null, "Old Save")
        manager.saveToSlot(1, "new data".toByteArray(), null, "New Save")
        
        val slot = manager.getSlot(1)
        assertEquals("New Save", slot.name)
        assertArrayEquals("new data".toByteArray(), manager.loadFromSlot(1))
    }

    @Test
    fun copySlot_overwritesExistingTarget() {
        manager.saveToSlot(1, "source data".toByteArray(), null, "Source")
        manager.saveToSlot(2, "target data".toByteArray(), null, "Target")
        
        manager.copySlot(1, 2)
        
        assertArrayEquals("source data".toByteArray(), manager.loadFromSlot(2))
    }

    @Test
    fun legacyMigration_migratesLegacyFile() {
        // Clean singleton and saves directory
        SaveStateManager.clearInstance()
        savesDir.deleteRecursively()
        
        // Create legacy save file BEFORE initializing manager
        val legacyFile = File(context.filesDir, "state")
        legacyFile.writeBytes("legacy save data".toByteArray())
        
        // Initialize manager - should trigger migration
        val freshManager = SaveStateManager.getInstance(context)
        
        // Check if migrated to slot 1
        val slot1 = freshManager.getSlot(1)
        assertFalse("Slot 1 should not be empty after migration", slot1.isEmpty)
        assertEquals("Slot 1 (Legacy)", slot1.name)
        
        // Legacy file should be deleted
        assertFalse("Legacy file should be deleted after migration", legacyFile.exists())
        
        // Data should be preserved
        assertArrayEquals("legacy save data".toByteArray(), freshManager.loadFromSlot(1))
    }

    @Test
    fun legacyMigration_doesNotOverwriteSlot1() {
        // First create a save in slot 1
        manager.saveToSlot(1, "existing data".toByteArray(), null, "Existing Save")
        
        // Now create legacy file
        val legacyFile = File(context.filesDir, "state")
        legacyFile.writeBytes("legacy data".toByteArray())
        
        // Reinitialize manager
        SaveStateManager.clearInstance()
        val freshManager = SaveStateManager.getInstance(context)
        
        // Slot 1 should still have original data
        val slot1 = freshManager.getSlot(1)
        assertEquals("Existing Save", slot1.name)
        assertArrayEquals("existing data".toByteArray(), freshManager.loadFromSlot(1))
    }

    @Test
    fun singleton_returnsSameInstance() {
        val instance1 = SaveStateManager.getInstance(context)
        val instance2 = SaveStateManager.getInstance(context)
        assertSame(instance1, instance2)
    }

    @Test
    fun getSavesDirectory_returnsCorrectPath() {
        val savesDir = manager.getSavesDirectory()
        assertTrue(savesDir.absolutePath.endsWith("/saves"))
    }
}
