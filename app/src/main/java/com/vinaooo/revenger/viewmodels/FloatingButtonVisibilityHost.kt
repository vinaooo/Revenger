package com.vinaooo.revenger.viewmodels

/**
 * Implemented by the host Activity to let the ViewModel manage floating-button visibility
 * around menu open/close without depending on the concrete Activity class.
 */
interface FloatingButtonVisibilityHost {
    fun restoreFloatingButtonVisibility()
    fun fadeFloatingButtonImmediately()
}
