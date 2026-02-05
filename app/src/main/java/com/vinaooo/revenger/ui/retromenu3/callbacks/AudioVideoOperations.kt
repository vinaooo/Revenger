package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener para operações de áudio e shader
 */
interface AudioVideoOperations {
    fun onToggleAudio()
    fun onToggleShader()
    fun getAudioState(): Boolean
    fun getShaderState(): String
}
