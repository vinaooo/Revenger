package com.vinaooo.revenger.models

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SaveSlotData.
 * Tests empty slot creation, display names, and timestamp formatting.
 */
class SaveSlotData_test {

    @Test
    fun `empty slot deve ter isEmpty true`() {
        val slot = SaveSlotData.empty(1)
        
        assertTrue(slot.isEmpty)
        assertEquals(1, slot.slotNumber)
        assertEquals("Slot 1", slot.name)
        assertNull(slot.timestamp)
        assertNull(slot.stateFile)
        assertNull(slot.screenshotFile)
    }

    @Test
    fun `empty cria slots com numero correto`() {
        for (i in 1..9) {
            val slot = SaveSlotData.empty(i)
            assertEquals(i, slot.slotNumber)
            assertEquals("Slot $i", slot.name)
            assertTrue(slot.isEmpty)
        }
    }

    @Test
    fun `getDisplayName retorna Empty para slots vazios`() {
        val slot = SaveSlotData.empty(5)
        assertEquals("Empty", slot.getDisplayName())
    }

    @Test
    fun `getDisplayName retorna nome para slots ocupados`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Boss Fight",
            timestamp = Instant.now(),
            romName = "Test ROM",
            playTime = 3600,
            description = "Before final boss",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        assertEquals("Boss Fight", slot.getDisplayName())
    }

    @Test
    fun `getFormattedTimestamp retorna vazio para timestamp null`() {
        val slot = SaveSlotData.empty(1)
        assertEquals("", slot.getFormattedTimestamp())
    }

    @Test
    fun `getFormattedTimestamp retorna data formatada`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Test",
            timestamp = Instant.parse("2026-02-05T14:30:00Z"),
            romName = "Test ROM",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        val formatted = slot.getFormattedTimestamp()
        assertTrue(formatted.isNotEmpty())
        assertTrue(formatted.contains("/") || formatted.contains("-"))
    }

    @Test
    fun `copy cria nova instancia com dados modificados`() {
        val original = SaveSlotData.empty(1)
        val modified = original.copy(name = "New Name", isEmpty = false)
        
        assertEquals("New Name", modified.name)
        assertFalse(modified.isEmpty)
        assertEquals(original.slotNumber, modified.slotNumber)
    }

    @Test
    fun `equals compara corretamente dois slots`() {
        val slot1 = SaveSlotData.empty(1)
        val slot2 = SaveSlotData.empty(1)
        val slot3 = SaveSlotData.empty(2)
        
        assertEquals(slot1, slot2)
        assertNotEquals(slot1, slot3)
    }

    @Test
    fun `toString retorna representacao legivel`() {
        val slot = SaveSlotData.empty(5)
        val str = slot.toString()

        assertTrue(str.contains("SaveSlotData"))
        assertTrue(str.contains("slotNumber=5"))
    }

    @Test
    fun `getFormattedPlayTime retorna vazio para playTime zero ou negativo`() {
        val zero = SaveSlotData.empty(1).copy(playTime = 0)
        val negative = SaveSlotData.empty(1).copy(playTime = -10)

        assertEquals("", zero.getFormattedPlayTime())
        assertEquals("", negative.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime retorna menos de 1m para playTime abaixo de 60 segundos`() {
        val slot = SaveSlotData.empty(1).copy(playTime = 59)
        assertEquals("<1m", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime converte 60 segundos em 1m, pinando SECONDS_PER_MINUTE`() {
        val slot = SaveSlotData.empty(1).copy(playTime = 60)
        assertEquals("1m", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime converte 3600 segundos em 1h, pinando SECONDS_PER_HOUR`() {
        val slot = SaveSlotData.empty(1).copy(playTime = 3600)
        assertEquals("1h", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime combina horas e minutos`() {
        // 1h30m = 3600 + 30*60 = 5400s
        val slot = SaveSlotData.empty(1).copy(playTime = 5400)
        assertEquals("1h 30m", slot.getFormattedPlayTime())
    }

    @Test
    fun `getFormattedPlayTime nao conta segundos residuais como minuto extra`() {
        // 3659s = 1h 0m 59s -> hours=1, minutes=0 (residual seconds below a minute are dropped)
        val slot = SaveSlotData.empty(1).copy(playTime = 3659)
        assertEquals("1h", slot.getFormattedPlayTime())
    }
}
