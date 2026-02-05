package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener para operações de save state
 */
interface SaveStateOperations {
    fun onSaveState()
    fun onLoadState()
    fun hasSaveState(): Boolean
}
