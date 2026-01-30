package com.vinaooo.revenger.tests

import com.vinaooo.revenger.models.SaveSlotData
import java.io.File
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SaveSlotData class.
 *
 * Tests verify:
 * - Empty slot creation
 * - Display name formatting
 * - Timestamp formatting
 * - Play time formatting
 * - Screenshot availability checking
 */
class SaveSlotDataTest {

    @Test
    fun `empty slot should have isEmpty true`() {
        val slot = SaveSlotData.empty(1)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 1", slot.name)
        assertNull(slot.timestamp)
        assertNull(slot.stateFile)
        assertNull(slot.screenshotFile)
    }

    @Test
    fun `getDisplayName returns Empty for empty slots`() {
        val slot = SaveSlotData.empty(5)
        assertEquals("Empty", slot.getDisplayName())
    }

    @Test
    fun `getDisplayName returns name for occupied slots`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Boss Fight",
                        timestamp = Instant.now(),
                        romName = "Zelda",
                        stateFile = null,
                        screenshotFile = null,
                        isEmpty = false
                )
        assertEquals("Boss Fight", slot.getDisplayName())
    }

    @Test
    fun `empty slots should have correct slot numbers 1 to 9`() {
        for (i in 1..9) {
            val slot = SaveSlotData.empty(i)
            assertEquals(i, slot.slotNumber)
            assertEquals("Slot $i", slot.name)
        }
    }

    @Test
    fun `getFormattedTimestamp returns empty string for null timestamp`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedTimestamp())
    }

    @Test
    fun `getFormattedTimestamp returns formatted date for valid timestamp`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Test",
                        timestamp = Instant.parse("2026-01-30T14:30:00Z"),
                        romName = "TestROM",
                        stateFile = null,
                        screenshotFile = null,
                        isEmpty = false
                )
        // The formatted timestamp will depend on the system timezone
        val formatted = slot.getFormattedTimestamp()
        assertTrue("Timestamp should not be empty", formatted.isNotEmpty())
        assertTrue("Timestamp should contain date separators", formatted.contains("/"))
    }

    @Test
    fun `getFormattedPlayTime returns empty string for zero play time`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats seconds correctly`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Test",
                        timestamp = Instant.now(),
                        romName = "TestROM",
                        playTime = 45, // 45 seconds
                        stateFile = null,
                        screenshotFile = null,
                        isEmpty = false
                )
        assertEquals("00:00:45", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats minutes correctly`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Test",
                        timestamp = Instant.now(),
                        romName = "TestROM",
                        playTime = 125, // 2 min 5 sec
                        stateFile = null,
                        screenshotFile = null,
                        isEmpty = false
                )
        assertEquals("00:02:05", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime formats hours correctly`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Test",
                        timestamp = Instant.now(),
                        romName = "TestROM",
                        playTime = 3665, // 1 hour 1 min 5 sec
                        stateFile = null,
                        screenshotFile = null,
                        isEmpty = false
                )
        assertEquals("01:01:05", slot.getFormattedPlayTime())
    }

    @Test
    fun `hasScreenshot returns false for null screenshotFile`() {
        val slot = SaveSlotData.empty(1)
        assertFalse(slot.hasScreenshot())
    }

    @Test
    fun `hasScreenshot returns false for non-existent file`() {
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Test",
                        timestamp = Instant.now(),
                        romName = "TestROM",
                        stateFile = null,
                        screenshotFile = File("/non/existent/path/screenshot.webp"),
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
    fun `occupied slot preserves all properties`() {
        val now = Instant.now()
        val slot =
                SaveSlotData(
                        slotNumber = 3,
                        name = "Final Boss",
                        timestamp = now,
                        romName = "Super Mario World",
                        playTime = 7200,
                        description = "Before final battle",
                        stateFile = File("/path/to/state.bin"),
                        screenshotFile = File("/path/to/screenshot.webp"),
                        isEmpty = false
                )

        assertEquals(3, slot.slotNumber)
        assertEquals("Final Boss", slot.name)
        assertEquals(now, slot.timestamp)
        assertEquals("Super Mario World", slot.romName)
        assertEquals(7200, slot.playTime)
        assertEquals("Before final battle", slot.description)
        assertFalse(slot.isEmpty)
    }
}
