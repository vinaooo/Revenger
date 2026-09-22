package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GridSelectionState], extracted from [SaveStateGridFragment] to keep that
 * fragment's function count within detekt's `TooManyFunctions` threshold. Mirrors the bounded
 * 2D grid navigation behavior [SaveStateGridFragment_test] already exercised through the
 * fragment's public API, but at the class level directly.
 */
class GridSelectionState_test {

    private fun newState() = GridSelectionState(rows = 3, cols = 3)

    @Test
    fun `starts at row 0 col 0 with back button not selected`() {
        val state = newState()

        assertEquals(0, state.row)
        assertEquals(0, state.col)
        assertFalse(state.isBackButtonSelected)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `totalNavigableItems is rows times cols plus the back button`() {
        val state = newState()

        assertEquals(10, state.totalNavigableItems)
    }

    @Test
    fun `navigateDown advances through rows then to the back button and does not wrap`() {
        val state = newState()

        state.navigateDown()
        assertEquals(1, state.row)
        assertFalse(state.isBackButtonSelected)

        state.navigateDown()
        assertEquals(2, state.row)
        assertFalse(state.isBackButtonSelected)

        state.navigateDown()
        assertTrue("A 4th down from the last row must land on the back button", state.isBackButtonSelected)

        // Bounded: one more down must not wrap back to row 0.
        state.navigateDown()
        assertTrue(state.isBackButtonSelected)
        assertEquals(2, state.row)
    }

    @Test
    fun `navigateUp is bounded at the first row - no wrap`() {
        val state = newState()

        state.navigateUp()

        assertEquals(0, state.row)
        assertFalse(state.isBackButtonSelected)
    }

    @Test
    fun `navigateUp from the back button returns to the last row keeping the column`() {
        val state = newState()
        state.setIndex(state.totalNavigableItems - 1) // back button
        state.col = 2
        assertTrue(state.isBackButtonSelected)

        state.navigateUp()

        assertFalse(state.isBackButtonSelected)
        assertEquals(2, state.row) // last row (rows - 1)
        assertEquals(2, state.col)
    }

    @Test
    fun `navigateLeft and navigateRight are bounded on columns and report whether they changed anything`() {
        val state = newState()

        assertFalse(state.navigateLeft()) // already at col 0, bounded, reports no change
        assertEquals(0, state.col)

        assertTrue(state.navigateRight())
        assertEquals(1, state.col)
        assertTrue(state.navigateRight())
        assertEquals(2, state.col)
        assertFalse(state.navigateRight()) // bounded at last column, reports no change
        assertEquals(2, state.col)
    }

    @Test
    fun `lateral navigation is ignored when the back button is selected`() {
        val state = newState()
        state.setIndex(state.totalNavigableItems - 1) // back button
        state.col = 1

        assertFalse(state.navigateLeft())
        assertFalse(state.navigateRight())
        assertTrue(state.isBackButtonSelected)
        assertEquals(1, state.col)
    }

    @Test
    fun `setIndex converts a linear index into row and col`() {
        val state = newState()

        state.setIndex(4) // row=1, col=1

        assertEquals(4, state.currentIndex)
        assertEquals(1, state.row)
        assertEquals(1, state.col)
        assertFalse(state.isBackButtonSelected)
    }

    @Test
    fun `setIndex at or beyond rows times cols selects the back button`() {
        val state = newState()

        state.setIndex(9)

        assertTrue(state.isBackButtonSelected)
        assertEquals(9, state.currentIndex)
    }

    @Test
    fun `selectSlot clears back button selection and sets row and col`() {
        val state = newState()
        state.selectBackButton()

        state.selectSlot(2, 1)

        assertFalse(state.isBackButtonSelected)
        assertEquals(2, state.row)
        assertEquals(1, state.col)
    }

    @Test
    fun `selectBackButton selects the back button`() {
        val state = newState()

        state.selectBackButton()

        assertTrue(state.isBackButtonSelected)
    }

    @Test
    fun `reset returns to row 0 col 0 with back button not selected`() {
        val state = newState()
        state.setIndex(9) // back button
        state.col = 2

        state.reset()

        assertEquals(0, state.row)
        assertEquals(0, state.col)
        assertFalse(state.isBackButtonSelected)
    }
}
