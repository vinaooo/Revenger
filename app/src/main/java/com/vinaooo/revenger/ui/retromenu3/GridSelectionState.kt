package com.vinaooo.revenger.ui.retromenu3

/**
 * Owns [SaveStateGridFragment]'s 2D grid navigation position (row/col/back-button-selected),
 * extracted to keep that fragment's function count within detekt's `TooManyFunctions` threshold.
 * Navigation is bounded only -- none of the move functions wrap around grid edges.
 */
class GridSelectionState(private val rows: Int, private val cols: Int) {

        var row: Int = 0
        var col: Int = 0
        var isBackButtonSelected: Boolean = false

        /** 9 slots + 1 back button, handled as a flat count for external navigation registries. */
        val totalNavigableItems: Int
                get() = rows * cols + 1

        val currentIndex: Int
                get() = if (isBackButtonSelected) rows * cols else row * cols + col

        fun reset() {
                row = 0
                col = 0
                isBackButtonSelected = false
        }

        fun selectSlot(newRow: Int, newCol: Int) {
                isBackButtonSelected = false
                row = newRow
                col = newCol
        }

        fun selectBackButton() {
                isBackButtonSelected = true
        }

        fun navigateUp() {
                if (isBackButtonSelected) {
                        // Move from back button to last row of grid, keeping the same column.
                        isBackButtonSelected = false
                        row = rows - 1
                } else if (row > 0) {
                        row--
                }
        }

        fun navigateDown() {
                if (!isBackButtonSelected) {
                        if (row < rows - 1) {
                                row++
                        } else {
                                // Move to back button.
                                isBackButtonSelected = true
                        }
                }
        }

        /** @return whether [col] actually changed. */
        fun navigateLeft(): Boolean {
                if (!isBackButtonSelected && col > 0) {
                        col--
                        return true
                }
                return false
        }

        /** @return whether [col] actually changed. */
        fun navigateRight(): Boolean {
                if (!isBackButtonSelected && col < cols - 1) {
                        col++
                        return true
                }
                return false
        }

        fun setIndex(index: Int) {
                if (index >= rows * cols) {
                        isBackButtonSelected = true
                } else {
                        isBackButtonSelected = false
                        row = index / cols
                        col = index % cols
                }
        }
}
