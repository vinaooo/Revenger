package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener for game control operations
 */
interface GameControlOperations {
    fun onResetGame()
    fun onFastForward()
    fun getFastForwardState(): Boolean
}
