package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Listener for audio and shader operations
 */
interface AudioVideoOperations {
    fun onToggleAudio()
    fun onToggleShader()
    fun getAudioState(): Boolean
    fun getShaderState(): String
}
