package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener for save state operations
 */
interface SaveStateOperations {
    fun onSaveState()
    fun onLoadState()
    fun hasSaveState(): Boolean
}
