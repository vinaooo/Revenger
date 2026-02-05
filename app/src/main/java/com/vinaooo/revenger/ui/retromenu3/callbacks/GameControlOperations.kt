package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener para operações de controle do jogo
 */
interface GameControlOperations {
    fun onResetGame()
    fun onFastForward()
    fun getFastForwardState(): Boolean
}
