package com.vinaooo.revenger.models

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Testes unitários para SaveSlotData.
 * Testa criação de slots vazios, nomes de exibição e formatação de timestamps.
 */
class SaveSlotDataTest {

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
}
