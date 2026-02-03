package com.vinaooo.revenger.tests

import com.vinaooo.revenger.models.SaveSlotData
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SaveSlotData
 */
class SaveSlotDataTest {

    @Test
    fun `empty slot should have isEmpty true`() {
        val slot = SaveSlotData.empty(1)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 1", slot.name)
        assertNull(slot.timestamp)
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
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        assertEquals("Boss Fight", slot.getDisplayName())
    }

    @Test
    fun `getFormattedTimestamp returns empty string for null timestamp`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedTimestamp())
    }

    @Test
    fun `getFormattedTimestamp returns formatted date for non-null timestamp`() {
        val instant = Instant.parse("2026-01-30T14:32:00Z")
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Test",
            timestamp = instant,
            romName = "Zelda",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        val formatted = slot.getFormattedTimestamp()
        assertNotNull(formatted)
        assertTrue(formatted.contains("/"))
        assertTrue(formatted.contains(":"))
    }

    @Test
    fun `slot with all parameters set correctly`() {
        val now = Instant.now()
        val slot = SaveSlotData(
            slotNumber = 7,
            name = "Custom Save",
            timestamp = now,
            romName = "Super Metroid",
            playTime = 3600,
            description = "Boss defeated",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )

        assertEquals(7, slot.slotNumber)
        assertEquals("Custom Save", slot.name)
        assertEquals(now, slot.timestamp)
        assertEquals("Super Metroid", slot.romName)
        assertEquals(3600L, slot.playTime)
        assertEquals("Boss defeated", slot.description)
        assertFalse(slot.isEmpty)
    }

    @Test
    fun `empty slots for all 9 slots`() {
        for (i in 1..9) {
            val slot = SaveSlotData.empty(i)
            assertEquals(i, slot.slotNumber)
            assertEquals("Slot $i", slot.name)
            assertTrue(slot.isEmpty)
        }
    }
}
