package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [RetroTextCursor] was extracted out of [RetroEditText] (text-buffer mutation + cursor-index
 * bookkeeping), which had zero test coverage before this file. Plain JVM test -- the class has no
 * Android dependency, which is exactly the point of the extraction.
 */
class RetroTextCursor_test {

    private lateinit var cursor: RetroTextCursor
    private var onChangeCalls = 0

    @Before
    fun setUp() {
        cursor = RetroTextCursor()
        cursor.onChange = { onChangeCalls++ }
    }

    @Test
    fun `estado inicial e texto vazio com cursor na posicao 0`() {
        assertEquals("", cursor.getTextContent())
        assertEquals(0, cursor.position)
    }

    @Test
    fun `setTextContent define o texto e move o cursor para o final`() {
        cursor.setTextContent("abc")

        assertEquals("abc", cursor.getTextContent())
        assertEquals(3, cursor.position)
        assertEquals(1, onChangeCalls)
    }

    @Test
    fun `insertChar insere no cursor e avanca a posicao`() {
        cursor.setTextContent("ac")
        cursor.moveCursorLeft()

        cursor.insertChar("b")

        assertEquals("abc", cursor.getTextContent())
        assertEquals(2, cursor.position)
    }

    @Test
    fun `deleteChar remove o caractere anterior ao cursor e retorna true`() {
        cursor.setTextContent("abc")

        val deleted = cursor.deleteChar()

        assertTrue(deleted)
        assertEquals("ab", cursor.getTextContent())
        assertEquals(2, cursor.position)
    }

    @Test
    fun `deleteChar na posicao 0 nao faz nada e retorna false`() {
        val deleted = cursor.deleteChar()

        assertFalse(deleted)
        assertEquals("", cursor.getTextContent())
    }

    @Test
    fun `moveCursorLeft e moveCursorRight respeitam os limites do texto`() {
        cursor.setTextContent("ab")
        cursor.moveCursorToStart()

        assertFalse(cursor.moveCursorLeft())

        assertTrue(cursor.moveCursorRight())
        assertEquals(1, cursor.position)
        assertTrue(cursor.moveCursorRight())
        assertEquals(2, cursor.position)
        assertFalse(cursor.moveCursorRight())
    }

    @Test
    fun `moveCursorToStart e moveCursorToEnd movem para os extremos`() {
        cursor.setTextContent("abcd")

        cursor.moveCursorToStart()
        assertEquals(0, cursor.position)

        cursor.moveCursorToEnd()
        assertEquals(4, cursor.position)
    }

    @Test
    fun `onChange e chamado a cada mutacao`() {
        cursor.setTextContent("a")
        cursor.insertChar("b")
        cursor.moveCursorLeft()
        cursor.moveCursorRight()
        cursor.moveCursorToStart()
        cursor.moveCursorToEnd()
        cursor.deleteChar()

        assertEquals(7, onChangeCalls)
    }
}
