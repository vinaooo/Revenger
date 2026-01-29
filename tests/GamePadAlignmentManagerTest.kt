package com.vinaooo.revenger.gamepad

import android.content.res.Resources
import com.vinaooo.revenger.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Testes unitários para GamePadAlignmentManager
 *
 * Valida:
 * - Cálculos de margin portrait/landscape
 * - Clamping de valores (0-100%)
 * - Validação de offsets XML
 * - Edge cases
 */
class GamePadAlignmentManagerTest {

    @Mock private lateinit var mockResources: Resources

    private lateinit var alignmentManager: GamePadAlignmentManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        alignmentManager = GamePadAlignmentManager(mockResources)
    }

    // ==============================
    // Testes: calculateBottomMarginPortrait
    // ==============================

    @Test
    fun `calculateBottomMarginPortrait - offset 100% deve retornar 0px (na borda)`() {
        val screenHeight = 1000
        val offsetPercent = 100
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(0, result)
    }

    @Test
    fun `calculateBottomMarginPortrait - offset 50% deve retornar metade da altura`() {
        val screenHeight = 1000
        val offsetPercent = 50
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(500, result)
    }

    @Test
    fun `calculateBottomMarginPortrait - offset 0% deve retornar altura total`() {
        val screenHeight = 1000
        val offsetPercent = 0
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(1000, result)
    }

    @Test
    fun `calculateBottomMarginPortrait - offset 75% deve retornar 25% da altura`() {
        val screenHeight = 800
        val offsetPercent = 75
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(200, result)
    }

    @Test
    fun `calculateBottomMarginPortrait - valores acima de 100% devem ser clampados`() {
        val screenHeight = 1000
        val offsetPercent = 150
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(0, result) // Clampado para 100%
    }

    @Test
    fun `calculateBottomMarginPortrait - valores negativos devem ser clampados`() {
        val screenHeight = 1000
        val offsetPercent = -50
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(1000, result) // Clampado para 0%
    }

    // ==============================
    // Testes: calculateTopMarginLandscape
    // ==============================

    @Test
    fun `calculateTopMarginLandscape - offset 0% deve retornar 0px (no topo)`() {
        val screenHeight = 1000
        val offsetPercent = 0
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(0, result)
    }

    @Test
    fun `calculateTopMarginLandscape - offset 100% deve retornar altura total (abaixo)`() {
        val screenHeight = 1000
        val offsetPercent = 100
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(1000, result)
    }

    @Test
    fun `calculateTopMarginLandscape - offset 50% deve retornar metade da altura`() {
        val screenHeight = 600
        val offsetPercent = 50
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(300, result)
    }

    @Test
    fun `calculateTopMarginLandscape - valores acima de 100% devem ser clampados`() {
        val screenHeight = 1000
        val offsetPercent = 200
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(1000, result) // Clampado para 100%
    }

    @Test
    fun `calculateTopMarginLandscape - valores negativos devem ser clampados`() {
        val screenHeight = 1000
        val offsetPercent = -30
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(0, result) // Clampado para 0%
    }

    // ==============================
    // Testes: getEmptyDialIndicesForLeft
    // ==============================

    @Test
    fun `getEmptyDialIndicesForLeft - deve retornar lista com índice 8`() {
        val result = alignmentManager.getEmptyDialIndicesForLeft()
        assertEquals(listOf(8), result)
    }

    // ==============================
    // Testes: validateOffsets
    // ==============================

    @Test
    fun `validateOffsets - valores válidos devem retornar true`() {
        `when`(mockResources.getInteger(R.integer.gp_offset_portrait)).thenReturn(100)
        `when`(mockResources.getInteger(R.integer.gp_offset_landscape)).thenReturn(100)

        val (isValid, _) = alignmentManager.validateOffsets()
        assertTrue(isValid)
    }

    @Test
    fun `validateOffsets - portrait acima de 100 deve retornar false`() {
        `when`(mockResources.getInteger(R.integer.gp_offset_portrait)).thenReturn(150)
        `when`(mockResources.getInteger(R.integer.gp_offset_landscape)).thenReturn(100)

        val (isValid, message) = alignmentManager.validateOffsets()
        assertFalse(isValid)
        assertTrue(message.contains("gp_offset_portrait"))
    }

    @Test
    fun `validateOffsets - landscape negativo deve retornar false`() {
        `when`(mockResources.getInteger(R.integer.gp_offset_portrait)).thenReturn(50)
        `when`(mockResources.getInteger(R.integer.gp_offset_landscape)).thenReturn(-10)

        val (isValid, message) = alignmentManager.validateOffsets()
        assertFalse(isValid)
        assertTrue(message.contains("gp_offset_landscape"))
    }

    @Test
    fun `validateOffsets - valores no limite inferior (0) devem ser válidos`() {
        `when`(mockResources.getInteger(R.integer.gp_offset_portrait)).thenReturn(0)
        `when`(mockResources.getInteger(R.integer.gp_offset_landscape)).thenReturn(0)

        val (isValid, _) = alignmentManager.validateOffsets()
        assertTrue(isValid)
    }

    @Test
    fun `validateOffsets - valores no limite superior (100) devem ser válidos`() {
        `when`(mockResources.getInteger(R.integer.gp_offset_portrait)).thenReturn(100)
        `when`(mockResources.getInteger(R.integer.gp_offset_landscape)).thenReturn(100)

        val (isValid, _) = alignmentManager.validateOffsets()
        assertTrue(isValid)
    }

    // ==============================
    // Testes: Edge Cases
    // ==============================

    @Test
    fun `calculateBottomMarginPortrait - altura zero deve retornar zero`() {
        val screenHeight = 0
        val offsetPercent = 50
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(0, result)
    }

    @Test
    fun `calculateTopMarginLandscape - altura zero deve retornar zero`() {
        val screenHeight = 0
        val offsetPercent = 50
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(0, result)
    }

    @Test
    fun `calculateBottomMarginPortrait - altura muito grande deve calcular corretamente`() {
        val screenHeight = 10000
        val offsetPercent = 25
        val result = alignmentManager.calculateBottomMarginPortrait(screenHeight, offsetPercent)
        assertEquals(7500, result) // 75% de 10000
    }

    @Test
    fun `calculateTopMarginLandscape - altura muito grande deve calcular corretamente`() {
        val screenHeight = 10000
        val offsetPercent = 80
        val result = alignmentManager.calculateTopMarginLandscape(screenHeight, offsetPercent)
        assertEquals(8000, result) // 80% de 10000
    }
}
