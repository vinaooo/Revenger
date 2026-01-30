package com.vinaooo.revenger.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SaveStateManager
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SaveStateManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = SaveStateManager.getInstance(context)
        savesDir = File(context.filesDir, "saves")
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
    }

    @Test
    fun `getAllSlots returns 9 slots`() {
        val slots = manager.getAllSlots()
        assertEquals(9, slots.size)
    }

    @Test
    fun `empty slot has isEmpty true`() {
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
    }

    @Test
    fun `saveToSlot creates state file`() {
        val testData = "test state data".toByteArray()
        val result = manager.saveToSlot(1, testData, null, "Test Save")

        assertTrue(result)

        val slot = manager.getSlot(1)
        assertFalse(slot.isEmpty)
        assertEquals("Test Save", slot.name)
    }

    @Test
    fun `loadFromSlot returns saved data`() {
        val testData = "test state data".toByteArray()
        manager.saveToSlot(2, testData, null)

        val loadedData = manager.loadFromSlot(2)

        assertNotNull(loadedData)
        assertArrayEquals(testData, loadedData)
    }

    @Test
    fun `deleteSlot removes save`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(3, testData, null)

        val deleteResult = manager.deleteSlot(3)
        assertTrue(deleteResult)

        val slot = manager.getSlot(3)
        assertTrue(slot.isEmpty)
    }

    @Test
    fun `copySlot duplicates save`() {
        val testData = "copy test".toByteArray()
        manager.saveToSlot(1, testData, null, "Original")

        val copyResult = manager.copySlot(1, 2)
        assertTrue(copyResult)

        assertFalse(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(2).isEmpty)

        assertArrayEquals(manager.loadFromSlot(1), manager.loadFromSlot(2))
    }

    @Test
    fun `moveSlot transfers save`() {
        val testData = "move test".toByteArray()
        manager.saveToSlot(1, testData, null, "ToMove")

        val moveResult = manager.moveSlot(1, 3)
        assertTrue(moveResult)

        assertTrue(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(3).isEmpty)
    }

    @Test
    fun `renameSlot updates metadata`() {
        val testData = "rename test".toByteArray()
        manager.saveToSlot(4, testData, null, "OldName")

        val renameResult = manager.renameSlot(4, "NewName")
        assertTrue(renameResult)

        val slot = manager.getSlot(4)
        assertEquals("NewName", slot.name)
    }

    @Test
    fun `invalid slot number throws exception`() {
        try {
            manager.getSlot(0)
            fail("Expected IllegalArgumentException for slot 0")
        } catch (e: IllegalArgumentException) {}

        try {
            manager.getSlot(10)
            fail("Expected IllegalArgumentException for slot 10")
        } catch (e: IllegalArgumentException) {}
    }
}
