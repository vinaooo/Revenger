package com.vinaooo.revenger.models

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Unit tests for SaveSlotData data class.
 * Tests empty slot creation, display name logic, and timestamp formatting.
 */
class SaveSlotDataTest {

    @Test
    fun `empty slot should have isEmpty true`() {
        val slot = SaveSlotData.empty(1)
        
        assertTrue(slot.isEmpty)
        assertEquals(1, slot.slotNumber)
        assertEquals("Slot 1", slot.name)
        assertNull(slot.timestamp)
        assertNull(slot.stateFile)
        assertNull(slot.screenshotFile)
        assertEquals("", slot.romName)
        assertEquals(0L, slot.playTime)
    }

    @Test
    fun `empty creates slots with correct number`() {
        for (i in 1..9) {
            val slot = SaveSlotData.empty(i)
            assertEquals(i, slot.slotNumber)
            assertEquals("Slot $i", slot.name)
        }
    }

    @Test
    fun `getDisplayName returns Empty for empty slots`() {
        val slot = SaveSlotData.empty(5)
        assertEquals("Empty", slot.getDisplayName())
    }

    @Test
    fun `getDisplayName returns name for occupied slots`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Boss Fight",
            timestamp = Instant.now(),
            romName = "Zelda",
            playTime = 3600,
            description = "Before final boss",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        assertEquals("Boss Fight", slot.getDisplayName())
    }

    @Test
    fun `getFormattedTimestamp returns empty for null timestamp`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedTimestamp())
    }

    @Test
    fun `getFormattedTimestamp returns formatted date`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Test",
            timestamp = Instant.parse("2026-01-30T14:32:00Z"),
            romName = "Test ROM",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        // Note: Exact format depends on system timezone
        val formatted = slot.getFormattedTimestamp()
        assertTrue(formatted.isNotEmpty())
        assertTrue(formatted.contains("/"))
        assertTrue(formatted.contains(":"))
    }

    @Test
    fun `getFormattedPlayTime returns empty for zero`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats hours and minutes`() {
        val slot = createSlotWithPlayTime(5400) // 1h 30m
        assertEquals("1h 30m", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats hours only`() {
        val slot = createSlotWithPlayTime(7200) // 2h
        assertEquals("2h", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats minutes only`() {
        val slot = createSlotWithPlayTime(2700) // 45m
        assertEquals("45m", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime returns less than 1m for small values`() {
        val slot = createSlotWithPlayTime(30) // 30 seconds
        assertEquals("<1m", slot.getFormattedPlayTime())
    }

    @Test
    fun `hasScreenshot returns false when screenshotFile is null`() {
        val slot = SaveSlotData.empty(1)
        assertFalse(slot.hasScreenshot())
    }

    @Test
    fun `hasScreenshot returns false when file does not exist`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Test",
            timestamp = Instant.now(),
            romName = "Test",
            stateFile = null,
            screenshotFile = File("/nonexistent/path/screenshot.webp"),
            isEmpty = false
        )
        assertFalse(slot.hasScreenshot())
    }

    @Test
    fun `data class equality works correctly`() {
        val slot1 = SaveSlotData.empty(1)
        val slot2 = SaveSlotData.empty(1)
        val slot3 = SaveSlotData.empty(2)
        
        assertEquals(slot1, slot2)
        assertNotEquals(slot1, slot3)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = SaveSlotData.empty(1)
        val copied = original.copy(name = "New Name", isEmpty = false)
        
        assertEquals(1, copied.slotNumber)
        assertEquals("New Name", copied.name)
        assertFalse(copied.isEmpty)
    }

    // Helper function
    private fun createSlotWithPlayTime(seconds: Long): SaveSlotData {
        return SaveSlotData(
            slotNumber = 1,
            name = "Test",
            timestamp = Instant.now(),
            romName = "Test",
            playTime = seconds,
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
    }
}
