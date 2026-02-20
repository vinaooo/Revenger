package com.vinaooo.revenger.ui.retromenu3


/**
 * Constants for main menu item indices.
 *
 * **Purpose**: Centralize magic values (0–5) for easier maintenance. If menu order
 * changes, edit only here.
 *
 * **SOLID Pattern**: Avoids duplication of magic numbers. **Maintainability**: Centralized change = single source of truth.
 *
 * **Implemented**: Phase 3.1 - Extracted constants to eliminate magic numbers.
 *
 * @see com.vinaooo.revenger.ui.retromenu3.NavigationEventProcessor
 * @see com.vinaooo.revenger.input.KeyboardInputAdapter
 */
object MenuIndices {
    /** Item 0: Continue (resume game) */
    const val CONTINUE = 0

    /** Item 1: Reset (restart game) */
    const val RESET = 1

    /** Item 2: Progress (save/load states) */
    const val PROGRESS = 2

    /** Item 3: Settings (configurations) */
    const val SETTINGS = 3

    /** Item 4: About (about the game/emulator) */
    const val ABOUT = 4

    /** Item 5: Exit (leave emulator) */
    const val EXIT = 5

    /** Total number of items in main menu */
    const val TOTAL_ITEMS = 6
}
