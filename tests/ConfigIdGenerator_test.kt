package com.vinaooo.revenger.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigIdGenerator_test {

    @Test
    fun `generate combina nome e core em minusculas separados por underscore`() {
        assertEquals("test_game_test_core", ConfigIdGenerator.generate("Test Game", "test_core"))
    }

    @Test
    fun `generate substitui caracteres especiais por underscore`() {
        assertEquals("test_game_core", ConfigIdGenerator.generate("Test: Game!", "core"))
    }

    @Test
    fun `generate colapsa sequencias de caracteres especiais em um unico underscore`() {
        assertEquals("test_game_core", ConfigIdGenerator.generate("Test   Game", "core"))
    }

    @Test
    fun `generate remove underscores nas bordas resultantes da limpeza`() {
        assertEquals("game_core", ConfigIdGenerator.generate("-Game-", "core"))
    }

    @Test
    fun `generate com nome e core vazios produz apenas o separador`() {
        assertEquals("_", ConfigIdGenerator.generate("", ""))
    }

    @Test
    fun `generate preserva digitos e separa palavras de numeros com underscore`() {
        assertEquals("game_2_core64", ConfigIdGenerator.generate("Game 2", "core64"))
    }

    @Test
    fun `generate e deterministico para a mesma entrada`() {
        val first = ConfigIdGenerator.generate("Test Game", "test_core")
        val second = ConfigIdGenerator.generate("Test Game", "test_core")

        assertEquals(first, second)
    }
}
