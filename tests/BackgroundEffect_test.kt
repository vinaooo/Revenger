package com.vinaooo.revenger.ui.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundEffect_test {

    @Test
    fun `create com tipo 3 retorna ScanlineEffect`() {
        assertTrue(BackgroundEffectFactory.create(3) is ScanlineEffect)
    }

    @Test
    fun `create com tipo 0 retorna NoEffect`() {
        assertTrue(BackgroundEffectFactory.create(0) is NoEffect)
    }

    @Test
    fun `create com tipo desconhecido retorna NoEffect como fallback`() {
        assertTrue(BackgroundEffectFactory.create(99) is NoEffect)
    }

    @Test
    fun `getAllEffectTypes retorna os tipos 0 e 3`() {
        assertEquals(listOf(0, 3), BackgroundEffectFactory.getAllEffectTypes())
    }

    @Test
    fun `getEffectName mapeia os tipos conhecidos e usa fallback para os demais`() {
        assertEquals("None (Dimming Only)", BackgroundEffectFactory.getEffectName(0))
        assertEquals("Scanline (CRT)", BackgroundEffectFactory.getEffectName(3))
        assertEquals("Unknown", BackgroundEffectFactory.getEffectName(99))
    }
}
