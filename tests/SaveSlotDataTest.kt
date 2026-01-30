package com.vinaooo.revenger.tests

import com.vinaooo.revenger.models.SaveSlotData
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

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
        val slot =
                SaveSlotData(
                        slotNumber = 1,
                        name = "Boss Fight",
                        timestamp = Instant.now(),
                        romName = "Zelda",
                        stateFile = null,
                        screenshotFile = null,
                        playTime = 0,
                        description = "",
                        isEmpty = false
                )
        assertEquals("Boss Fight", slot.getDisplayName())
    }
}
